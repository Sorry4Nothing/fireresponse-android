package ch.fireresponse.app.profile

import org.junit.Assert.assertEquals
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
    fun anAccountWithoutRolesSquadsOrParticipantsHasNoSquad() {
        assertEquals(true, Profile("u").hasNoSquad)
        assertEquals(false, Profile("u", roles = listOf(ProfileRole("s1", "leitung"))).hasNoSquad)
        assertEquals(false, Profile("u", participants = listOf(ProfileParticipant("k", "s1", "Lena"))).hasNoSquad)
    }

    @Test
    fun displayNamePrefersANonBlankNickname() {
        assertEquals("Blitz", displayName("Anna Muster", "Blitz"))
        assertEquals("Anna Muster", displayName("Anna Muster", null))
        assertEquals("Anna Muster", displayName("Anna Muster", "   "))
    }
}
