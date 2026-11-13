package ch.fireresponse.app.appointments

import ch.fireresponse.app.profile.Badge
import ch.fireresponse.app.profile.Profile
import ch.fireresponse.app.ui.ChipKind
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wie viele «ja», «nein» und «offen» (noch keine Antwort) gesagt haben. */
@Serializable
data class Counts(val ja: Int = 0, val nein: Int = 0, val offen: Int = 0) {
    val total get() = ja + nein + offen
}

@Serializable
data class RegistrationCounts(
    @SerialName("teilnehmende") val participants: Counts,
    @SerialName("leitende") val leaders: Counts,
)

@Serializable
data class ParticipantRegistration(
    val id: String,
    @SerialName("vorname") val firstName: String,
    @SerialName("nachname") val lastName: String,
    val status: String,
    @SerialName("abzeichen") val badge: Badge? = null,
) {
    val name get() = "$firstName $lastName".trim()
}

/** Eine Rolle der Einheit mit ihrer Anmeldung; `role` ist «kommandant», «stellvertretung» oder «leitung». */
@Serializable
data class LeaderRegistration(
    val id: String,
    val name: String,
    @SerialName("rolle") val role: String,
    val status: String,
    /** Pfad zum Profilbild (`GET /api/v1/personen/{id}/bild`); `null` ohne Bild. Teilnehmende haben keins. */
    @SerialName("bildUrl") val imageUrl: String? = null,
    @SerialName("abzeichen") val badge: Badge? = null,
) {
    val isCommand get() = role == "kommandant" || role == "stellvertretung"
}

/** `GET /api/v1/programme/{id}/teilnahmen`: Zahlen und Listen. Sie zählen genau die gelieferten Listen. */
@Serializable
data class Registrations(
    @SerialName("zahlen") val counts: RegistrationCounts,
    @SerialName("teilnehmende") val participants: List<ParticipantRegistration>,
    @SerialName("leitende") val leaders: List<LeaderRegistration> = emptyList(),
)

/** «ja», «nein» oder «offen» als Chip. Was der Server sonst schickt, gilt als offen. */
fun registrationChip(status: String): ChipKind = when (status) {
    "ja" -> ChipKind.YES
    "nein" -> ChipKind.NO
    else -> ChipKind.PENDING
}

/** Wer die Übersicht ansehen darf; entschieden wird trotzdem beim Server (403). Ändern darf hier niemand etwas. */
object RegistrationsRules {
    /** Kantonsadmin sowie Kommando und Leitende der Einheit des Termins. Eltern sehen nur ihre eigenen Personen im Detail. */
    fun maySee(appointment: Appointment, profile: Profile?): Boolean =
        profile != null && (profile.admin || profile.squads.any { it.id == appointment.squad.id })

    /** Anteil eines Wertes am Ganzen für die Breite des Balkens; 0, wenn niemand zählt. */
    fun share(count: Int, total: Int): Float = if (total <= 0) 0f else count.toFloat() / total
}
