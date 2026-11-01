package io.github.sorry4nothing.cevios.security

import androidx.biometric.BiometricPrompt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockOutcomeTest {
    @Test
    fun cancelingIsNotAnError() {
        assertEquals(LockOutcome.Declined, outcomeOfError(BiometricPrompt.ERROR_NEGATIVE_BUTTON))
        assertEquals(LockOutcome.Declined, outcomeOfError(BiometricPrompt.ERROR_USER_CANCELED))
        assertEquals(LockOutcome.Declined, outcomeOfError(BiometricPrompt.ERROR_CANCELED))
    }

    @Test
    fun relocksOnlyAfterAWhileInTheBackground() {
        assertFalse(shouldRelock(null))
        assertFalse(shouldRelock(RELOCK_AFTER_MS - 1))
        assertTrue(shouldRelock(RELOCK_AFTER_MS))
    }

    @Test
    fun lockoutAndDeviceErrorsFail() {
        assertEquals(LockOutcome.Failed, outcomeOfError(BiometricPrompt.ERROR_LOCKOUT))
        assertEquals(LockOutcome.Failed, outcomeOfError(BiometricPrompt.ERROR_LOCKOUT_PERMANENT))
        assertEquals(LockOutcome.Failed, outcomeOfError(BiometricPrompt.ERROR_HW_UNAVAILABLE))
    }
}
