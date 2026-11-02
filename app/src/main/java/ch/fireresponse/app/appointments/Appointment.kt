package ch.fireresponse.app.appointments

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

/** Die Einheit rechnet in Schweizer Zeit, unabhängig von der Zone des Geräts. */
val SWISS_ZONE: ZoneId = ZoneId.of("Europe/Zurich")

@Serializable
data class AppointmentSquad(
    val id: String,
    val name: String,
)

/** Eigene teilnehmende Person in einer Einheit des Termins. `status` ist «ja», «nein» oder `null` für noch offen. */
@Serializable
data class AppointmentParticipant(val id: String, @SerialName("vorname") val firstName: String, val status: String? = null)

/** Listeneintrag und Detail der API. Die Felder ab `attendanceOpen` liefert nur das Detail. */
@Serializable
data class Appointment(
    val id: String,
    @SerialName("titel") val title: String,
    val status: String,
    /** «intern», «leitende» oder «eltern»: wer das Termin ausser Admins und Kommandos sieht. */
    @SerialName("sichtbarkeit") val visibility: String = "intern",
    @SerialName("ganztaegig") val allDay: Boolean,
    @SerialName("beginn") val start: String? = null,
    @SerialName("ende") val end: String? = null,
    @SerialName("tagVon") val dayFrom: String? = null,
    @SerialName("tagBis") val dayTo: String? = null,
    @SerialName("googleOrt") val googleLocation: String? = null,
    @SerialName("treffpunkt") val meetingPoint: String? = null,
    @SerialName("schlussOrt") val closingLocation: String? = null,
    @SerialName("schlussZeit") val closingTime: String? = null,
    @SerialName("mitnehmen") val bringAlong: String? = null,
    @SerialName("stufen") val squads: List<AppointmentSquad> = emptyList(),
    /** Wahr, solange das Termin aktiv ist und nicht begonnen hat: nur dann lässt sich antworten. */
    @SerialName("teilnahmeOffen") val attendanceOpen: Boolean = false,
    @SerialName("kinder") val participants: List<AppointmentParticipant> = emptyList(),
    /** «ja», «nein» oder «offen»; `null`, wenn die Person keine Leitung dieses Termins ist. */
    @SerialName("eigeneAnmeldung") val ownRegistration: String? = null,
) {
    val cancelled get() = status == "abgesagt"
    val inReview get() = status == "pruefen"
    val unassigned get() = squads.isEmpty()

    /** Der Treffpunkt der Leitung, sonst der Ort aus dem Google-Kalender. */
    val location get() = parseMeetingPoint(meetingPoint).place ?: googleLocation?.takeIf { it.isNotBlank() }
}

/** Zeit und Ort des Treffpunkts, wie der Server sie in einem Text liefert. */
data class MeetingPoint(val time: String?, val place: String?)

private val MEETING_TIME = Regex("""^(\d{2}:\d{2}) Uhr$""")

/** Der Server schreibt «14:00 Uhr» in die erste Zeile, wenn die Webseite eine Zeit nennt; der Rest ist der Ort. */
fun parseMeetingPoint(text: String?): MeetingPoint {
    val lines = text.orEmpty().lines().map { it.trim() }.filter { it.isNotEmpty() }
    val time = lines.firstOrNull()?.let { MEETING_TIME.matchEntire(it)?.groupValues?.get(1) }
    val place = (if (time != null) lines.drop(1) else lines).joinToString("\n").ifEmpty { null }
    return MeetingPoint(time, place)
}

@Serializable
data class AppointmentList(@SerialName("programme") val appointments: List<Appointment>)

sealed interface Timing {
    /** Schweizer Kalendertage, `until` ist inklusive. */
    data class AllDay(val from: LocalDate, val until: LocalDate) : Timing

    data class Timed(val start: ZonedDateTime, val end: ZonedDateTime) : Timing
}

/** `null`, wenn die Angaben des Servers fehlen oder kein Datum sind. */
fun Appointment.time(): Timing? = try {
    if (allDay) {
        Timing.AllDay(LocalDate.parse(dayFrom ?: return null), LocalDate.parse(dayTo ?: return null))
    } else {
        Timing.Timed(
            Instant.parse(start ?: return null).atZone(SWISS_ZONE),
            Instant.parse(end ?: return null).atZone(SWISS_ZONE),
        )
    }
} catch (e: DateTimeParseException) {
    null
}
