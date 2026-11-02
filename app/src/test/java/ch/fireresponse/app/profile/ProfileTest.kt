package ch.fireresponse.app.profile

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileTest {
    @Test
    fun initialsUseFirstAndLastWord() {
        assertEquals("SL", initials("Sarah Läuchli"))
        assertEquals("AM", initials("Anna Maria Meier"))
        assertEquals("T", initials("  tom "))
        assertEquals("", initials("   "))
    }

    @Test
    fun anAccountWithoutUnitsOrParticipantsHasNoSquad() {
        assertTrue(Profile("u").hasNoSquad)
        assertFalse(Profile("u", squads = listOf(ProfileSquad("e1", "JF", role = "leitung"))).hasNoSquad)
        assertFalse(Profile("u", participants = listOf(ProfileParticipant("k", "Lena", "Muster", "e1"))).hasNoSquad)
    }

    @Test
    fun commandMeansCommanderOrDeputyOfThatUnit() {
        val profile = Profile(
            "u",
            squads = listOf(
                ProfileSquad("e1", "JF", role = "kommandant"),
                ProfileSquad("e2", "KF", role = "stellvertretung"),
                ProfileSquad("e3", "JF Nord", role = "leitung"),
            ),
        )
        assertTrue(profile.isCommand("e1") && profile.isCommand("e2"))
        assertFalse("Leitung ist nicht Kommando", profile.isCommand("e3"))
        assertFalse(profile.isCommand("e4"))
        assertTrue(profile.isCommand && profile.isLeader)
    }

    @Test
    fun onlyRanksTheBadgeCanShowAreShown() {
        assertEquals(listOf(0, 1, 2, null, null, null), (0..5).map { Badge(stars = it).shownStars })
        assertNull("Bild-Abzeichen", Badge(stars = null, imageUrl = "/api/v1/raenge/r1/abzeichen").shownStars)
    }

    @Test
    fun aRankWithoutStarsAndImageIsOnlyTheAbbreviation() {
        val badge = Json { ignoreUnknownKeys = true }.decodeFromString<Badge>("""{"kuerzel":"Oblt","sterne":null,"bildUrl":null}""")
        assertEquals("Oblt", badge.abbreviation)
        assertNull("kein Bild für die App", badge.shownStars)
    }

    @Test
    fun participantsAndUnitsCarryTheirBadge() {
        val profile = Json { ignoreUnknownKeys = true }.decodeFromString<Profile>(
            """{"id":"u1","name":"Petra","einwilligung":true,
                "einheiten":[{"id":"e1","name":"JF","typ":"jf","bezirk":"B","rolle":"kommandant","logoUrl":null,
                               "abzeichen":{"kuerzel":"Oblt","sterne":null,"bildUrl":null}}],
                "teilnehmende":[{"id":"k1","vorname":"Lena","nachname":"M","einheitId":"e1","art":"eltern",
                                  "abzeichen":{"kuerzel":"R1","sterne":1,"bildUrl":null}},
                                 {"id":"k2","vorname":"Noah","nachname":"M","einheitId":"e1","art":"eltern","abzeichen":null}]}""",
        )
        assertEquals("Oblt", profile.squads.single().badge?.abbreviation)
        assertEquals(Badge("R1", stars = 1), profile.participants.first().badge)
        assertNull("kein Rang", profile.participants.last().badge)
    }

    @Test
    fun theProfileIsReadAsTheContractSaysIt() {
        val profile = Json { ignoreUnknownKeys = true }.decodeFromString<Profile>(
            """{"id":"u1","name":"Sandra Muster","benutzername":"smuster","kantonsadmin":true,"zweiterFaktor":true,
                "einwilligung":false,"mussPasswortAendern":true,"telefon":"079 000 00 00","bildUrl":"/api/v1/personen/u1/bild?v=3",
                "pushProgramme":false,"einheiten":[],"teilnehmende":[]}""",
        )
        assertTrue(profile.admin && profile.mustChangePassword)
        assertFalse("die Einwilligung fehlt", profile.consented)
        assertFalse(profile.pushAppointments)
        assertNull("ohne Rolle kein Schalter", profile.pushRegistrations)
        assertEquals("079 000 00 00", profile.phone)
    }
}
