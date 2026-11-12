package ch.fireresponse.app.appointments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

private val SQUAD = AppointmentSquad("e1", "JF")

/** Die Zeit in zwei Zeilen, wie die Detailansicht sie zeigt (Schweizer Zeit, 24 Stunden). */
class TimeDisplayTest {
    private val today = LocalDate.of(2026, 11, 12)

    private fun timed(start: String?, end: String?, title: String = "Übung") = Appointment(
        id = "x", title = title, status = "aktiv", allDay = false, start = start, end = end, squad = SQUAD,
    )

    private fun allDay(from: String?, to: String?) = Appointment(
        id = "x", title = "Lager", status = "aktiv", allDay = true, dayFrom = from, dayTo = to, squad = SQUAD,
    )

    @Test
    fun anOrdinaryAppointmentShowsTheTimeAboveTheSpelledOutDay() {
        val display = timed("2026-11-14T18:00:00.000Z", "2026-11-14T20:00:00.000Z").timeDisplay(today)!!
        assertEquals("19:00–21:00", display.primary)
        assertEquals("Samstag, 14. November", display.secondary)
        assertEquals("Samstag, 14. November, 19:00 bis 21:00 Uhr", display.spoken)
    }

    @Test
    fun summerTimeIsTwoHoursAheadOfUtc() {
        val display = timed("2027-07-14T17:00:00.000Z", "2027-07-14T19:00:00.000Z").timeDisplay(LocalDate.of(2027, 7, 1))!!
        assertEquals("19:00–21:00", display.primary)
        assertEquals("Mittwoch, 14. Juli", display.secondary)
    }

    @Test
    fun theYearShowsOnlyWhenItIsNotTheCurrentOne() {
        val display = timed("2027-01-16T13:00:00.000Z", "2027-01-16T15:00:00.000Z").timeDisplay(today)!!
        assertEquals("Samstag, 16. Januar 2027", display.secondary)
    }

    @Test
    fun anAppointmentPastMidnightShowsBothDays() {
        val display = timed("2026-11-14T21:00:00.000Z", "2026-11-15T01:00:00.000Z").timeDisplay(today)!!
        assertEquals("22:00 – 02:00", display.primary)
        assertEquals("Samstag, 14. November bis Sonntag, 15. November", display.secondary)
        assertEquals("Samstag, 14. November, 22:00 Uhr bis Sonntag, 15. November, 02:00 Uhr", display.spoken)
    }

    @Test
    fun overTheNewYearBothDaysCarryTheYear() {
        val display = timed("2026-12-31T22:00:00.000Z", "2026-12-31T23:30:00.000Z").timeDisplay(LocalDate.of(2026, 12, 20))!!
        assertEquals("Donnerstag, 31. Dezember 2026 bis Freitag, 1. Januar 2027", display.secondary)
    }

    @Test
    fun anAllDayAppointmentOfOneDayShowsAllDayAndTheDay() {
        val display = allDay("2026-11-14", "2026-11-14").timeDisplay(today)!!
        assertEquals("Ganztägig", display.primary)
        assertEquals("Samstag, 14. November", display.secondary)
    }

    @Test
    fun anAllDayAppointmentOfSeveralDaysShowsFromTo() {
        val display = allDay("2026-11-14", "2026-11-16").timeDisplay(today)!!
        assertEquals("Ganztägig", display.primary)
        assertEquals("Samstag, 14. November bis Montag, 16. November", display.secondary)
        assertEquals("Ganztägig, Samstag, 14. November bis Montag, 16. November", display.spoken)
    }

    @Test
    fun withoutStartOrEndNothingShowsAndNothingCrashes() {
        assertNull(timed(null, "2026-11-14T20:00:00.000Z").timeDisplay(today))
        assertNull(timed("2026-11-14T18:00:00.000Z", null).timeDisplay(today))
        assertNull(timed(null, null).timeDisplay(today))
        assertNull(timed("morgen", "übermorgen").timeDisplay(today))
        assertNull(allDay(null, null).timeDisplay(today))
    }

    @Test
    fun theTitleIsPlainTextAndNeverReadAsATime() {
        val display = timed("2026-11-14T18:00:00.000Z", "2026-11-14T20:00:00.000Z", title = "Hauptübung 08:00–09:00").timeDisplay(today)!!
        assertEquals("19:00–21:00", display.primary)
    }
}
