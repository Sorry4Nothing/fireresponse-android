package io.github.sorry4nothing.cevios.programs

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DaysTest {
    private fun timed(id: String, start: String, end: String) =
        Program(id = id, title = id, status = "aktiv", allDay = false, start = start, end = end)

    private fun allDay(id: String, from: String, until: String) =
        Program(id = id, title = id, status = "aktiv", allDay = true, dayFrom = from, dayTo = until)

    @Test
    fun sameDayGivesOneGroupWithSeveralPrograms() {
        val days = groupByDay(
            listOf(
                timed("a", "2026-11-14T13:00:00.000Z", "2026-11-14T15:00:00.000Z"),
                timed("b", "2026-11-14T15:00:00.000Z", "2026-11-14T16:00:00.000Z"),
                timed("c", "2026-11-21T13:00:00.000Z", "2026-11-21T15:00:00.000Z"),
            ),
        )
        assertEquals(listOf(LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 21)), days.map { it.date })
        assertEquals(listOf("a", "b"), days[0].programs.map { it.id })
    }

    @Test
    fun lateEveningInUtcBelongsToNextDayInSwitzerland() {
        val days = groupByDay(
            listOf(
                timed("a", "2026-11-14T10:00:00Z", "2026-11-14T11:00:00Z"),
                timed("b", "2026-11-14T23:30:00Z", "2026-11-15T00:30:00Z"),
            ),
        )
        assertEquals(listOf(LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 15)), days.map { it.date })
    }

    @Test
    fun multiDayProgramSitsOnFirstDay() {
        val days = groupByDay(listOf(allDay("lager", "2027-07-10", "2027-07-17")))
        assertEquals(LocalDate.of(2027, 7, 10), days.single().date)
    }

    @Test
    fun programsWithoutReadableTimeStayTogetherWithoutDate() {
        val without = Program(id = "x", title = "x", status = "aktiv", allDay = false)
        val days = groupByDay(listOf(without, without.copy(id = "y")))
        assertEquals(1, days.size)
        assertEquals(null, days.single().date)
    }

    @Test
    fun emptyListGivesNoDays() {
        assertEquals(emptyList<Day>(), groupByDay(emptyList()))
    }

    @Test
    fun weekdayAndDateForTimeline() {
        assertEquals("SA", weekdayShort(LocalDate.of(2026, 11, 14)))
        assertEquals("MO", weekdayShort(LocalDate.of(2026, 11, 16)))
        assertEquals("14. November", dayAndMonth(LocalDate.of(2026, 11, 14)))
    }
}
