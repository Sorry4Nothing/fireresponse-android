package io.github.sorry4nothing.cevios.programs

import io.github.sorry4nothing.cevios.chat.Profile
import io.github.sorry4nothing.cevios.contacts.ContactList
import io.github.sorry4nothing.cevios.net.ApiClient
import io.github.sorry4nothing.cevios.net.ApiResult
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
private data class RsvpRequest(val status: String)

class ProgramsApi(private val api: ApiClient) {
    /** Programme, die das Fenster `from` bis `until` (Schweizer Kalendertage, inklusive) berühren. */
    suspend fun list(from: LocalDate, until: LocalDate): ApiResult<ProgramList> =
        api.get("/api/v1/programme?von=$from&bis=$until")

    suspend fun read(id: String): ApiResult<Program> = api.get("/api/v1/programme/$id")

    /** Meldet ein eigenes Kind an («ja») oder ab («nein»); eine frühere Antwort wird überschrieben. */
    suspend fun setAttendance(id: String, childId: String, attending: Boolean): ApiResult<Unit> =
        api.put("/api/v1/programme/$id/teilnahmen/$childId", rsvpBody(attending))

    /** Dasselbe für die eigene Anmeldung als Leitung. */
    suspend fun setLeaderRsvp(id: String, attending: Boolean): ApiResult<Unit> =
        api.put("/api/v1/programme/$id/leitung", rsvpBody(attending))

    /** Setzt Stufen und Sichtbarkeit; die App schickt den vollen neuen Stand, wie die API es verlangt. */
    suspend fun assign(id: String, request: AssignmentRequest): ApiResult<Unit> =
        api.put("/api/v1/programme/$id/zuordnung", request)

    suspend fun ageGroups(): ApiResult<AgeGroupList> = api.get("/api/v1/stufen")

    /** Die Stufenleitungen der Stufen des Programms, ohne die, die abgesagt haben («nein»); der Hauptkontakt steht zuerst. Leer nur, wenn die Stufe keine SL hat. */
    suspend fun contacts(id: String): ApiResult<ContactList> = api.get("/api/v1/programme/$id/kontakte")

    /** Zahlen und Listen der Anmeldungen (nur ansehen): Admins sowie Leitende und Stufenleitungen der Stufen des Programms. */
    suspend fun registrations(id: String): ApiResult<Registrations> = api.get("/api/v1/programme/$id/teilnahmen")

    suspend fun profile(): ApiResult<Profile> = api.get("/api/v1/ich")

    private fun rsvpBody(attending: Boolean) = RsvpRequest(if (attending) "ja" else "nein")
}
