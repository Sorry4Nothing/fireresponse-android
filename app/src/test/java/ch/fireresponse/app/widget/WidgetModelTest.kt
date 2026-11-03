package ch.fireresponse.app.widget

import ch.fireresponse.app.appointments.Appointment
import ch.fireresponse.app.appointments.AppointmentParticipant
import ch.fireresponse.app.appointments.AppointmentSquad
import ch.fireresponse.app.appointments.SWISS_ZONE
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetModelTest {
    private val now = ZonedDateTime.of(2026, 11, 10, 12, 0, 0, 0, SWISS_ZONE)
    private val squad = AppointmentSquad("s1", "Jugendfeuerwehr Musterdorf")

    private fun timed(id: String, start: String, end: String, status: String = "aktiv") =
        Appointment(id = id, title = "Übung $id", status = status, allDay = false, start = start, end = end, squad = squad)

    @Test
    fun theNextTwoComeInOrderOfTheirStart() {
        val list = listOf(
            timed("c", "2026-11-21T13:00:00.000Z", "2026-11-21T15:00:00.000Z"),
            timed("a", "2026-11-14T13:00:00.000Z", "2026-11-14T16:00:00.000Z"),
            timed("b", "2026-11-17T17:00:00.000Z", "2026-11-17T18:30:00.000Z"),
        )
        assertEquals(listOf("a", "b"), upcoming(list, now).map { it.id })
    }

    @Test
    fun cancelledAndPastAppointmentsAreLeftOut() {
        val list = listOf(
            timed("past", "2026-11-03T13:00:00.000Z", "2026-11-03T15:00:00.000Z"),
            timed("cancelled", "2026-11-12T13:00:00.000Z", "2026-11-12T15:00:00.000Z", status = "abgesagt"),
            timed("next", "2026-11-14T13:00:00.000Z", "2026-11-14T15:00:00.000Z"),
        )
        assertEquals(listOf("next"), upcoming(list, now).map { it.id })
    }

    @Test
    fun anAppointmentThatIsStillRunningCounts() {
        val running = timed("running", "2026-11-10T09:00:00.000Z", "2026-11-10T13:00:00.000Z")
        assertEquals(listOf("running"), upcoming(listOf(running), now).map { it.id })
    }

    @Test
    fun anAllDayAppointmentCountsUntilItsLastDay() {
        val camp = Appointment(id = "camp", title = "Lager", status = "aktiv", allDay = true, dayFrom = "2026-11-08", dayTo = "2026-11-10", squad = squad)
        val over = camp.copy(id = "over", dayFrom = "2026-11-07", dayTo = "2026-11-09")
        assertEquals(listOf("camp"), upcoming(listOf(over, camp), now).map { it.id })
    }

    @Test
    fun withoutAReadableTimeAnAppointmentDoesNotCount() {
        val unknown = Appointment(id = "x", title = "?", status = "aktiv", allDay = false, squad = squad)
        assertTrue(upcoming(listOf(unknown), now).isEmpty())
    }

    @Test
    fun answersCountOwnPeopleAndTheOwnLeaderRegistration() {
        val base = timed("a", "2026-11-14T13:00:00.000Z", "2026-11-14T15:00:00.000Z")
        val kids = listOf(AppointmentParticipant("k1", "Lena", "ja"), AppointmentParticipant("k2", "Noah", null))
        assertEquals(Answers(1, 2), base.copy(participants = kids).answers())
        assertEquals(Answers(2, 3), base.copy(participants = kids, ownRegistration = "nein").answers())
        assertEquals(Answers(0, 2), base.copy(participants = listOf(kids[1]), ownRegistration = "offen").answers())
    }

    @Test
    fun peopleAreTheChildrenThenTheOwnRegistrationAndSkippedWhenNoAnswerIsAsked() {
        val base = timed("a", "2026-11-14T13:00:00.000Z", "2026-11-14T15:00:00.000Z")
        val kids = listOf(AppointmentParticipant("k1", "Lena", "ja"), AppointmentParticipant("k2", "Noah", "nein"), AppointmentParticipant("k3", "Mia", null))
        assertEquals(
            listOf(Person("Lena", Stand.YES), Person("Noah", Stand.NO), Person("Mia", Stand.OPEN), Person("Du", Stand.OPEN)),
            base.copy(participants = kids, ownRegistration = "offen").people("Du"),
        )
        assertTrue(base.copy(registrable = false, participants = kids).people("Du").isEmpty())
    }

    @Test
    fun stateOfTheAnswers() {
        assertTrue(Answers(2, 2).done)
        assertFalse(Answers(1, 2).done)
        assertTrue(Answers(0, 2).open)
        assertFalse("ohne Antwort zu verlangen gibt es weder offen noch beantwortet", Answers(0, 0).open || Answers(0, 0).done)
    }

    @Test
    fun anAppointmentWithoutRegistrationAsksForNoAnswer() {
        val kids = listOf(AppointmentParticipant("k1", "Lena", null))
        assertEquals(Answers(0, 0), timed("a", "2026-11-14T13:00:00.000Z", "2026-11-14T15:00:00.000Z").copy(registrable = false, participants = kids).answers())
    }
}
