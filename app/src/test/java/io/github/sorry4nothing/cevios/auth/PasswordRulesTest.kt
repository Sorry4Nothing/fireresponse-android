package io.github.sorry4nothing.cevios.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordRulesTest {
    @Test
    fun needsTwelveCharacters() {
        assertFalse(PasswordRules.longEnough("elf Zeichen"))
        assertTrue(PasswordRules.longEnough("zwoelf Zeich"))
    }

    @Test
    fun mustDifferFromTheCurrentPassword() {
        assertFalse(PasswordRules.different("gleiches Passwort", "gleiches Passwort"))
        assertTrue(PasswordRules.different("Startpasswort", "ganz anderes Passwort"))
    }

    @Test
    fun withoutCurrentPasswordNothingIsValid() {
        assertFalse(PasswordRules.valid("", "ein langes neues Passwort"))
    }

    @Test
    fun knownStartPasswordNeedsOnlyALongNewPassword() {
        assertTrue(PasswordRules.canSave(null, "ein langes neues Passwort"))
        assertFalse(PasswordRules.canSave(null, "zu kurz"))
        assertFalse(PasswordRules.canSave("ein langes Passwort", "ein langes Passwort"))
        assertTrue(PasswordRules.canSave("Startpasswort", "ein langes neues Passwort"))
    }

    @Test
    fun validNeedsBothRules() {
        assertTrue(PasswordRules.valid("Startpasswort", "ein langes neues Passwort"))
        assertFalse(PasswordRules.valid("Startpasswort", "zu kurz"))
        assertFalse(PasswordRules.valid("ein langes Passwort", "ein langes Passwort"))
    }

    @Test
    fun firstNameIsTheFirstWord() {
        assertEquals("Marion", firstName("Marion Keller"))
        assertEquals("Marion", firstName("  Marion  "))
        assertEquals("", firstName(""))
    }

    @Test
    fun waitTimeIsShownInSecondsThenMinutes() {
        assertEquals(WaitTime(1, false), waitTime(0))
        assertEquals(WaitTime(30, false), waitTime(30))
        assertEquals(WaitTime(90, false), waitTime(90))
        assertEquals(WaitTime(2, true), waitTime(91))
        assertEquals(WaitTime(15, true), waitTime(900))
        assertEquals(WaitTime(16, true), waitTime(901))
    }
}
