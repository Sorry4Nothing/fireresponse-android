package io.github.sorry4nothing.cevios.programs

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Programme eines Kalendertages (Schweizer Zeit). `date` ist `null`, wenn das Programm keine lesbare Zeit hat. */
data class Day(val date: LocalDate?, val programs: List<Program>)

/** Der Tag, an dem das Programm beginnt: bei ganztägigen `dayFrom`, sonst der Beginn in Schweizer Zeit. */
fun Program.startDay(): LocalDate? = when (val time = time()) {
    is Timing.AllDay -> time.from
    is Timing.Timed -> time.start.toLocalDate()
    null -> null
}

/**
 * Gruppiert die Liste nach Beginn-Tag. Der Server sortiert nach Beginn, deshalb liegen gleiche Tage hintereinander;
 * ein Datum erscheint in der Zeitleiste nur einmal, auch bei mehreren Programmen am selben Tag.
 */
fun groupByDay(programs: List<Program>): List<Day> {
    val days = mutableListOf<Day>()
    for (program in programs) {
        val date = program.startDay()
        val lastGroup = days.lastOrNull()
        if (lastGroup != null && lastGroup.date == date) {
            days[days.lastIndex] = lastGroup.copy(programs = lastGroup.programs + program)
        } else {
            days += Day(date, listOf(program))
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
