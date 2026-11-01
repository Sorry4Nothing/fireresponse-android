package io.github.sorry4nothing.cevios.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import io.github.sorry4nothing.cevios.R
import io.github.sorry4nothing.cevios.chat.Profile
import io.github.sorry4nothing.cevios.net.ApiError
import io.github.sorry4nothing.cevios.net.ApiResult
import io.github.sorry4nothing.cevios.security.LockState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Die ViewModels (Programme, Chat) einer angemeldeten Person. Sie liegen in einem eigenen Speicher, der beim Abmelden
 * geleert wird: Die nächste Person sieht nichts von der vorherigen, auch nicht im Arbeitsspeicher.
 */
class SessionScope : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}

sealed interface AuthState {
    /** Die App fragt den Server, ob eine Sitzung besteht. */
    data object Loading : AuthState

    /** Die Person hat das Entsperren mit Fingerabdruck oder Gesicht eingeschaltet: Bis sie sich ausgewiesen hat, zeigt die App nichts. */
    data object Locked : AuthState

    /** Die Antwort kam nicht (offline, Fehler); `error` sagt warum. */
    data class Failed(val error: ApiError) : AuthState

    /** Niemand angemeldet. `expired`: Die Sitzung ist abgelaufen oder wurde beendet. */
    data class SignedOut(val expired: Boolean) : AuthState

    /** Die Person hat noch ein Startpasswort und muss ein eigenes wählen. */
    data object MustChangePassword : AuthState

    /** Erster Start: Die Person nimmt die Datenschutzerklärung zur Kenntnis. */
    data class Welcome(val profile: Profile) : AuthState

    data class SignedIn(val profile: Profile, val scope: SessionScope) : AuthState
}

/** Zustand eines Formulars (Anmelden, Passwort, Datenschutz): läuft gerade etwas, und was ging schief. */
data class FormState(val running: Boolean = false, @StringRes val error: Int? = null, val waitSeconds: Int? = null)

/** Der Fehler einer Anfrage als Zustand eines Formulars; bei «zu viele Anfragen» mit der Wartezeit des Servers. */
internal fun failureForm(failure: ApiResult.Failure, @StringRes text: Int) =
    if (failure.error == ApiError.TOO_MANY_REQUESTS && failure.retryAfterSeconds != null) {
        FormState(error = R.string.error_too_many_wait, waitSeconds = failure.retryAfterSeconds)
    } else {
        FormState(error = text)
    }

/**
 * Wer ist angemeldet, und was muss die Person noch tun. Passwörter liegen nie hier: Die Eingabe bleibt im Bildschirm und
 * geht direkt an den Server.
 */
