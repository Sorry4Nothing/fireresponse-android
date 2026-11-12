package ch.fireresponse.app.appointments

import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Die Tage eines Monats für die Zeitleiste. `month` ist `null` bei Terminen ohne lesbare Zeit: Für sie gibt es keine Überschrift. */
data class MonthGroup(val month: YearMonth?, val days: List<Day>)

/** Gruppiert die Tage der Liste nach Monat. Die Reihenfolge bleibt, wie der Server sie liefert; gleiche Monate liegen hintereinander. */
fun groupByMonth(days: List<Day>): List<MonthGroup> {
    val groups = mutableListOf<MonthGroup>()
    for (day in days) {
        val month = day.date?.let(YearMonth::from)
        val last = groups.lastOrNull()
        if (last != null && last.month == month) {
            groups[groups.lastIndex] = last.copy(days = last.days + day)
        } else {
            groups += MonthGroup(month, listOf(day))
        }
    }
    return groups
}

/** «November 2026»: mit Jahr, damit der Wechsel über den Jahreswechsel («Januar 2027») klar ist. */
fun monthLabel(month: YearMonth): String = "${month.month.getDisplayName(TextStyle.FULL, Locale.GERMAN)} ${month.year}"
