package ch.fireresponse.app.appointments

import android.content.Context
import ch.fireresponse.app.R
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Termine eines Kalendertages (Schweizer Zeit). `date` ist `null`, wenn das Termin keine lesbare Zeit hat. */
data class Day(val date: LocalDate?, val appointments: List<Appointment>)

/** Der Tag, an dem das Termin beginnt: bei ganztägigen `dayFrom`, sonst der Beginn in Schweizer Zeit. */
fun Appointment.startDay(): LocalDate? = when (val time = time()) {
    is Timing.AllDay -> time.from
    is Timing.Timed -> time.start.toLocalDate()
    null -> null
}

/**
 * Gruppiert die Liste nach Beginn-Tag. Der Server sortiert nach Beginn, deshalb liegen gleiche Tage hintereinander;
 * ein Datum erscheint in der Zeitleiste nur einmal, auch bei mehreren Terminen am selben Tag.
 */
fun groupByDay(appointments: List<Appointment>): List<Day> {
    val days = mutableListOf<Day>()
    for (appointment in appointments) {
        val date = appointment.startDay()
        val lastGroup = days.lastOrNull()
        if (lastGroup != null && lastGroup.date == date) {
            days[days.lastIndex] = lastGroup.copy(appointments = lastGroup.appointments + appointment)
        } else {
            days += Day(date, listOf(appointment))
        }
    }
    return days
}

/** «SA» für Samstag: die ersten zwei Buchstaben des deutschen Kürzels in Grossbuchstaben. */
fun weekdayShort(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.GERMAN).take(2).uppercase(Locale.GERMAN)

/** «14. November» */
fun dayAndMonth(date: LocalDate): String =
    "${date.dayOfMonth}. ${date.month.getDisplayName(TextStyle.FULL, Locale.GERMAN)}"

/** «Samstag, 14. November»: Für den Screenreader ausgeschrieben statt «SA 14». */
fun spokenDay(date: LocalDate): String =
    "${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.GERMAN)}, ${dayAndMonth(date)}"

/** Uhrzeit in 24 Stunden; die Zeiten kommen schon in Schweizer Zeit aus [time]. Eine Fassung für Liste und Detail. */
private val CLOCK = DateTimeFormatter.ofPattern("HH:mm")
private val LIST_DAY = DateTimeFormatter.ofPattern("d.M.")

/** «14:00–17:00»: Beginn und Ende am selben Tag. */
private fun clockRange(start: ZonedDateTime, end: ZonedDateTime) = "${CLOCK.format(start)}–${CLOCK.format(end)}"

/** «14:00–17:00», bei mehreren Tagen der Beginn, bei ganztägigen «Ganztägig»; für die Karte der Liste und das Widget. */
fun listTimeText(appointment: Appointment, context: Context): String = when (val time = appointment.time()) {
    null -> context.getString(R.string.time_pending)
    is Timing.AllDay ->
        if (time.from == time.until) {
            context.getString(R.string.time_list_all_day)
        } else {
            context.getString(R.string.time_list_until, LIST_DAY.format(time.until))
        }
    is Timing.Timed ->
        if (time.start.toLocalDate() == time.end.toLocalDate()) clockRange(time.start, time.end) else CLOCK.format(time.start)
}

/**
 * Die Zeit einer Übung für die Detailansicht in zwei Zeilen: oben die Uhrzeit, darunter das Datum ausgeschrieben. `spoken` ist der
 * Satz für den Screenreader («14:00 bis 17:00 Uhr» statt Zahlen mit Strich).
 */
data class TimeDisplay(val primary: String, val secondary: String, val spoken: String)

/** «Samstag, 14. November»; mit Jahr («… 2027»), wenn es nicht das laufende ist. */
private fun dayText(date: LocalDate, withYear: Boolean) = spokenDay(date) + if (withYear) " ${date.year}" else ""

private const val ALL_DAY = "Ganztägig"

/**
 * Normal: «14:00–17:00» und «Samstag, 14. November». Endet die Übung an einem anderen Tag: «22:00 – 02:00» und «Samstag, 14. November
 * bis Sonntag, 15. November». Ganztägig: «Ganztägig» und das Datum, bei mehreren Tagen von bis (`until` ist inklusive). Das Jahr steht
 * nur, wenn es nicht das von `today` ist; geht die Übung über einen Jahreswechsel, steht es bei beiden Daten.
 */
fun appointmentTimeDisplay(timing: Timing, today: LocalDate): TimeDisplay = when (timing) {
    is Timing.Timed -> {
        val startDay = timing.start.toLocalDate()
        val endDay = timing.end.toLocalDate()
        val withYear = startDay.year != today.year || endDay.year != today.year
        if (startDay == endDay) {
            val day = dayText(startDay, withYear)
            TimeDisplay(clockRange(timing.start, timing.end), day, "$day, ${CLOCK.format(timing.start)} bis ${CLOCK.format(timing.end)} Uhr")
        } else {
            val start = dayText(startDay, withYear)
            val end = dayText(endDay, withYear)
            TimeDisplay(
                "${CLOCK.format(timing.start)} – ${CLOCK.format(timing.end)}",
                "$start bis $end",
                "$start, ${CLOCK.format(timing.start)} Uhr bis $end, ${CLOCK.format(timing.end)} Uhr",
            )
        }
    }
    is Timing.AllDay -> {
        val withYear = timing.from.year != today.year || timing.until.year != today.year
        val days = if (timing.from == timing.until) dayText(timing.from, withYear) else "${dayText(timing.from, withYear)} bis ${dayText(timing.until, withYear)}"
        TimeDisplay(ALL_DAY, days, "$ALL_DAY, $days")
    }
}

/** Die Zeit für die Detailansicht; `null`, wenn sie fehlt oder nicht lesbar ist (dann steht nichts da, der Titel bleibt reiner Text). */
fun Appointment.timeDisplay(today: LocalDate = LocalDate.now(SWISS_ZONE)): TimeDisplay? = time()?.let { appointmentTimeDisplay(it, today) }
