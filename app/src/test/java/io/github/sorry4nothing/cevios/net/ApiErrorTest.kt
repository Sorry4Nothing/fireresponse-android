package io.github.sorry4nothing.cevios.net

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiErrorTest {
    @Test
    fun everyErrorHasItsOwnText() {
        assertEquals(ApiError.entries.size, ApiError.entries.map { it.text }.toSet().size)
    }

    @Test
    fun unknownStatusIsUnknownError() {
        assertEquals(ApiError.UNKNOWN, ApiError.fromStatus(418))
    }

    @Test
    fun conflictCodeDecidesTheText() {
        assertEquals(ApiError.PROGRAM_STARTED, ApiError.from(409, "PROGRAMM_BEGONNEN"))
        assertEquals(ApiError.REGISTRATION_CLOSED, ApiError.from(409, "PROGRAMM_GESCHLOSSEN"))
        assertEquals(ApiError.REGISTRATION_CLOSED, ApiError.from(409, null))
    }
}
