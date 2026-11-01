package io.github.sorry4nothing.cevios.programs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TimingTest {
    private fun timed(start: String?, end: String?) = Program(
        id = "x", title = "T", status = "aktiv", allDay = false, start = start, end = end,
    )

    @Test
    fun winterTimeIsOneHourAheadOfUtc() {
        val time = timed("2026-11-14T13:00:00.000Z", "2026-11-14T16:00:00.000Z").time() as Timing.Timed
        assertEquals(LocalDateTime.of(2026, 11, 14, 14, 0), time.start.toLocalDateTime())
        assertEquals(LocalDateTime.of(2026, 11, 14, 17, 0), time.end.toLocalDateTime())
    }

    @Test
    fun summerTimeIsTwoHoursAheadOfUtc() {
        val time = timed("2026-07-14T12:00:00.000Z", "2026-07-14T15:00:00.000Z").time() as Timing.Timed
        assertEquals(LocalDateTime.of(2026, 7, 14, 14, 0), time.start.toLocalDateTime())
    }

    @Test
    fun lateEveningInUtcIsNextDayInSwitzerland() {
        val time = timed("2026-11-14T23:30:00Z", "2026-11-15T01:00:00Z").time() as Timing.Timed
        assertEquals(LocalDate.of(2026, 11, 15), time.start.toLocalDate())
    }

    @Test
    fun allDayTakesCalendarDaysUnchanged() {
        val p = Program(id = "x", title = "T", status = "aktiv", allDay = true, dayFrom = "2027-03-01", dayTo = "2027-03-03")
        assertEquals(Timing.AllDay(LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 3)), p.time())
    }

    @Test
    fun missingOrBrokenDataGiveNoTime() {
        assertNull(timed(null, null).time())
        assertNull(timed("morgen", "übermorgen").time())
        assertNull(Program(id = "x", title = "T", status = "aktiv", allDay = true).time())
    }
}
