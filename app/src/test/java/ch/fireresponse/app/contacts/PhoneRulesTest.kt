package ch.fireresponse.app.contacts

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
}
