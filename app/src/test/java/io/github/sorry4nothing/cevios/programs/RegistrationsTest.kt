package io.github.sorry4nothing.cevios.programs

import io.github.sorry4nothing.cevios.chat.Profile
import io.github.sorry4nothing.cevios.chat.ProfileRole
import io.github.sorry4nothing.cevios.ui.ChipKind
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class RegistrationsTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val body = """
        {"zahlen":{"kinder":{"ja":11,"nein":3,"offen":6},"leitende":{"ja":4,"nein":1,"offen":1}},
         "kinder":[{"id":"k1","vorname":"Lena","nachname":"Meier","stufeId":"s1","status":"ja"},
                   {"id":"k2","vorname":"Elin","nachname":"Frei","stufeId":"s1","status":"offen"}],
         "leitende":[{"id":"l1","name":"Sarah Meier","stufeId":"s1","rolle":"stufenleitung","status":"ja"},
                     {"id":"l2","name":"Noah Keller","stufeId":"s1","rolle":"leitung","status":"nein"}]}
    """.trimIndent()

    private fun program(vararg groups: String) = Program(
        id = "p1", title = "Waldweihnacht", status = "aktiv", allDay = false,
        ageGroups = groups.map { ProgramAgeGroup(it, "Stufe $it") },
    )

    private fun profile(admin: Boolean = false, vararg roles: Pair<String, String>) =
        Profile(id = "u1", admin = admin, roles = roles.map { ProfileRole(it.first, it.second) })

    @Test
    fun theOverviewIsReadAsTheContractSaysIt() {
        val data = json.decodeFromString<Registrations>(body)
        assertEquals(Counts(11, 3, 6), data.counts.children)
        assertEquals(20, data.counts.children.total)
        assertEquals(Counts(4, 1, 1), data.counts.leaders)
        assertEquals("Lena Meier", data.children.first().name)
        assertEquals("offen", data.children.last().status)
        assertTrue(data.leaders.first().isGroupLeader)
        assertFalse(data.leaders.last().isGroupLeader)
    }

    @Test
    fun statusBecomesChip() {
        assertEquals(ChipKind.YES, registrationChip("ja"))
        assertEquals(ChipKind.NO, registrationChip("nein"))
        assertEquals(ChipKind.PENDING, registrationChip("offen"))
        assertEquals(ChipKind.PENDING, registrationChip("unbekannt"))
    }

    @Test
    fun adminsAndLeadersOfTheProgramsAgeGroupsMaySee() {
        assertTrue(RegistrationsRules.maySee(program("s1"), profile(admin = true)))
        assertTrue(RegistrationsRules.maySee(program("s1"), profile(roles = arrayOf("s1" to "leitung"))))
        assertTrue(RegistrationsRules.maySee(program("s1", "s2"), profile(roles = arrayOf("s2" to "stufenleitung"))))
    }

    @Test
    fun othersDoNotSeeTheEntry() {
        assertFalse(RegistrationsRules.maySee(program("s1"), profile(roles = arrayOf("s2" to "stufenleitung"))))
        assertFalse(RegistrationsRules.maySee(program("s1"), profile()))
        assertFalse(RegistrationsRules.maySee(program("s1"), null))
    }

    @Test
    fun withoutAgeGroupThereIsNothingToCount() {
        assertFalse(RegistrationsRules.maySee(program(), profile(admin = true)))
    }

    @Test
    fun sharesAddUpAndNobodyMeansZero() {
        assertEquals(0.55f, RegistrationsRules.share(11, 20), 0.0001f)
        assertEquals(0f, RegistrationsRules.share(0, 20), 0f)
        assertEquals(0f, RegistrationsRules.share(0, 0), 0f)
    }

    @Test
    fun leadersReadTheProfileImagePath() {
        val json = Json { ignoreUnknownKeys = true }
        val withImage = json.decodeFromString<LeaderRegistration>("""{"id":"1","name":"Anna Muster","rolle":"leitung","status":"ja","bildUrl":"/api/v1/personen/1/bild?v=2"}""")
        assertEquals("/api/v1/personen/1/bild?v=2", withImage.imageUrl)
        assertNull(json.decodeFromString<LeaderRegistration>("""{"id":"2","name":"Ben","rolle":"leitung","status":"nein","bildUrl":null}""").imageUrl)
    }

    @Test
    fun leadersShowTheCeviNameAboveTheFullName() {
        val withCevi = LeaderRegistration("1", "Anna Muster", "leitung", "ja", ceviName = "Fuchs")
        assertEquals("Fuchs", withCevi.title)
        assertEquals("Anna Muster", withCevi.fullName)
        val without = LeaderRegistration("2", "Ben Beispiel", "stufenleitung", "nein")
        assertEquals("Ben Beispiel", without.title)
        assertNull(without.fullName)
        assertNull(LeaderRegistration("3", "Cara Test", "leitung", "offen", ceviName = " ").fullName)
    }
}
