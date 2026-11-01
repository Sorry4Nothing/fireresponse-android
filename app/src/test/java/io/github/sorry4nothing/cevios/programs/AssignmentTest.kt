package io.github.sorry4nothing.cevios.programs

import io.github.sorry4nothing.cevios.chat.Profile
import io.github.sorry4nothing.cevios.chat.ProfileRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssignmentTest {
    private val pfadi = ProgramAgeGroup("s1", "Pfadi")
    private val woelfe = ProgramAgeGroup("s2", "Wölfe")

    private fun program(ageGroups: List<ProgramAgeGroup> = listOf(pfadi), visibility: String = "eltern") = Program(
        id = "p1", title = "Hock", status = "aktiv", visibility = visibility, allDay = false,
        ageGroups = ageGroups,
    )

    private val admin = Profile("a", admin = true)
    private val groupLeader = Profile("l", roles = listOf(ProfileRole("s1", "stufenleitung")))
    private val leader = Profile("m", roles = listOf(ProfileRole("s1", "leitung")))
    private val parent = Profile("e")

    @Test
    fun onlyAdminsAndGroupLeadersMayAssign() {
        assertTrue(AssignmentRules.mayAssign(admin))
        assertTrue(AssignmentRules.mayAssign(groupLeader))
        assertFalse("Leitende nie", AssignmentRules.mayAssign(leader))
        assertFalse(AssignmentRules.mayAssign(parent))
        assertFalse("ohne Profil", AssignmentRules.mayAssign(null))
    }

    @Test
    fun adminsChooseAllGroupLeadersOnlyTheirOwn() {
        val all = listOf(pfadi, woelfe)
        assertEquals(all, AssignmentRules.choices(all, admin))
        assertEquals(listOf(pfadi), AssignmentRules.choices(all, groupLeader))
        assertTrue(AssignmentRules.choices(all, leader).isEmpty())
    }

    @Test
    fun foreignAgeGroupsStayAndLockVisibility() {
        val program = program(ageGroups = listOf(pfadi, woelfe))
        val choices = AssignmentRules.choices(listOf(pfadi, woelfe), groupLeader)
        val fixed = AssignmentRules.fixed(program, choices)
        assertEquals(listOf(woelfe), fixed)
        assertFalse(AssignmentRules.mayChangeVisibility(fixed))
        assertTrue(AssignmentRules.mayChangeVisibility(AssignmentRules.fixed(program, listOf(pfadi, woelfe))))
        val request = AssignmentRules.request(setOf("s1"), fixed, Visibility.PARENTS)
        assertEquals(setOf("s1", "s2"), request.ageGroupIds.toSet())
        assertEquals("eltern", request.visibility)
    }

    @Test
    fun withoutAgeGroupOnlyInternIsValid() {
        assertTrue(AssignmentRules.valid(emptySet(), emptyList(), Visibility.INTERN))
        assertFalse(AssignmentRules.valid(emptySet(), emptyList(), Visibility.LEADERS))
        assertFalse(AssignmentRules.valid(emptySet(), emptyList(), Visibility.PARENTS))
        assertTrue(AssignmentRules.valid(setOf("s1"), emptyList(), Visibility.PARENTS))
        assertTrue("eine fremde Stufe bleibt", AssignmentRules.valid(emptySet(), listOf(woelfe), Visibility.PARENTS))
    }
}
