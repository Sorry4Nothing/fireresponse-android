package ch.fireresponse.app

import ch.fireresponse.app.ui.screenTransition
import ch.fireresponse.app.ui.rememberMotionEnabled
import ch.fireresponse.app.ui.loginTransition
import ch.fireresponse.app.ui.fadeTransition
import ch.fireresponse.app.security.LockOutcome
import ch.fireresponse.app.security.BiometricPrompter
import ch.fireresponse.app.security.AppLock
import ch.fireresponse.app.auth.LockedScreen
import ch.fireresponse.app.security.shouldRelock
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.runtime.mutableLongStateOf
import android.os.SystemClock
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import android.Manifest
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import ch.fireresponse.app.ui.theme.WideBreakpoint
import ch.fireresponse.app.ui.theme.Spacing
import ch.fireresponse.app.ui.ContentWidth
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import ch.fireresponse.app.auth.AuthApi
import ch.fireresponse.app.auth.AuthState
import ch.fireresponse.app.auth.AuthViewModel
import ch.fireresponse.app.auth.CropImageScreen
import ch.fireresponse.app.auth.FormState
import ch.fireresponse.app.auth.LoginScreen
import ch.fireresponse.app.auth.PrivacyScreen
import ch.fireresponse.app.auth.PrivacyViewModel
import ch.fireresponse.app.auth.NoSquadScreen
import ch.fireresponse.app.auth.PasswordScreen
import ch.fireresponse.app.auth.ProfileScreen
import ch.fireresponse.app.auth.StartScreen
import ch.fireresponse.app.auth.WelcomeScreen
import ch.fireresponse.app.profile.Profile
import ch.fireresponse.app.images.LocalProfileImages
import ch.fireresponse.app.images.LocalRankImages
import ch.fireresponse.app.images.RankImageApi
import ch.fireresponse.app.images.RankImages
import ch.fireresponse.app.images.ProfileImageApi
import ch.fireresponse.app.images.ProfileImageEncoder
import ch.fireresponse.app.images.ProfileImages
import ch.fireresponse.app.net.ApiClient
import ch.fireresponse.app.net.ApiError
import ch.fireresponse.app.net.ApiResult
import ch.fireresponse.app.push.Notifications
import ch.fireresponse.app.push.NotificationsScreen
import ch.fireresponse.app.push.PushApi
import ch.fireresponse.app.push.PushTarget
import ch.fireresponse.app.push.PushToken
import ch.fireresponse.app.push.PushViewModel
import ch.fireresponse.app.appointments.AppointmentDetailScreen
import ch.fireresponse.app.appointments.AppointmentsApi
import ch.fireresponse.app.appointments.AppointmentListScreen
import ch.fireresponse.app.appointments.RegistrationsRules
import ch.fireresponse.app.appointments.RegistrationsScreen
import ch.fireresponse.app.appointments.startDay
import ch.fireresponse.app.appointments.weekdayShort
import ch.fireresponse.app.appointments.AppointmentsViewModel
import ch.fireresponse.app.ui.MainScaffold
import ch.fireresponse.app.ui.TwoPane
import ch.fireresponse.app.ui.Destination
import ch.fireresponse.app.ui.BusyOverlay
import ch.fireresponse.app.ui.ErrorState
import ch.fireresponse.app.ui.theme.FireResponseTheme
import ch.fireresponse.app.widget.AppointmentWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** So oft liest der offene Termin Stand und Kontakte still neu. */
private const val DETAIL_POLL_INTERVAL_MS = 15_000L

class MainActivity : FragmentActivity() {
    /** Wohin das Tippen auf einen Push führt; bleibt stehen, bis die Person angemeldet ist und der Bildschirm es aufgreift. */
    private var pushTarget by mutableStateOf<PushTarget?>(null)

