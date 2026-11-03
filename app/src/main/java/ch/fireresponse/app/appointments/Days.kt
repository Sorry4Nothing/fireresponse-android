package ch.fireresponse.app.appointments

import android.content.Context
import ch.fireresponse.app.R
import java.time.LocalDate
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

private val LIST_TIME = DateTimeFormatter.ofPattern("HH:mm")
private val LIST_DAY = DateTimeFormatter.ofPattern("d.M.")

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
        if (time.start.toLocalDate() == time.end.toLocalDate()) "${LIST_TIME.format(time.start)}–${LIST_TIME.format(time.end)}" else LIST_TIME.format(time.start)
}
