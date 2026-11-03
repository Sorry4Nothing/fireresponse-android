package ch.fireresponse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import ch.fireresponse.app.images.LocalRankImages
import ch.fireresponse.app.images.RankImages
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
import ch.fireresponse.app.auth.Privacy
import ch.fireresponse.app.auth.PrivacyScreen
import ch.fireresponse.app.auth.PrivacySection
import ch.fireresponse.app.auth.PasswordScreen
import ch.fireresponse.app.auth.StartScreen
import ch.fireresponse.app.auth.ProfileScreen
import ch.fireresponse.app.auth.WelcomeScreen
import ch.fireresponse.app.contacts.Contact
import ch.fireresponse.app.profile.Badge
import ch.fireresponse.app.net.ApiError
import ch.fireresponse.app.net.ApiResult
import ch.fireresponse.app.profile.Profile
import ch.fireresponse.app.profile.ProfileParticipant
import ch.fireresponse.app.profile.ProfileSquad
import ch.fireresponse.app.push.NotificationsScreen
import ch.fireresponse.app.push.PushViewModel
import ch.fireresponse.app.ui.Destination
import ch.fireresponse.app.ui.ErrorState
import ch.fireresponse.app.ui.MainScaffold
import ch.fireresponse.app.ui.TwoPane
import ch.fireresponse.app.ui.theme.FireResponseTheme

/** Beispiel eines Bild-Abzeichens vom Server (SVG wie die Miliz-Ränge): Die Galerie liefert es ohne Netz. */
private const val RANK_URL = "/api/v1/raenge/7c9e6679-7425-40de-944b-e07fc1f90ae7/abzeichen"
private const val RANK_SVG = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="-230.37 102.85 1166.57 1516.54"><g><path d="M0,1516.537h705.827V205.7H0V1516.537z"/><path fill="#EDD010" d="M93.543,1363.464h518.74v-34.017H93.543V1363.464z"/></g></svg>"""
private const val LOGO_SVG = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="50" cy="50" r="48" fill="#1B2A5C"/><path d="M50 18c10 16 22 24 22 40a22 22 0 0 1-44 0c0-16 12-24 22-40z" fill="#E8B22E"/></svg>"""

/**
 * Nur im Debug-Build: zeigt einen Bildschirm mit erfundenen Beispieldaten, damit Gestaltung und Querformat ohne Server zu sehen
 * sind. Start mit `--es screen <name>`; die Namen stehen in [screens].
 */
class GalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val name = intent.getStringExtra("screen") ?: "login"
        // Abzeichen: das Beispiel-SVG. Logos: die erste Einheit hat eines, die zweite keins (dann steht «KF» da).
        val rankImages = RankImages { path ->
            when {
                path.endsWith("/s2/logo") -> ApiResult.Failure(ApiError.NOT_FOUND)
                path.endsWith("/logo") -> ApiResult.Ok(LOGO_SVG.toByteArray())
                else -> ApiResult.Ok(RANK_SVG.toByteArray())
            }
        }
        setContent {
            CompositionLocalProvider(LocalRankImages provides rankImages) {
                FireResponseTheme { Surface(Modifier.fillMaxSize()) { screens[name]?.invoke() } }
            }
        }
    }
}

private val squad = AppointmentSquad("s1", "Jugendfeuerwehr Musterdorf")
private val kfSquad = AppointmentSquad("s2", "Kinderfeuerwehr Musterdorf", "kf")

private val contact = Contact("c1", "Petra Beispiel", "kommandant", phone = "079 000 00 00", badge = Badge("Gfr", imageUrl = RANK_URL))

private val appointments = listOf(
    Appointment(
        id = "a1", title = "Schläuche kuppeln", status = "aktiv", allDay = false,
        start = "2026-11-14T13:00:00.000Z", end = "2026-11-14T16:00:00.000Z",
        registrationFrom = "2026-10-03", attendanceOpen = true, squad = squad,
        participants = listOf(AppointmentParticipant("k1", "Lena", "ja", Badge("R1", stars = 1)), AppointmentParticipant("k2", "Noah", null)),
        topic = "Schläuche kuppeln und ausrollen",
        info = "Festes Schuhwerk und eine Trinkflasche mitnehmen.",
        callUp = "Treffpunkt 13:45 Uhr, Parkplatz beim Magazin",
        contact = contact,
    ),
    Appointment(
        id = "a2", title = "Knotenkunde", status = "aktiv", allDay = false,
        start = "2026-11-14T17:00:00.000Z", end = "2026-11-14T18:30:00.000Z",
        registrationFrom = "2026-10-03", attendanceOpen = true, squad = squad,
        participants = listOf(AppointmentParticipant("k1", "Lena", "nein")),
        contact = contact,
    ),
    Appointment(
        id = "a3", title = "Leiterübung", status = "abgesagt", allDay = false,
        start = "2026-11-21T13:00:00.000Z", end = "2026-11-21T15:00:00.000Z",
        registrationFrom = "2026-10-10", squad = squad,
    ),
    Appointment(
        id = "a4", title = "Leitendensitzung", status = "aktiv", allDay = false,
        start = "2026-11-28T18:00:00.000Z", end = "2026-11-28T19:30:00.000Z",
        registrationFrom = "2026-10-17", attendanceOpen = true, squad = squad, ownRegistration = "ja",
    ),
    Appointment(
        id = "a5", title = "Winterlager", status = "aktiv", allDay = true,
        dayFrom = "2027-01-16", dayTo = "2027-01-17", registrationFrom = "2026-12-05", squad = squad,
        participants = listOf(AppointmentParticipant("k1", "Lena", null)),
    ),
    Appointment(
        id = "a6", title = "Vorstandssitzung", status = "aktiv", allDay = false, kind = "Sitzung", registrable = false,
        start = "2027-01-20T18:00:00.000Z", end = "2027-01-20T19:30:00.000Z", squad = kfSquad,
    ),
)

