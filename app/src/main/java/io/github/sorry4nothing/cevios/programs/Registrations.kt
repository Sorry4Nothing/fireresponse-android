package io.github.sorry4nothing.cevios.programs

import io.github.sorry4nothing.cevios.chat.Profile
import io.github.sorry4nothing.cevios.chat.displayName
import io.github.sorry4nothing.cevios.ui.ChipKind
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wie viele «ja», «nein» und «offen» (noch keine Antwort) gesagt haben. */
@Serializable
data class Counts(val ja: Int = 0, val nein: Int = 0, val offen: Int = 0) {
    val total get() = ja + nein + offen
}

@Serializable
data class RegistrationCounts(@SerialName("kinder") val children: Counts, @SerialName("leitende") val leaders: Counts)

@Serializable
data class ChildRegistration(
    val id: String,
    @SerialName("vorname") val firstName: String,
    @SerialName("nachname") val lastName: String,
    val status: String,
) {
    val name get() = "$firstName $lastName".trim()
}

/** `role` ist «leitung» oder «stufenleitung». */
@Serializable
data class LeaderRegistration(
    val id: String,
    val name: String,
    @SerialName("rolle") val role: String,
    val status: String,
    /** Cevi-Name (Spitzname); `null` oder leer ohne Angabe. */
    val ceviName: String? = null,
    /** Pfad zum Profilbild (`GET /api/v1/personen/{id}/bild`); `null` ohne Bild. Kinder haben keins. */
    @SerialName("bildUrl") val imageUrl: String? = null,
) {
    val isGroupLeader get() = role == "stufenleitung"

    /** Titel der Zeile: der Cevi-Name, ohne ihn der volle Name. */
    val title get() = displayName(name, ceviName)

    /** Der volle Name darunter; nur, wenn der Titel ein Cevi-Name ist. */
    val fullName get() = name.takeIf { title != name }
}

/** `GET /api/v1/programme/{id}/teilnahmen`: Zahlen und Listen. Sie zählen genau die gelieferten Listen. */
@Serializable
data class Registrations(
    @SerialName("zahlen") val counts: RegistrationCounts,
    @SerialName("kinder") val children: List<ChildRegistration>,
    @SerialName("leitende") val leaders: List<LeaderRegistration>,
)

/** «ja», «nein» oder «offen» als Chip. Was der Server sonst schickt, gilt als offen. */
fun registrationChip(status: String): ChipKind = when (status) {
    "ja" -> ChipKind.YES
    "nein" -> ChipKind.NO
    else -> ChipKind.PENDING
}

/** Wer die Übersicht ansehen darf; entschieden wird trotzdem beim Server (403). Ändern darf hier niemand etwas. */
object RegistrationsRules {
    /** Admins sowie Leitende und Stufenleitungen einer Stufe des Programms. Ohne Stufe gibt es nichts zu zählen. */
    fun maySee(program: Program, profile: Profile?): Boolean {
        if (profile == null || program.unassigned) return false
        return profile.admin || profile.roles.any { role -> program.ageGroups.any { it.id == role.ageGroupId } }
    }

    /** Anteil eines Wertes am Ganzen für die Breite des Balkens; 0, wenn niemand zählt. */
    fun share(count: Int, total: Int): Float = if (total <= 0) 0f else count.toFloat() / total
}
