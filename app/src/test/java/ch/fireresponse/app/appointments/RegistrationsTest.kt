package ch.fireresponse.app.appointments

import ch.fireresponse.app.profile.Profile
import ch.fireresponse.app.profile.ProfileSquad
import ch.fireresponse.app.ui.ChipKind
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistrationsTest {
    private val json = Json { ignoreUnknownKeys = true }

    /** Die Übersicht im Format des Vertrags (`TeilnahmenUebersicht`). */
    private val body = """
        {"zahlen":{"teilnehmende":{"ja":11,"nein":3,"offen":6},"leitende":{"ja":4,"nein":1,"offen":1}},
         "teilnehmende":[{"id":"k1","vorname":"Lena","nachname":"Meier","status":"ja"},
                         {"id":"k2","vorname":"Elin","nachname":"Frei","status":"offen"}],
         "leitende":[{"id":"l1","name":"Sarah Meier","bildUrl":null,"rolle":"kommandant","status":"ja"},
                     {"id":"l2","name":"Tim Weber","bildUrl":null,"rolle":"stellvertretung","status":"offen"},
                     {"id":"l3","name":"Noah Keller","bildUrl":"/api/v1/personen/l3/bild","rolle":"leitung","status":"nein"}]}
    """.trimIndent()

    private fun appointment(squadId: String) = Appointment(
        id = "p1", title = "Waldweihnacht", status = "aktiv", allDay = false,
        squad = AppointmentSquad(squadId, "Einheit $squadId"),
    )

    private fun profile(admin: Boolean = false, vararg roles: Pair<String, String>) =
        Profile(id = "u1", admin = admin, squads = roles.map { ProfileSquad(it.first, "Einheit ${it.first}", role = it.second) })

    @Test
    fun theOverviewIsReadAsTheContractSaysIt() {
        val data = json.decodeFromString<Registrations>(body)
        assertEquals(Counts(11, 3, 6), data.counts.participants)
        assertEquals(20, data.counts.participants.total)
        assertEquals(Counts(4, 1, 1), data.counts.leaders)
        assertEquals("Lena Meier", data.participants.first().name)
        assertEquals("offen", data.participants.last().status)
        assertEquals(listOf(true, true, false), data.leaders.map { it.isCommand })
        assertEquals("/api/v1/personen/l3/bild", data.leaders.last().imageUrl)
    }

    @Test
    fun participantsAndLeadersCarryTheirBadge() {
        val data = json.decodeFromString<Registrations>(
            """{"zahlen":{"teilnehmende":{"ja":1,"nein":0,"offen":0},"leitende":{"ja":1,"nein":0,"offen":0}},
                "teilnehmende":[{"id":"k1","vorname":"Lena","nachname":"Meier","status":"ja",
                                  "abzeichen":{"kuerzel":"R3","sterne":3,"bildUrl":null}}],
                "leitende":[{"id":"l1","name":"Sarah Meier","bildUrl":null,"rolle":"kommandant","status":"ja",
                              "abzeichen":{"kuerzel":"Hptm","sterne":null,"bildUrl":null}}]}""",
        )
        assertEquals("R3", data.participants.single().badge?.abbreviation)
        assertEquals("Hptm", data.leaders.single().badge?.abbreviation)
    }

    @Test
    fun parentsGetNoLeadersAtAll() {
        val data = json.decodeFromString<Registrations>(
            """{"zahlen":{"teilnehmende":{"ja":1,"nein":0,"offen":0},"leitende":{"ja":0,"nein":0,"offen":0}},
                "teilnehmende":[{"id":"k1","vorname":"Lena","nachname":"Meier","status":"ja"}],"leitende":[]}""",
        )
        assertTrue(data.leaders.isEmpty())
    }

    @Test
    fun statusBecomesChip() {
        assertEquals(ChipKind.YES, registrationChip("ja"))
        assertEquals(ChipKind.NO, registrationChip("nein"))
        assertEquals(ChipKind.PENDING, registrationChip("offen"))
        assertEquals(ChipKind.PENDING, registrationChip("unbekannt"))
    }

    @Test
    fun theCantonAdminAndEveryRoleInTheUnitMaySee() {
        assertTrue(RegistrationsRules.maySee(appointment("e1"), profile(admin = true)))
        assertTrue(RegistrationsRules.maySee(appointment("e1"), profile(roles = arrayOf("e1" to "leitung"))))
        assertTrue(RegistrationsRules.maySee(appointment("e2"), profile(roles = arrayOf("e1" to "kommandant", "e2" to "stellvertretung"))))
    }

    @Test
    fun withoutRegistrationThereIsNothingToSee() {
        assertFalse(RegistrationsRules.maySee(appointment("e1").copy(registrable = false), profile(admin = true)))
    }

    @Test
    fun othersDoNotSeeTheEntry() {
        assertFalse(RegistrationsRules.maySee(appointment("e1"), profile(roles = arrayOf("e2" to "kommandant"))))
        assertFalse("Eltern ohne Rolle", RegistrationsRules.maySee(appointment("e1"), profile()))
        assertFalse(RegistrationsRules.maySee(appointment("e1"), null))
    }

    @Test
    fun sharesAddUpAndNobodyMeansZero() {
        assertEquals(0.55f, RegistrationsRules.share(11, 20), 0.0001f)
        assertEquals(0f, RegistrationsRules.share(0, 20), 0f)
        assertEquals(0f, RegistrationsRules.share(0, 0), 0f)
    }

    @Test
    fun leadersReadTheProfileImagePath() {
        val withImage = json.decodeFromString<LeaderRegistration>("""{"id":"1","name":"Anna Muster","rolle":"leitung","status":"ja","bildUrl":"/api/v1/personen/1/bild?v=2"}""")
        assertEquals("/api/v1/personen/1/bild?v=2", withImage.imageUrl)
        assertNull(json.decodeFromString<LeaderRegistration>("""{"id":"2","name":"Ben","rolle":"leitung","status":"nein","bildUrl":null}""").imageUrl)
    }
}
