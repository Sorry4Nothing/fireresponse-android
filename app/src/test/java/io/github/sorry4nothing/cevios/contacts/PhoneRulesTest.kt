package io.github.sorry4nothing.cevios.contacts

import io.github.sorry4nothing.cevios.chat.Profile
import io.github.sorry4nothing.cevios.chat.ProfileRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneRulesTest {
    @Test
    fun acceptsDigitsAndAllowedSigns() {
        listOf("+41 79 000 00 01", "079 000 00 01", "(079) 000-00.01", "044/000 00 00", "  079 000 00 01  ", "123456")
            .forEach { assertTrue(it, PhoneRules.valid(it)) }
    }

    @Test
    fun rejectsTooShortTooLongLettersAndNoDigits() {
        listOf("12345", "1".repeat(31), "079 abc 00 01", "+++---", "079 000 00 01 ext. 5", "", "   ", "079\n000 00 01")
            .forEach { assertFalse(it, PhoneRules.valid(it)) }
    }

    @Test
    fun lengthIsCountedAfterTrimming() {
        assertTrue(PhoneRules.valid("      123456      "))
        assertTrue(PhoneRules.valid("1".repeat(30)))
    }

    @Test
    fun dialUriKeepsOnlyDigitsAndLeadingPlus() {
        assertEquals("tel:+41790000001", PhoneRules.dialUri("+41 79 000 00 01"))
        assertEquals("tel:0790000001", PhoneRules.dialUri("(079) 000-00.01"))
        assertEquals("tel:0440000000", PhoneRules.dialUri("044/000 00 00"))
    }

    @Test
    fun onlyGroupLeadersSetANumberButAnyoneWithOneMayRemoveIt() {
        val groupLeader = Profile("l", roles = listOf(ProfileRole("s1", "stufenleitung")))
        val leader = Profile("m", roles = listOf(ProfileRole("s1", "leitung")))
        val formerLeaderWithNumber = Profile("f", phone = "+41 79 000 00 01")
        val parent = Profile("e")
        assertTrue(PhoneRules.maySet(groupLeader) && PhoneRules.mayEdit(groupLeader))
        assertFalse(PhoneRules.maySet(leader) || PhoneRules.mayEdit(leader))
        assertFalse("ohne Rolle nur entfernen", PhoneRules.maySet(formerLeaderWithNumber))
        assertTrue(PhoneRules.mayEdit(formerLeaderWithNumber))
        assertFalse(PhoneRules.mayEdit(parent) || PhoneRules.mayEdit(null))
        assertFalse("leerer Text zählt nicht als Nummer", PhoneRules.mayEdit(Profile("x", phone = "  ")))
    }
}
