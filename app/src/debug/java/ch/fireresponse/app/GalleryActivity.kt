package ch.fireresponse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ch.fireresponse.app.appointments.Appointment
import ch.fireresponse.app.appointments.AppointmentDetailScreen
import ch.fireresponse.app.appointments.AppointmentList
import ch.fireresponse.app.appointments.AppointmentListScreen
import ch.fireresponse.app.appointments.AppointmentParticipant
import ch.fireresponse.app.appointments.AppointmentSquad
import ch.fireresponse.app.appointments.Counts
import ch.fireresponse.app.appointments.LeaderRegistration
import ch.fireresponse.app.appointments.ParticipantRegistration
import ch.fireresponse.app.appointments.RegistrationCounts
import ch.fireresponse.app.appointments.Registrations
import ch.fireresponse.app.appointments.RegistrationsScreen
import ch.fireresponse.app.appointments.RsvpState
import ch.fireresponse.app.auth.FormState
import ch.fireresponse.app.auth.LockedScreen
import ch.fireresponse.app.auth.LoginScreen
import ch.fireresponse.app.auth.NoSquadScreen
import ch.fireresponse.app.auth.PasswordScreen
import ch.fireresponse.app.auth.ProfileScreen
import ch.fireresponse.app.auth.WelcomeScreen
import ch.fireresponse.app.contacts.Contact
import ch.fireresponse.app.contacts.ContactList
import ch.fireresponse.app.net.ApiError
import ch.fireresponse.app.net.ApiResult
import ch.fireresponse.app.profile.Profile
import ch.fireresponse.app.profile.ProfileParticipant
import ch.fireresponse.app.profile.ProfileSquad
import ch.fireresponse.app.push.NotificationsScreen
import ch.fireresponse.app.push.PushViewModel
import ch.fireresponse.app.ui.Destination
import ch.fireresponse.app.ui.MainScaffold
import ch.fireresponse.app.ui.theme.FireResponseTheme

/**
 * Nur im Debug-Build: zeigt einen Bildschirm mit erfundenen Beispieldaten, damit Gestaltung und Querformat ohne Server zu sehen
 * sind. Start mit `--es screen <name>`; die Namen stehen in [screens].
 */
class GalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val name = intent.getStringExtra("screen") ?: "login"
        setContent { FireResponseTheme { Surface(Modifier.fillMaxSize()) { screens[name]?.invoke() } } }
    }
}

private val squad = AppointmentSquad("s1", "Jugendfeuerwehr Musterdorf")

private val appointments = listOf(
    Appointment(
        id = "a1", title = "Schläuche kuppeln", status = "aktiv", allDay = false,
        start = "2026-11-14T13:00:00.000Z", end = "2026-11-14T16:00:00.000Z",
        googleLocation = "Feuerwehrmagazin", meetingPoint = "13:45 Uhr\nParkplatz beim Magazin",
        closingLocation = "Feuerwehrmagazin", closingTime = "17:00", bringAlong = "Festes Schuhwerk und eine Trinkflasche",
        squads = listOf(squad), attendanceOpen = true,
        participants = listOf(AppointmentParticipant("k1", "Lena", "ja"), AppointmentParticipant("k2", "Noah", null)),
    ),
    Appointment(
        id = "a2", title = "Knotenkunde", status = "aktiv", allDay = false,
        start = "2026-11-14T17:00:00.000Z", end = "2026-11-14T18:30:00.000Z", googleLocation = "Schulhaus",
        squads = listOf(squad), attendanceOpen = true,
        participants = listOf(AppointmentParticipant("k1", "Lena", "nein")),
    ),
    Appointment(
        id = "a3", title = "Leiterübung", status = "abgesagt", allDay = false,
        start = "2026-11-21T13:00:00.000Z", end = "2026-11-21T15:00:00.000Z", googleLocation = "Feuerwehrmagazin",
        squads = listOf(squad),
    ),
    Appointment(
        id = "a4", title = "Leitendensitzung", status = "aktiv", allDay = false,
        start = "2026-11-28T18:00:00.000Z", end = "2026-11-28T19:30:00.000Z", googleLocation = "Magazin",
        squads = listOf(squad), attendanceOpen = true, ownRegistration = "ja",
    ),
)