class AuthViewModel(
    private val api: AuthApi,
    unauthorized: Flow<Unit>,
    forbidden: Flow<Unit>,
    private val forgetSession: () -> Unit,
    private val lock: LockState,
    private val hasSession: () -> Boolean,
) : ViewModel() {
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _form = MutableStateFlow(FormState())
    val form: StateFlow<FormState> = _form.asStateFlow()

    /** Die Person hat gerade ihr Passwort gespeichert (Hinweis im Willkommen und im Profil). */
    private val _passwordSaved = MutableStateFlow(false)
    val passwordSaved: StateFlow<Boolean> = _passwordSaved.asStateFlow()

    /** Zustand von «Profilbild ändern» und «Profilbild entfernen» (getrennt von Abmelden und Passwort). */
    private val _imageForm = MutableStateFlow(FormState())
    val imageForm: StateFlow<FormState> = _imageForm.asStateFlow()

    /**
     * Das Passwort des Logins, nur im Arbeitsspeicher und nur, bis der Pflichtwechsel durch ist: Der Server verlangt zum Ändern
     * das bisherige Passwort, und die Person hat es gerade getippt. Nie in `rememberSaveable`, Logs oder auf dem Gerät.
     */
    private var startPassword: String? = null
    private val _startPasswordKnown = MutableStateFlow(false)
    val startPasswordKnown: StateFlow<Boolean> = _startPasswordKnown.asStateFlow()

    private fun rememberStartPassword(value: String?) {
        startPassword = value
        _startPasswordKnown.value = value != null
    }

    private val _lockEnabled = MutableStateFlow(lock.enabled)

    /** Ob die App sich beim Start mit Fingerabdruck oder Gesicht entsperrt. */
    val lockEnabled: StateFlow<Boolean> = _lockEnabled.asStateFlow()

    /** Gilt ab dem Entsperren, bis der Prozess endet: Danach fragt der Start wieder. */
    private var unlocked = false

    private var scope: SessionScope? = null

    init {
        start()
        viewModelScope.launch {
            unauthorized.collect { if (_state.value !is AuthState.SignedOut) leave(expired = true) }
        }
        viewModelScope.launch { forbidden.collect { recheckAfterForbidden() } }
    }

    fun start() {
        if (lock.enabled && !unlocked && hasSession()) {
            _state.value = AuthState.Locked
            return
        }
        _state.value = AuthState.Loading
        viewModelScope.launch { resolve() }
    }

    /**
     * Die App war eine Weile im Hintergrund: Mit eingeschaltetem Entsperren zeigt sie wieder den Sperrbildschirm. Die Daten der
     * Sitzung verlassen den Arbeitsspeicher, die Sitzung selbst bleibt; nach dem Ausweis lädt die App neu.
     */
    fun relock() {
        if (!lock.enabled || _state.value !is AuthState.SignedIn) return
        unlocked = false
        scope?.viewModelStore?.clear()
        scope = null
        _imageForm.value = FormState()
        _form.value = FormState()
        _passwordSaved.value = false
        _state.value = AuthState.Locked
    }

    /** Die Person hat sich ausgewiesen: Jetzt fragt die App den Server. */
    fun unlock() {
        unlocked = true
        _form.value = FormState()
        start()
    }

    /** Das Entsperren hat nicht geklappt (Gerätefehler, zu viele Versuche); die App bleibt gesperrt. */
    fun unlockFailed() {
        _form.value = FormState(error = R.string.lock_failed)
    }

    /** Die Biometrie des Geräts hat sich geändert: Der Schlüssel gilt nicht mehr, die Anmeldung mit Passwort folgt. */
    fun lockInvalidated() {
        turnLockOff()
        leave(expired = false)
        _form.value = FormState(error = R.string.lock_changed)
    }

    /** Die Person hat das Entsperren eingeschaltet (der Schlüssel ist angelegt und einmal benutzt). */
    fun lockTurnedOn() {
        _form.value = FormState()
        _lockEnabled.value = true
    }

    fun turnLockOff() {
        lock.disable()
        _lockEnabled.value = false
    }

    fun signIn(username: String, password: String) {
        if (_form.value.running) return
        _form.value = FormState(running = true)
        viewModelScope.launch {
            when (val result = api.signIn(username.trim(), password)) {
                is ApiResult.Failure -> _form.value = failureForm(result, signInErrorText(result))
                is ApiResult.Ok -> if (result.value.twoFactorRedirect) {
                    // Admins melden sich nur im Admin-Bereich an: kein Sitzungscookie, auch kein halbes (`two_factor`).
                    forgetSession()
                    _form.value = FormState(error = R.string.login_admin_only)
                } else {
                    // Das Passwort ist ein Ausweis wie die Biometrie: Wer sich damit anmeldet, wird nicht gleich noch einmal gefragt.
                    unlocked = true
                    rememberStartPassword(password)
                    resolve()
                    // Wer nicht zum Pflichtwechsel kommt, braucht es nicht.
                    if (_state.value !is AuthState.MustChangePassword) rememberStartPassword(null)
                    _form.value = FormState()
                }
            }
        }
    }

    /** `current` ist `null`, wenn die App das Startpasswort schon vom Login kennt. */
    fun changePassword(current: String?, new: String) {
        if (_form.value.running) return
        val bisher = current ?: startPassword ?: return
        _form.value = FormState(running = true)
        viewModelScope.launch {
            when (val result = api.changePassword(bisher, new)) {
                is ApiResult.Failure -> if (result.error != ApiError.NOT_SIGNED_IN) {
                    // Stimmt es nicht, fragt der Bildschirm danach, statt dasselbe noch einmal zu senden.
                    if (result.code == "INVALID_PASSWORD") rememberStartPassword(null)
                    _form.value = failureForm(result, PasswordRules.errorText(result))
                }
                is ApiResult.Ok -> {
                    rememberStartPassword(null)
                    _passwordSaved.value = true
                    resolve()
                    _form.value = FormState()
                }
            }
        }
    }

    fun acceptPrivacy() {
        val welcome = _state.value as? AuthState.Welcome ?: return
        if (_form.value.running) return
        _form.value = FormState(running = true)
        viewModelScope.launch {
            when (val result = api.acceptPrivacy()) {
                is ApiResult.Failure -> if (result.error != ApiError.NOT_SIGNED_IN) _form.value = failureForm(result, result.error.text)
                is ApiResult.Ok -> {
                    _form.value = FormState()
                    _passwordSaved.value = false
                    enter(welcome.profile)
                }
            }
        }
    }

    /**
     * Lädt ein neues Profilbild hoch. `prepare` liest und verkleinert das gewählte Foto (kann dauern, läuft nicht auf dem
     * Hauptthread) und liefert `null`, wenn es sich nicht lesen lässt. Danach lädt die App das Profil neu: Die `bildUrl` hat
     * eine neue Fassung.
     */
    fun uploadImage(prepare: suspend () -> ByteArray?) {
        if (_imageForm.value.running) return
        _imageForm.value = FormState(running = true)
        viewModelScope.launch {
            val jpeg = prepare()
            if (jpeg == null) {
                _imageForm.value = FormState(error = R.string.error_image_unreadable)
                return@launch
            }
            finishImageChange(api.uploadImage(jpeg))
        }
    }

    fun removeImage() {
        if (_imageForm.value.running) return
        _imageForm.value = FormState(running = true)
        viewModelScope.launch { finishImageChange(api.removeImage()) }
    }

    private suspend fun finishImageChange(result: ApiResult<Unit>) {
        when (result) {
            is ApiResult.Failure -> _imageForm.value = if (result.error == ApiError.NOT_SIGNED_IN) FormState() else failureForm(result, result.error.text)
            is ApiResult.Ok -> {
                val fresh = (api.profile() as? ApiResult.Ok)?.value
                val current = _state.value
                if (fresh != null && current is AuthState.SignedIn) _state.value = current.copy(profile = fresh)
                _imageForm.value = FormState()
            }
        }
    }

    fun clearImageError() {
        _imageForm.value = FormState()
    }

    /** Beendet die Sitzung auf dem Server und leert alles auf dem Gerät. Offline geht es auch: Dann endet sie dort mit ihrem Ablauf. */
    fun signOut() {
        if (_form.value.running) return
        _form.value = FormState(running = true)
        viewModelScope.launch {
            api.signOut()
            leave(expired = false)
        }
    }

    /** Das Profil lädt neu: Danach zeigt die App, ob noch ein Schritt fehlt (Passwort, Datenschutz). */
    private suspend fun resolve() {
        when (val result = api.session()) {
            is ApiResult.Failure -> fail(result.error)
            is ApiResult.Ok -> {
                val session = result.value
                if (session == null) leave(expired = false)
                else if (session.user.mussPasswortAendern) _state.value = AuthState.MustChangePassword
                else loadProfile()
            }
        }
    }

    private suspend fun loadProfile() {
        when (val result = api.profile()) {
            is ApiResult.Failure -> fail(result.error)
            is ApiResult.Ok -> {
                val profile = result.value
                when {
                    profile.mustChangePassword -> _state.value = AuthState.MustChangePassword
                    profile.privacyReadAt == null -> _state.value = AuthState.Welcome(profile)
                    else -> enter(profile)
                }
            }
        }
    }

    /** Bei 429 mit Wartezeit sagt der Text, wie lange; sonst der Text des Aufrufs. */
    private fun fail(error: ApiError) {
        if (error == ApiError.NOT_SIGNED_IN) leave(expired = true) else _state.value = AuthState.Failed(error)
    }

    private fun enter(profile: Profile) {
        scope?.viewModelStore?.clear()
        val fresh = SessionScope().also { scope = it }
        _state.value = AuthState.SignedIn(profile, fresh)
    }

    private var rechecking = false

    /**
     * Ein 403 unter `/api/v1` heisst auch «Passwortwechsel offen». Das Profil (`GET /ich`) bleibt dann erreichbar und sagt es:
     * Dann geht es zur Passwortseite, sonst bleibt es bei der Meldung der Stelle. Mehrere 403 gleichzeitig prüfen einmal.
     */
    private fun recheckAfterForbidden() {
        if (rechecking || _state.value !is AuthState.SignedIn) return
        rechecking = true
        viewModelScope.launch {
            try {
                val profile = (api.profile() as? ApiResult.Ok)?.value
                if (profile != null && profile.mustChangePassword) {
                    scope?.viewModelStore?.clear()
                    scope = null
                    _state.value = AuthState.MustChangePassword
                }
            } finally {
                rechecking = false
            }
        }
    }

    /** Verlässt die Sitzung: Cookie, Daten im Arbeitsspeicher und Formular sind weg. */
    private fun leave(expired: Boolean) {
        rememberStartPassword(null)
        forgetSession()
        scope?.viewModelStore?.clear()
        scope = null
        _imageForm.value = FormState()
        _form.value = FormState()
        _passwordSaved.value = false
        _state.value = AuthState.SignedOut(expired)
    }

    /** Beim Wechsel des Bildschirms: ein Fehler des vorigen gehört nicht auf den nächsten. */
    fun clearError() {
        _form.value = FormState()
    }

    fun clearPasswordSaved() {
        _passwordSaved.value = false
    }

    override fun onCleared() {
        scope?.viewModelStore?.clear()
    }
}
