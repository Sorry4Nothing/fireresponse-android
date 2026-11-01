package io.github.sorry4nothing.cevios.ui

import io.github.sorry4nothing.cevios.net.ApiError
import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorPageTest {
    @Test
    fun everyErrorGetsAPage() {
        ApiError.entries.forEach { ErrorPage.from(it) }
    }

    @Test
    fun mappingFollowsDesign() {
        assertEquals(ErrorPage.OFFLINE, ErrorPage.from(ApiError.NETWORK))
        assertEquals(ErrorPage.NOT_FOUND, ErrorPage.from(ApiError.NOT_FOUND))
        assertEquals(ErrorPage.NO_ACCESS, ErrorPage.from(ApiError.FORBIDDEN))
        assertEquals(ErrorPage.SESSION, ErrorPage.from(ApiError.NOT_SIGNED_IN))
        assertEquals(ErrorPage.OUTDATED, ErrorPage.from(ApiError.APP_OUTDATED))
        assertEquals(ErrorPage.ERROR, ErrorPage.from(ApiError.UNKNOWN))
        assertEquals(ErrorPage.ERROR, ErrorPage.from(ApiError.TOO_MANY_REQUESTS))
    }
}
