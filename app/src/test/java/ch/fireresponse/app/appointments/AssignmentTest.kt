package ch.fireresponse.app.appointments

import ch.fireresponse.app.profile.Profile
import ch.fireresponse.app.profile.ProfileRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssignmentTest {
    private val pfadi = AppointmentSquad("s1", "Pfadi")
    private val woelfe = AppointmentSquad("s2", "Wölfe")

    private fun appointment(squads: List<AppointmentSquad> = listOf(pfadi), visibility: String = "eltern") = Appointment(
        id = "p1", title = "Hock", status = "aktiv", visibility = visibility, allDay = false,
        squads = squads,
    )

    private val admin = Profile("a", admin = true)
    private val commander = Profile("l", roles = listOf(ProfileRole("s1", "stufenleitung")))
    private val leader = Profile("m", roles = listOf(ProfileRole("s1", "leitung")))
    private val parent = Profile("e")

    @Test
    fun onlyAdminsAndCommandersMayAssign() {
        assertTrue(AssignmentRules.mayAssign(admin))
        assertTrue(AssignmentRules.mayAssign(commander))
        assertFalse("Leitende nie", AssignmentRules.mayAssign(leader))
        assertFalse(AssignmentRules.mayAssign(parent))
        assertFalse("ohne Profil", AssignmentRules.mayAssign(null))
    }

    @Test
    fun adminsChooseAllCommandersOnlyTheirOwn() {
        val all = listOf(pfadi, woelfe)
        assertEquals(all, AssignmentRules.choices(all, admin))
        assertEquals(listOf(pfadi), AssignmentRules.choices(all, commander))
        assertTrue(AssignmentRules.choices(all, leader).isEmpty())
    }

    @Test
    fun foreignSquadsStayAndLockVisibility() {
        val appointment = appointment(squads = listOf(pfadi, woelfe))
        val choices = AssignmentRules.choices(listOf(pfadi, woelfe), commander)
        val fixed = AssignmentRules.fixed(appointment, choices)
        assertEquals(listOf(woelfe), fixed)
        assertFalse(AssignmentRules.mayChangeVisibility(fixed))
        assertTrue(AssignmentRules.mayChangeVisibility(AssignmentRules.fixed(appointment, listOf(pfadi, woelfe))))
        val request = AssignmentRules.request(setOf("s1"), fixed, Visibility.PARENTS)
        assertEquals(setOf("s1", "s2"), request.squadIds.toSet())
        assertEquals("eltern", request.visibility)
    }

    @Test
    fun withoutSquadOnlyInternIsValid() {
        assertTrue(AssignmentRules.valid(emptySet(), emptyList(), Visibility.INTERN))
        assertFalse(AssignmentRules.valid(emptySet(), emptyList(), Visibility.LEADERS))
        assertFalse(AssignmentRules.valid(emptySet(), emptyList(), Visibility.PARENTS))
        assertTrue(AssignmentRules.valid(setOf("s1"), emptyList(), Visibility.PARENTS))
        assertTrue("eine fremde Stufe bleibt", AssignmentRules.valid(emptySet(), listOf(woelfe), Visibility.PARENTS))
    }
}
