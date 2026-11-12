package ch.fireresponse.app.appointments

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthsTest {
    private val squad = AppointmentSquad("e1", "JF")

    private fun appointment(id: String) = Appointment(id = id, title = id, status = "aktiv", allDay = false, squad = squad)

    private fun day(date: LocalDate?, vararg ids: String) = Day(date, ids.map(::appointment))

    @Test
    fun daysAreGroupedByMonthInTheOrderOfTheList() {
        val days = listOf(
            day(LocalDate.of(2026, 11, 14), "a"),
            day(LocalDate.of(2026, 11, 28), "b"),
            day(LocalDate.of(2026, 12, 5), "c"),
            day(LocalDate.of(2027, 1, 16), "d"),
        )
        val groups = groupByMonth(days)
        assertEquals(listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 12), YearMonth.of(2027, 1)), groups.map { it.month })
        assertEquals(listOf(2, 1, 1), groups.map { it.days.size })
    }

    @Test
    fun theSameMonthOfAnotherYearIsAnotherGroup() {
        val days = listOf(day(LocalDate.of(2026, 11, 14), "a"), day(LocalDate.of(2027, 11, 13), "b"))
        assertEquals(2, groupByMonth(days).size)
    }

    @Test
    fun appointmentsWithoutATimeGetNoMonth() {
        val groups = groupByMonth(listOf(day(LocalDate.of(2026, 11, 14), "a"), day(null, "b")))
        assertEquals(listOf<YearMonth?>(YearMonth.of(2026, 11), null), groups.map { it.month })
    }

    @Test
    fun noDaysGiveNoGroups() {
        assertEquals(emptyList<MonthGroup>(), groupByMonth(emptyList()))
    }

    @Test
    fun theYearShowsOnlyAtAYearChange() {
        assertEquals("November", monthLabel(YearMonth.of(2026, 11)))
        assertEquals("Dezember", monthLabel(YearMonth.of(2026, 12), previous = YearMonth.of(2026, 11)))
        assertEquals("Januar 2027", monthLabel(YearMonth.of(2027, 1), previous = YearMonth.of(2026, 12)))
        assertEquals("März", monthLabel(YearMonth.of(2027, 3), previous = YearMonth.of(2027, 2)))
        assertEquals("Januar 2027", monthLabel(YearMonth.of(2027, 1), previous = YearMonth.of(2026, 11)))
    }
}
