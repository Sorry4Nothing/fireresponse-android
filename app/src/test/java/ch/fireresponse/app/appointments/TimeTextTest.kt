package ch.fireresponse.app.appointments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private val SQUAD = AppointmentSquad("e1", "JF")

/** Die Zeit mit Tag, wie die Detailansicht sie zeigt (Schweizer Zeit, 24 Stunden). */
class TimeTextTest {
    private fun timed(start: String?, end: String?, title: String = "Übung") = Appointment(
        id = "x", title = title, status = "aktiv", allDay = false, start = start, end = end, squad = SQUAD,
    )

    private fun allDay(from: String?, to: String?) = Appointment(
        id = "x", title = "Lager", status = "aktiv", allDay = true, dayFrom = from, dayTo = to, squad = SQUAD,
    )

    @Test
    fun anOrdinaryAppointmentShowsDayAndTimeRange() {
        assertEquals("Sa., 14.11.2026, 19:00–21:00", timed("2026-11-14T18:00:00.000Z", "2026-11-14T20:00:00.000Z").timeText())
    }

    @Test
    fun summerTimeIsTwoHoursAheadOfUtc() {
        assertEquals("Di., 14.07.2026, 19:00–21:00", timed("2026-07-14T17:00:00.000Z", "2026-07-14T19:00:00.000Z").timeText())
    }

    @Test
    fun anAppointmentPastMidnightShowsBothDays() {
        assertEquals(
            "Sa., 14.11.2026, 22:00 – So., 15.11.2026, 02:00",
            timed("2026-11-14T21:00:00.000Z", "2026-11-15T01:00:00.000Z").timeText(),
        )
    }

    @Test
    fun anAllDayAppointmentOfOneDayShowsOnlyTheDate() {
        assertEquals("14.11.2026", allDay("2026-11-14", "2026-11-14").timeText())
    }

    @Test
    fun anAllDayAppointmentOfSeveralDaysShowsFromTo() {
        assertEquals("14.11.2026 – 16.11.2026", allDay("2026-11-14", "2026-11-16").timeText())
    }

    @Test
    fun withoutStartOrEndNothingShowsAndNothingCrashes() {
        assertNull(timed(null, "2026-11-14T20:00:00.000Z").timeText())
        assertNull(timed("2026-11-14T18:00:00.000Z", null).timeText())
        assertNull(timed(null, null).timeText())
        assertNull(timed("morgen", "übermorgen").timeText())
        assertNull(allDay(null, null).timeText())
    }

    @Test
    fun theTitleIsPlainTextAndNeverReadAsATime() {
        val text = timed("2026-11-14T18:00:00.000Z", "2026-11-14T20:00:00.000Z", title = "Hauptübung 08:00–09:00").timeText()
        assertEquals("Sa., 14.11.2026, 19:00–21:00", text)
    }
}
