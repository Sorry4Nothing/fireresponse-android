package ch.fireresponse.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AvatarTest {
    @Test
    fun initialsAreFirstLettersOfFirstAndLastWord() {
        assertEquals("SL", avatarInitial("Sarah Läuchli"))
        assertEquals("AM", avatarInitial("Anna Maria Meier"))
        assertEquals("T", avatarInitial("  tom "))
    }

    @Test
    fun initialOfEmptyNameIsEmpty() {
        assertEquals("", avatarInitial("   "))
    }
}
