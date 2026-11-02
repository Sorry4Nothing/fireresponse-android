package ch.fireresponse.app.appointments

import ch.fireresponse.app.profile.Profile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Die Einheiten der Einheit (`GET /api/v1/stufen`); gelesen werden nur ID und Name. */
@Serializable
data class SquadList(@SerialName("stufen") val squads: List<AppointmentSquad>)

/** Der volle neue Stand der Zuordnung (`PUT /api/v1/programme/{id}/zuordnung`), kein Hinzufügen einzelner Einträge. */
@Serializable
data class AssignmentRequest(
    @SerialName("stufen") val squadIds: List<String>,
    @SerialName("sichtbarkeit") val visibility: String,
)

/** Wer ein Termin ausser Admins und Kommandos sieht. Die Reihenfolge ist die der Einheiten im Dialog. */
enum class Visibility(val api: String) {
    INTERN("intern"),
    LEADERS("leitende"),
    PARENTS("eltern"),
}

/**
 * Zuordnung von Einheiten und Sichtbarkeit. Admins dürfen alles, Kommandos nur Einheiten, in denen sie selbst Kommando
 * sind. Leitende und Eltern nie. Die Entscheidung trifft der Server; eine Aktion, die er ablehnt, zeigt die App als Fehler.
 */
object AssignmentRules {
    fun mayAssign(profile: Profile?) = profile != null && (profile.admin || profile.roles.any { it.type == "stufenleitung" })

    /** Die Einheiten, die die Person zuordnen darf. */
    fun choices(all: List<AppointmentSquad>, profile: Profile): List<AppointmentSquad> =
        if (profile.admin) all else all.filter { profile.isCommander(it.id) }

    /** Einheiten, die das Termin schon hat und die die Person nicht ändern darf: Sie bleiben im neuen Stand. */
    fun fixed(appointment: Appointment, choices: List<AppointmentSquad>): List<AppointmentSquad> =
        appointment.squads.filter { group -> choices.none { it.id == group.id } }

    /** Die Sichtbarkeit ändern darf nur, wem alle bisherigen und neuen Einheiten gehören: Es darf keine fremde Einheit bleiben. */
    fun mayChangeVisibility(fixed: List<AppointmentSquad>) = fixed.isEmpty()

    /** Ohne Einheit ist nur «intern» erlaubt. */
    fun valid(selected: Set<String>, fixed: List<AppointmentSquad>, visibility: Visibility) =
        selected.isNotEmpty() || fixed.isNotEmpty() || visibility == Visibility.INTERN

    fun request(selected: Set<String>, fixed: List<AppointmentSquad>, visibility: Visibility) =
        AssignmentRequest((selected + fixed.map { it.id }).toList(), visibility.api)
}