private val profile = Profile(
    id = "u1", name = "Sandra Muster", username = "smuster",
    squads = listOf(ProfileSquad("s1", "Jugendfeuerwehr Musterdorf")),
    participants = listOf(ProfileParticipant("k1", "s1", "Lena"), ProfileParticipant("k2", "s1", "Noah")),
)

private val registrations = Registrations(
    counts = RegistrationCounts(Counts(ja = 11, nein = 3, offen = 6), Counts(ja = 3, nein = 0, offen = 1)),
    participants = listOf(
        ParticipantRegistration("k1", "Lena", "Muster", "offen"),
        ParticipantRegistration("k2", "Noah", "Beispiel", "ja"),
        ParticipantRegistration("k3", "Mia", "Probe", "nein"),
    ),
    leaders = listOf(LeaderRegistration("l1", "Petra Beispiel", "leitung", "ja")),
)

private val screens: Map<String, @Composable () -> Unit> = mapOf(
    "login" to { LoginScreen(FormState()) { _, _ -> } },
    "loginError" to { LoginScreen(FormState(error = R.string.login_failed)) { _, _ -> } },
    "password" to { PasswordScreen(first = true, form = FormState(), onSave = { _, _ -> }, startPasswordKnown = true) },
    "welcome" to {
        WelcomeScreen(
            name = profile.name, squads = profile.squads.map { it.name }, participants = profile.participants.map { it.firstName },
            passwordSaved = true, form = FormState(), onReadPrivacy = {}, onAccept = {}, lockAvailable = true,
        )
    },
    "locked" to { LockedScreen(FormState(), onUnlock = {}, onUsePassword = {}) },
    "nosquad" to { NoSquadScreen {} },
    "list" to {
        MainScaffold(Destination.APPOINTMENTS, {}) { AppointmentListScreen(ApiResult.Ok(AppointmentList(appointments)), {}, {}) }
    },
    "empty" to {
        MainScaffold(Destination.APPOINTMENTS, {}) { AppointmentListScreen(ApiResult.Ok(AppointmentList(emptyList())), {}, {}) }
    },
    "offline" to { MainScaffold(Destination.APPOINTMENTS, {}) { AppointmentListScreen(ApiResult.Failure(ApiError.NETWORK), {}, {}) } },
    "detail" to {
        MainScaffold(Destination.APPOINTMENTS, {}) {
            AppointmentDetailScreen(
                ApiResult.Ok(appointments[0]), RsvpState(), canAssign = false, canSeeRegistrations = true,
                contacts = ApiResult.Ok(ContactList(listOf(Contact("c1", "Petra Beispiel", phone = "079 000 00 00")))),
                onParticipant = { _, _ -> }, onLeader = {}, onAssign = {}, onRegistrations = {}, onBack = {}, onRetry = {},
            )
        }
    },
    "registrations" to {
        MainScaffold(Destination.APPOINTMENTS, {}) {
            RegistrationsScreen("Schläuche kuppeln · SA 14.11.", ApiResult.Ok(registrations), onBack = {}, onRetry = {})
        }
    },
    "profile" to {
        MainScaffold(Destination.PROFILE, {}) {
            ProfileScreen(
                profile, passwordSaved = false, form = FormState(), imageForm = FormState(), onPickImage = {}, onRemoveImage = {},
                onOpenNotifications = {}, onOpenPrivacy = {}, onSignOut = {}, onLeave = {}, lockAvailable = true, lockEnabled = true,
            )
        }
    },
    "notifications" to {
        NotificationsScreen(PushViewModel.Switches(appointments = true, registrations = false), FormState(), {}, {}, {})
    },
)