private val profile = Profile(
    id = "u1", name = "Sandra Muster", username = "smuster", consented = true,
    squads = listOf(ProfileSquad("s1", "Jugendfeuerwehr Musterdorf", "jf", "Bezirk Beispiel", "leitung")),
    participants = listOf(ProfileParticipant("k1", "Lena", "Muster", "s1", badge = Badge("R1", stars = 1)), ProfileParticipant("k2", "Noah", "Beispiel", "s1", badge = Badge("R4", stars = 4))),
)

private val privacy = Privacy(
    1,
    listOf(
        PrivacySection("Was wir speichern", "– Deinen Namen, Benutzernamen und deine Rolle\n\n– Bei Eltern: die Namen eurer Kinder\n\n– Deine Zusagen zu Übungen\n\n– Dein Profilbild"),
        PrivacySection("Wer was sieht", "Übungen sehen alle in der Einheit. Zusagen sieht die Leitung."),
        PrivacySection("Was wir nicht machen", "– Keine Werbung und keine Tracker\n\n– Keine Weitergabe an andere\n\n– Keine Namen in Mitteilungen"),
        PrivacySection("Löschen", "Dein Konto löscht die Jugendfeuerwehr. Schreib dafür an die Leitung."),
    ),
)

private val registrations = Registrations(
    counts = RegistrationCounts(Counts(ja = 11, nein = 3, offen = 6), Counts(ja = 3, nein = 0, offen = 1)),
    participants = listOf(
        ParticipantRegistration("k1", "Lena", "Muster", "offen", Badge("R1", stars = 1)),
        ParticipantRegistration("k2", "Noah", "Beispiel", "ja", Badge("R4", stars = 4)),
        ParticipantRegistration("k3", "Mia", "Probe", "nein"),
    ),
    leaders = listOf(LeaderRegistration("l1", "Petra Beispiel", "kommandant", "ja", badge = Badge("Gfr", imageUrl = RANK_URL))),
)

private val screens: Map<String, @Composable () -> Unit> = mapOf(
    "login" to { LoginScreen(FormState()) { _, _ -> } },
    "loginError" to { LoginScreen(FormState(error = R.string.login_failed)) { _, _ -> } },
    "password" to { PasswordScreen(form = FormState(), onSave = { _, _ -> }, startPasswordKnown = true) },
    "welcome" to {
        WelcomeScreen(
            name = profile.name, squads = profile.squads.map { it.name }, participants = profile.participants,
            consentText = "Wir speichern Namen, Telefonnummer und Antworten nur für den Betrieb der Einheit und geben sie nicht weiter.",
            passwordSaved = true, form = FormState(), onReadPrivacy = {}, onAccept = {}, lockAvailable = true,
        )
    },
    "privacy" to {
        MainScaffold(Destination.PROFILE, {}) {
            PrivacyScreen(ApiResult.Ok(privacy), "Zurück zu Profil", onBack = {}, onRetry = {})
        }
    },
    "locked" to { LockedScreen(FormState(), onUnlock = {}, onUsePassword = {}) },
    "nosquad" to { NoSquadScreen {} },
    "list" to {
        MainScaffold(Destination.APPOINTMENTS, {}) { AppointmentListScreen(ApiResult.Ok(AppointmentList(appointments)), {}, {}) }
    },
    "empty" to {
        MainScaffold(Destination.APPOINTMENTS, {}) { AppointmentListScreen(ApiResult.Ok(AppointmentList(emptyList())), {}, {}) }
    },
    "start" to { StartScreen() },
    "loading" to { MainScaffold(Destination.APPOINTMENTS, {}) { AppointmentListScreen(null, {}, {}) } },
    "error" to { MainScaffold(Destination.APPOINTMENTS, {}) { AppointmentListScreen(ApiResult.Failure(ApiError.UNKNOWN), {}, {}) } },
    "notfound" to {
        MainScaffold(Destination.APPOINTMENTS, {}) {
            AppointmentDetailScreen(
                ApiResult.Failure(ApiError.NOT_FOUND), RsvpState(), canSeeRegistrations = false,
                onParticipant = { _, _ -> }, onLeader = {}, onRegistrations = {}, onBack = {}, onRetry = {},
            )
        }
    },
    "noaccess" to { MainScaffold(Destination.APPOINTMENTS, {}) { ErrorState(ApiError.FORBIDDEN, {}, onToAppointments = {}) } },
    "session" to { ErrorState(ApiError.NOT_SIGNED_IN, {}, onSignIn = {}) },
    "wide" to {
        MainScaffold(Destination.APPOINTMENTS, {}) {
            TwoPane(
                list = { AppointmentListScreen(ApiResult.Ok(AppointmentList(appointments)), {}, {}) },
                detail = {
                    AppointmentDetailScreen(
                        ApiResult.Ok(appointments[0]), RsvpState(), canSeeRegistrations = true,
                        onParticipant = { _, _ -> }, onLeader = {}, onRegistrations = {}, onBack = {}, onRetry = {},
                    )
                },
            )
        }
    },
    "offline" to { MainScaffold(Destination.APPOINTMENTS, {}) { AppointmentListScreen(ApiResult.Failure(ApiError.NETWORK), {}, {}) } },
    "detail" to {
        MainScaffold(Destination.APPOINTMENTS, {}) {
            AppointmentDetailScreen(
                ApiResult.Ok(appointments[0]), RsvpState(), canSeeRegistrations = true,
                onParticipant = { _, _ -> }, onLeader = {}, onRegistrations = {}, onBack = {}, onRetry = {},
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