    private fun targetOf(intent: Intent?) = PushTarget.from(intent?.getStringExtra("typ"), intent?.getStringExtra("id"))

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        targetOf(intent)?.let { pushTarget = it }
    }

    override fun onStart() {
        super.onStart()
        // Im Vordergrund ist das Netz erlaubt, auch im Energiesparmodus: So füllt sich ein Widget, dessen Aktualisierung im Hintergrund gesperrt war.
        CoroutineScope(Dispatchers.IO).launch { runCatching { AppointmentWidget.refreshIfPlaced(applicationContext) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Der Start lief mit dem Theme des Splash (Manifest); ab jetzt gilt das Theme der App.
        setTheme(R.style.Theme_FireResponse)
        super.onCreate(savedInstanceState)
        // Die App zeigt Daten von Kindern: Weder Bildschirmfotos noch «Zuletzt benutzt» zeigen etwas davon, mit und ohne Sperre.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        // Auch auf einem englisch gestellten Gerät liest der Screenreader die App mit deutscher Aussprache vor (ab Android 13).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags("de")
        }
        Notifications.createChannels(applicationContext)
        // Nach Drehen oder Prozess-Ende gilt der Push nicht noch einmal: Das Intent ist dann dasselbe wie beim ersten Start.
        if (savedInstanceState == null) pushTarget = targetOf(intent)
        val client = ApiClient.create(applicationContext)
        val lock = AppLock(applicationContext)
        val prompter = BiometricPrompter(this, lock)
        setContent { FireResponseTheme { Root(client, lock, prompter, pushTarget) { pushTarget = null } } }
    }
}

