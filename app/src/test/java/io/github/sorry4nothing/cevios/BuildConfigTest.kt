package io.github.sorry4nothing.cevios

import org.junit.Assert.assertTrue
import org.junit.Test

class BuildConfigTest {
    @Test
    fun apiBaseUrlIsHttps() {
        assertTrue(BuildConfig.API_BASE_URL.startsWith("https://"))
    }
}
