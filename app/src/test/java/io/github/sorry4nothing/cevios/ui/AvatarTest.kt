package io.github.sorry4nothing.cevios.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AvatarTest {
    @Test
    fun initialIsFirstLetterOfFirstWord() {
        assertEquals("S", avatarInitial("Sarah Läuchli"))
        assertEquals("A", avatarInitial("Anna Maria Meier"))
        assertEquals("T", avatarInitial("  tom "))
    }

    @Test
    fun initialOfEmptyNameIsEmpty() {
        assertEquals("", avatarInitial("   "))
    }
}