/** Je nach Anmeldestand: Start, Fehler, Anmelden, Passwort wählen, Willkommen oder die App selbst. */
@Composable
private fun Root(client: ApiClient, lock: AppLock, prompter: BiometricPrompter, pushTarget: PushTarget?, onPushTargetHandled: () -> Unit) {
    val authApi = remember(client) { AuthApi(client) }
    val auth = viewModel { AuthViewModel(authApi, client.unauthorized, client.forbidden, client::forgetSession, lock, client::hasSession) }
    val state by auth.state.collectAsState()
    val form by auth.form.collectAsState()
    val passwordSaved by auth.passwordSaved.collectAsState()
    val imageForm by auth.imageForm.collectAsState()
    val pending by client.pending.collectAsState()
    val context = LocalContext.current
    // Das Widget zeigt nach dem Abmelden gleich die Meldung statt der Übungen der vorigen Person.
    LaunchedEffect(state is AuthState.SignedOut) { if (state is AuthState.SignedOut) AppointmentWidget.showSignedOut(context) }
    val lockEnabled by auth.lockEnabled.collectAsState()
    var stoppedAt by remember { mutableLongStateOf(0L) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { stoppedAt = SystemClock.elapsedRealtime() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (shouldRelock(if (stoppedAt == 0L) null else SystemClock.elapsedRealtime() - stoppedAt)) auth.relock()
        stoppedAt = 0L
    }
    val lockAvailable = remember { lock.isAvailable() }
    val onLockChange: (Boolean) -> Unit = { on ->
        if (on) {
            prompter.enable { if (it == LockOutcome.Success) auth.lockTurnedOn() else if (it == LockOutcome.Failed) auth.unlockFailed() }
        } else {
            auth.turnLockOff()
        }
    }
    // Die Datenschutzerklärung vor der Einwilligung (Willkommen) zeigt die App selbst; der Text kommt vom Server.
    val privacy = viewModel { PrivacyViewModel(authApi) }
    var welcomePrivacyOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(welcomePrivacyOpen) { if (welcomePrivacyOpen) privacy.load() }
    val motion = rememberMotionEnabled()
    Surface(Modifier.fillMaxSize()) {
        AnimatedContent(
            state,
            transitionSpec = { loginTransition(initialState, motion) },
            contentKey = { it::class },
            label = "anmeldung",
        ) { current ->
            when (current) {
                AuthState.Loading -> StartScreen()
                AuthState.Locked -> LockedScreen(
                    form,
                    onUnlock = {
                        prompter.unlock {
                            when (it) {
                                LockOutcome.Success -> auth.unlock()
                                LockOutcome.Invalidated -> auth.lockInvalidated()
                                LockOutcome.Failed -> auth.unlockFailed()
                                LockOutcome.Declined -> Unit
                            }
                        }
                    },
                    onUsePassword = auth::signOut,
                )
                is AuthState.Failed -> Box(Modifier.safeDrawingPadding()) { ErrorState(current.error, onRetry = auth::start) }
                is AuthState.SignedOut -> {
                    var loginRequested by rememberSaveable { mutableStateOf(false) }
                    if (current.expired && !loginRequested) {
                        Box(Modifier.safeDrawingPadding()) {
                            ErrorState(ApiError.NOT_SIGNED_IN, onRetry = {}, onSignIn = { loginRequested = true })
                        }
                    } else {
                        LoginScreen(form, onSignIn = auth::signIn)
                    }
                }
                AuthState.MustChangePassword -> PasswordScreen(
                    form, onSave = auth::changePassword, startPasswordKnown = auth.startPasswordKnown.collectAsState().value,
                )
                is AuthState.Welcome -> if (welcomePrivacyOpen) {
                    BackHandler { welcomePrivacyOpen = false }
                    Box(Modifier.safeDrawingPadding()) {
                        ContentWidth {
                            PrivacyScreen(
                                privacy.state.collectAsState().value, stringResource(R.string.privacy_back_welcome),
                                onBack = { welcomePrivacyOpen = false }, onRetry = privacy::load,
                            )
                        }
                    }
                } else WelcomeScreen(
                    name = current.profile.name,
                    squads = current.profile.squads.map { it.name },
                    participants = current.profile.participants,
                    consentText = current.consent.text,
                    passwordSaved = passwordSaved,
                    form = form,
                    onReadPrivacy = { welcomePrivacyOpen = true },
                    onAccept = auth::acceptConsent,
                    lockAvailable = lockAvailable,
                    lockEnabled = lockEnabled,
                    onLockChange = onLockChange,
                )
                is AuthState.SignedIn -> if (current.profile.hasNoSquad) NoSquadScreen(onBackToLogin = auth::signOut) else CompositionLocalProvider(LocalViewModelStoreOwner provides current.scope) {
                    // Die Bilder der Sitzung liegen im Speicher der Sitzung und verschwinden mit ihr.
                    val images = viewModel { ProfileImages(ProfileImageApi(client)) }
                    val rankImages = viewModel { RankImages(RankImageApi(client)::load) }
                    CompositionLocalProvider(LocalProfileImages provides images, LocalRankImages provides rankImages) {
                        MainContent(
                            AppointmentsApi(client), PushApi(client), current.profile,
                            passwordSaved, form, imageForm, auth,
                            pushTarget, onPushTargetHandled,
                            privacy = privacy,
                            lockAvailable = lockAvailable,
                            lockEnabled = lockEnabled,
                            onLockChange = onLockChange,
                        )
                    }
                }
            }
        }
        // Über dem Start gibt es keinen zweiten Ladekreis.
        BusyOverlay(busy = pending > 0 && state !is AuthState.Loading)
    }
}

/** Der Bildschirm, den `MainContent` zeigt; `depth` bestimmt die Richtung des Übergangs (tiefer: von rechts herein). */
private sealed interface Screen {
    val depth: Int

    data object Home : Screen { override val depth = 0 }
    data object Notifications : Screen { override val depth = 1 }
    data object Privacy : Screen { override val depth = 1 }
    data class CropImage(val uri: Uri) : Screen { override val depth = 1 }
    data class Appointment(val appointmentId: String) : Screen { override val depth = 1 }
    data class Registrations(val appointmentId: String) : Screen { override val depth = 2 }
}

@Composable
private fun MainContent(
    appointmentsApi: AppointmentsApi,
    pushApi: PushApi,
    account: Profile,
    passwordSaved: Boolean,
    form: FormState,
    imageForm: FormState,
    auth: AuthViewModel,
    pushTarget: PushTarget?,
    onPushTargetHandled: () -> Unit,
    privacy: PrivacyViewModel,
    lockAvailable: Boolean,
    lockEnabled: Boolean,
    onLockChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val appointments = viewModel { AppointmentsViewModel(appointmentsApi) }
    // Lädt die Liste (Start, nach einer Antwort), zeigt das Widget den neuen Stand gleich, ohne eigene Anfrage.
    val appointmentList by appointments.list.collectAsState()
    LaunchedEffect(appointmentList) {
        (appointmentList as? ApiResult.Ok)?.let { withContext(Dispatchers.IO) { AppointmentWidget.show(context, it.value.appointments) } }
    }
    val push = viewModel { PushViewModel(pushApi, account) }
    var notificationsOpen by rememberSaveable { mutableStateOf(false) }
    var destination by rememberSaveable { mutableStateOf(Destination.APPOINTMENTS) }
    var openedAppointment by rememberSaveable { mutableStateOf<String?>(null) }
    var viewingRegistrations by rememberSaveable { mutableStateOf(false) }
    var privacyOpen by rememberSaveable { mutableStateOf(false) }
    // Das gewählte Profilfoto, solange die Person den Ausschnitt wählt.
    var croppingImage by rememberSaveable { mutableStateOf<Uri?>(null) }
    LaunchedEffect(privacyOpen) { if (privacyOpen) privacy.load() }

    // Pushes brauchen das Token dieses Geräts beim Server und, ab Android 13, die Erlaubnis (einmal fragen, danach im Profil).
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(account.id) {
        push.registerDevice { PushToken.fetch(context) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && Notifications.shouldAskOnce(context)) {
            askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    // Das Tippen auf einen Push öffnet das Termin und schliesst, was davor offen war.
    LaunchedEffect(pushTarget) {
        val target = pushTarget ?: return@LaunchedEffect
        notificationsOpen = false; viewingRegistrations = false; privacyOpen = false
        when (target) {
            is PushTarget.Appointment -> { destination = Destination.APPOINTMENTS; openedAppointment = target.id }
        }
        onPushTargetHandled()
    }
    // Auch nach Drehen oder Prozess-Ende lädt das Offene neu: Daten bleiben nur im Arbeitsspeicher.
    LaunchedEffect(openedAppointment) { openedAppointment?.let(appointments::loadDetail) }
    LaunchedEffect(viewingRegistrations, openedAppointment) {
        val id = openedAppointment
        if (viewingRegistrations && id != null) appointments.loadRegistrations(id)
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    Surface(Modifier.fillMaxSize()) {
        val appointmentId = openedAppointment
        val detail by appointments.detail.collectAsState()
        val appointmentProfile = (appointments.profile.collectAsState().value as? ApiResult.Ok)?.value
        val assignedAppointment = (detail as? ApiResult.Ok)?.value?.takeIf { it.id == appointmentId }
        // Ab 600 dp stehen Liste und Detail nebeneinander auf dem Hauptbildschirm.
        val wide = LocalConfiguration.current.screenWidthDp >= WideBreakpoint.value
        val detailPane: @Composable (String) -> Unit = { id ->
            LaunchedEffect(id) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    while (true) {
                        delay(DETAIL_POLL_INTERVAL_MS)
                        appointments.refreshDetail()
                    }
                }
            }
            AppointmentDetailScreen(
                detail,
                appointments.rsvp.collectAsState().value,
                canSeeRegistrations = assignedAppointment?.let { RegistrationsRules.maySee(it, appointmentProfile) } == true,
                ownName = account.name,
                ownImageUrl = account.imageUrl,
                onParticipant = { participantId, attending -> appointments.rsvpParticipant(id, participantId, attending) },
                onLeader = { attending -> appointments.rsvpLeader(id, attending) },
                onRegistrations = { viewingRegistrations = true },
                onBack = { openedAppointment = null },
                onRetry = { appointments.loadDetail(id) },
            )
        }
        val cropping = croppingImage
        val target = when {
            appointmentId != null && viewingRegistrations -> Screen.Registrations(appointmentId)
            appointmentId != null && !wide -> Screen.Appointment(appointmentId)
            cropping != null -> Screen.CropImage(cropping)
            privacyOpen -> Screen.Privacy
            notificationsOpen -> Screen.Notifications
            else -> Screen.Home
        }
        val motion = rememberMotionEnabled()
        AnimatedContent(
            target,
            transitionSpec = { screenTransition(initialState.depth, targetState.depth, motion) },
            label = "bildschirm",
        ) { screen ->
            when (screen) {
                is Screen.Registrations -> {
                    val appointmentId = screen.appointmentId
                    BackHandler { viewingRegistrations = false }
                    val date = assignedAppointment?.startDay()?.let { "${weekdayShort(it)} ${it.dayOfMonth}.${it.monthValue}." }
                    MainScaffold(Destination.APPOINTMENTS, onDestination = { destination = it; openedAppointment = null; viewingRegistrations = false }) {
                        ContentWidth {
                        RegistrationsScreen(
                            eyebrow = listOfNotNull(assignedAppointment?.title, date).joinToString(" · ").ifBlank { null },
                            state = appointments.registrations.collectAsState().value,
                            onBack = { viewingRegistrations = false },
                            onRetry = { appointments.loadRegistrations(appointmentId) },
                        )
                        }
                    }
                }
                is Screen.Appointment -> {
                    val appointmentId = screen.appointmentId
                    BackHandler { openedAppointment = null }
                    MainScaffold(Destination.APPOINTMENTS, onDestination = { destination = it; openedAppointment = null }) {
                        detailPane(appointmentId)
                    }
                }
                is Screen.CropImage -> {
                    BackHandler { croppingImage = null }
                    ContentWidth {
                        CropImageScreen(
                            screen.uri,
                            onCancel = { croppingImage = null },
                            onConfirm = { crop ->
                                croppingImage = null
                                auth.uploadImage { withContext(Dispatchers.Default) { ProfileImageEncoder.encode(resolver, screen.uri, crop) } }
                            },
                        )
                    }
                }
                Screen.Privacy -> {
                    BackHandler { privacyOpen = false }
                    MainScaffold(Destination.PROFILE, onDestination = { destination = it; privacyOpen = false }) {
                        ContentWidth {
                            PrivacyScreen(
                                privacy.state.collectAsState().value, stringResource(R.string.privacy_back_profile),
                                onBack = { privacyOpen = false }, onRetry = privacy::load,
                            )
                        }
                    }
                }
                Screen.Notifications -> {
                    val leave = { notificationsOpen = false; push.clearError() }
                    BackHandler(onBack = leave)
                    ContentWidth {
                    NotificationsScreen(
                        switches = push.switches.collectAsState().value,
                        form = push.form.collectAsState().value,
                        onAppointments = push::setAppointments,
                        onRegistrations = push::setRegistrations,
                        onBack = leave,
                    )
                    }
                }
                Screen.Home -> MainScaffold(destination, onDestination = { destination = it }) {
                    AnimatedContent(
                        destination,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = { fadeTransition(motion) },
                        label = "bereich",
                    ) { tab ->
                        if (tab == Destination.APPOINTMENTS) {
                            val list: @Composable () -> Unit = {
                                AppointmentListScreen(
                                    appointments.list.collectAsState().value,
                                    onOpen = { openedAppointment = it },
                                    onRetry = appointments::loadList,
                                )
                            }
                            if (wide) {
                                TwoPane(list = list, detail = { if (appointmentId != null) detailPane(appointmentId) else DetailPlaceholder() })
                            } else {
                                list()
                            }
                        } else if (tab == Destination.PROFILE) ContentWidth {
                            ProfileScreen(
                                account, passwordSaved, form, imageForm,
                                onPickImage = { uri -> croppingImage = uri },
                                onRemoveImage = auth::removeImage,
                                onOpenNotifications = { notificationsOpen = true },
                                onOpenPrivacy = { privacyOpen = true },
                                onSignOut = auth::signOut,
                                onLeave = { auth.clearPasswordSaved(); auth.clearImageError() },
                                lockAvailable = lockAvailable,
                                lockEnabled = lockEnabled,
                                onLockChange = onLockChange,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Rechte Spalte im Querformat, solange keine Übung offen ist. */
@Composable
private fun DetailPlaceholder() {
    Box(Modifier.fillMaxSize().padding(Spacing.s6), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.detail_placeholder), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
