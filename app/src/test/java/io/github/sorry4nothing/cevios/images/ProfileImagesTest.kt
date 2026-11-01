package io.github.sorry4nothing.cevios.images

import io.github.sorry4nothing.cevios.net.ApiClient
import io.github.sorry4nothing.cevios.net.ApiError
import io.github.sorry4nothing.cevios.net.ApiResult
import io.github.sorry4nothing.cevios.net.SessionStorage
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FixedStorage(private val cookie: String?) : SessionStorage {
    override fun read() = cookie
    override fun write(value: String?) = Unit
}

class ProfileImagesTest {
    private lateinit var server: MockWebServer
    private lateinit var images: ProfileImageApi
    private val id = "0190a1b2-3333-7e5f-8a9b-0c1d2e3f4a5b"

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        val client = ApiClient(server.url("/").toString(), ApiClient.httpClient(FixedStorage("session=abc"), "1.2.3"))
        images = ProfileImageApi(client)
    }

    @After
    fun stop() = server.close()

    @Test
    fun onlyProfileImageRoutesAreAccepted() {
        assertTrue(isProfileImagePath("/api/v1/personen/$id/bild"))
        assertTrue(isProfileImagePath("/api/v1/personen/$id/bild?v=1730300000"))
        assertFalse(isProfileImagePath("https://boese.example/api/v1/personen/$id/bild"))
        assertFalse(isProfileImagePath("//boese.example/api/v1/personen/$id/bild"))
        assertFalse(isProfileImagePath("/api/v1/personen/$id/bild/../../ich"))
        assertFalse(isProfileImagePath("/api/v1/ich"))
        assertFalse(isProfileImagePath("/api/v1/personen/$id/bild?v=1#x"))
    }

    @Test
    fun stageLogoRouteIsAcceptedAndNothingElseOnTheStagePath() {
        assertTrue(isProfileImagePath("/api/v1/stufen/$id/logo"))
        assertTrue(isProfileImagePath("/api/v1/stufen/$id/logo?v=3"))
        assertFalse(isProfileImagePath("https://boese.example/api/v1/stufen/$id/logo"))
        assertFalse(isProfileImagePath("/api/v1/stufen/$id/kontakte"))
        assertFalse(isProfileImagePath("/api/v1/stufen/$id/logo/../../ich"))
    }

    @Test
    fun aForeignAddressIsNeverRequestedAndGetsNoCookie() {
        val result = runBlocking { images.load("https://boese.example/api/v1/personen/$id/bild") }
        assertEquals(ApiResult.Failure(ApiError.NOT_FOUND), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun theImageComesAsBytesWithTheSession() {
        val bytes = byteArrayOf(1, 2, 3, 4)
        server.enqueue(MockResponse.Builder().code(200).addHeader("Content-Type", "image/jpeg").body(Buffer().write(bytes)).build())
        val result = runBlocking { images.load("/api/v1/personen/$id/bild?v=7") }
        assertArrayEquals(bytes, (result as ApiResult.Ok).value)
        val request = server.takeRequest()
        assertEquals("/api/v1/personen/$id/bild?v=7", request.url.encodedPath + "?" + request.url.encodedQuery)
        assertEquals("session=abc", request.headers["Cookie"])
    }

    @Test
    fun anImageThatIsTooBigIsNotLoaded() {
        server.enqueue(MockResponse.Builder().code(200).body(Buffer().write(ByteArray(600 * 1024))).build())
        assertEquals(ApiResult.Failure(ApiError.UNKNOWN), runBlocking { images.load("/api/v1/personen/$id/bild") })
    }

    @Test
    fun missingImageIsNotFound() {
        server.enqueue(MockResponse.Builder().code(404).body("""{"code":"NICHT_GEFUNDEN","message":"x"}""").build())
        assertEquals(ApiResult.Failure(ApiError.NOT_FOUND), runBlocking { images.load("/api/v1/personen/$id/bild") })
    }
}
