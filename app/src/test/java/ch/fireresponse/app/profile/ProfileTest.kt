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
    fun displayNamePrefersANonBlankNickname() {
        assertEquals("Fuchs", displayName("Anna Muster", "Fuchs"))
        assertEquals("Anna Muster", displayName("Anna Muster", null))
        assertEquals("Anna Muster", displayName("Anna Muster", "   "))
    }
}
