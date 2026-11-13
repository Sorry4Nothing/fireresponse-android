package ch.fireresponse.app.appointments

import ch.fireresponse.app.contacts.Contact
import ch.fireresponse.app.profile.Badge
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

/** Die Einheit rechnet in Schweizer Zeit, unabhängig von der Zone des Geräts. */
val SWISS_ZONE: ZoneId = ZoneId.of("Europe/Zurich")

/** Die Einheit eines Termins; `type` ist «jf» (Jugendfeuerwehr) oder «kf» (Kinderfeuerwehr). */
@Serializable
data class AppointmentSquad(
    val id: String,
    val name: String,
    @SerialName("typ") val type: String = "jf",
)

/** Eigene teilnehmende Person in der Einheit des Termins. `status` ist «ja», «nein» oder `null` für noch offen. */
@Serializable
data class AppointmentParticipant(
    val id: String,
    @SerialName("vorname") val firstName: String,
    val status: String? = null,
    @SerialName("abzeichen") val badge: Badge? = null,
)

/** Listeneintrag und Detail der API. Die Felder ab `topic` liefert nur das Detail. */
@Serializable
data class Appointment(
    val id: String,
    @SerialName("titel") val title: String,
    val status: String,
    @SerialName("ganztaegig") val allDay: Boolean,
    @SerialName("beginn") val start: String? = null,
    @SerialName("ende") val end: String? = null,
    @SerialName("tagVon") val dayFrom: String? = null,
    @SerialName("tagBis") val dayTo: String? = null,
    /** Schweizer Kalendertag (`JJJJ-MM-TT`), ab dem sich anmelden lässt; die App rechnet das Fenster nicht selbst. */
    @SerialName("anmeldungAb") val registrationFrom: String? = null,
    /** Wahr, solange der Termin aktiv ist, nicht begonnen hat und das Anmeldefenster offen ist: nur dann lässt sich antworten. */
    @SerialName("teilnahmeOffen") val attendanceOpen: Boolean = false,
    /** Art des Termins laut Lodur (Sitzung, Übung, Kurs …); `null`, wenn die Quelle sie nicht liefert. */
    @SerialName("art") val kind: String? = null,
    @SerialName("einheit") val squad: AppointmentSquad,
    @SerialName("teilnehmende") val participants: List<AppointmentParticipant> = emptyList(),
    @SerialName("thema") val topic: String? = null,
    val info: String? = null,
    @SerialName("aufgebot") val callUp: String? = null,
    /** Die Kontaktperson des Termins; `null`, wenn die Einheit keine Kommandantin und keinen Kommandanten hat. */
    @SerialName("kontakt") val contact: Contact? = null,
    /** «ja», «nein» oder «offen»; `null`, wenn die Person nicht zum Kommando oder zur Leitung der Einheit gehört. */
    @SerialName("eigeneAnmeldung") val ownRegistration: String? = null,
) {
    val cancelled get() = status == "abgesagt"
    val inReview get() = status == "pruefen"
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

/** Der Tag, ab dem sich anmelden lässt, solange das noch aussteht (aktiv, Fenster zu, Tag in der Zukunft); sonst `null`. */
fun Appointment.registrationOpensOn(today: LocalDate): LocalDate? =
    registrationFrom?.let { runCatching { LocalDate.parse(it) }.getOrNull() }?.takeIf { !cancelled && !attendanceOpen && it.isAfter(today) }
