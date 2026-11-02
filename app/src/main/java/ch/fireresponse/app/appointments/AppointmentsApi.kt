package ch.fireresponse.app.appointments

import ch.fireresponse.app.net.ApiClient
import ch.fireresponse.app.net.ApiResult
import ch.fireresponse.app.profile.Profile
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
private data class RsvpRequest(val status: String)

class AppointmentsApi(private val api: ApiClient) {
    /** Termine aller Einheiten des Kontos, die das Fenster `from` bis `until` (Schweizer Kalendertage, inklusive) berühren. */
    suspend fun list(from: LocalDate, until: LocalDate): ApiResult<AppointmentList> =
        api.get("/api/v1/programme?von=$from&bis=$until")

    /** Das Detail mit Thema, Info, Aufgebot und Kontaktperson. */
    suspend fun read(id: String): ApiResult<Appointment> = api.get("/api/v1/programme/$id")

    /** Meldet eine eigene teilnehmende Person an («ja») oder ab («nein»); eine frühere Antwort wird überschrieben. */
    suspend fun setAttendance(id: String, participantId: String, attending: Boolean): ApiResult<Unit> =
        api.put("/api/v1/programme/$id/teilnahmen/$participantId", rsvpBody(attending))

    /** Dasselbe für die eigene Anmeldung als Kommando oder Leitung. */
    suspend fun setLeaderRsvp(id: String, attending: Boolean): ApiResult<Unit> =
        api.put("/api/v1/programme/$id/leitung", rsvpBody(attending))

    /** Zahlen und Listen der Anmeldungen (nur ansehen). Eltern sehen nur ihre eigenen Personen. */
    suspend fun registrations(id: String): ApiResult<Registrations> = api.get("/api/v1/programme/$id/teilnahmen")

    suspend fun profile(): ApiResult<Profile> = api.get("/api/v1/ich")

    private fun rsvpBody(attending: Boolean) = RsvpRequest(if (attending) "ja" else "nein")
}
