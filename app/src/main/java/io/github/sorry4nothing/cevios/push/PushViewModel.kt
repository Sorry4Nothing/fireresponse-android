package io.github.sorry4nothing.cevios.push

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sorry4nothing.cevios.auth.FormState
import io.github.sorry4nothing.cevios.auth.failureForm
import io.github.sorry4nothing.cevios.chat.Profile
import io.github.sorry4nothing.cevios.net.ApiError
import io.github.sorry4nothing.cevios.net.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Die Push-Schalter der Person und das Hinterlegen des Tokens dieses Geräts. */
class PushViewModel(private val api: PushApi, profile: Profile) : ViewModel() {
    /** `registrations` ist `null` bei Personen ohne Stufenrolle: Sie haben den Schalter nicht. */
    data class Switches(val chat: Boolean, val programs: Boolean, val registrations: Boolean? = null)

    private val _switches = MutableStateFlow(Switches(profile.pushChat, profile.pushPrograms, profile.pushRegistrations))
    val switches: StateFlow<Switches> = _switches.asStateFlow()

    private val _form = MutableStateFlow(FormState())
    val form: StateFlow<FormState> = _form.asStateFlow()

    fun setChat(on: Boolean) = save(PushSwitchesRequest(chat = on)) { it.copy(chat = on) }

    fun setPrograms(on: Boolean) = save(PushSwitchesRequest(programs = on)) { it.copy(programs = on) }

    fun setRegistrations(on: Boolean) = save(PushSwitchesRequest(registrations = on)) { it.copy(registrations = on) }

    /** Der Schalter springt erst um, wenn der Server zugestimmt hat. */
    private fun save(request: PushSwitchesRequest, apply: (Switches) -> Switches) {
        if (_form.value.running) return
        _form.value = FormState(running = true)
        viewModelScope.launch {
            when (val result = api.setSwitches(request)) {
                is ApiResult.Failure ->
                    _form.value = if (result.error == ApiError.NOT_SIGNED_IN) FormState() else failureForm(result, result.error.text)
                is ApiResult.Ok -> {
                    _switches.update(apply)
                    _form.value = FormState()
                }
            }
        }
    }

    fun clearError() {
        if (!_form.value.running) _form.value = FormState()
    }

    /**
     * Holt das Token und hinterlegt es. Ohne Token (keine Firebase-Konfiguration) oder bei einem Fehler passiert nichts und
     * es erscheint nichts: Beim nächsten Start versucht die App es wieder, und Firebase meldet jede Erneuerung.
     */
    fun registerDevice(token: suspend () -> String?) {
        viewModelScope.launch { token()?.let { api.registerToken(it) } }
    }
}
