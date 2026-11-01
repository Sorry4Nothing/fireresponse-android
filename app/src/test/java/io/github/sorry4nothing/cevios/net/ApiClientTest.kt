package io.github.sorry4nothing.cevios.net

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private class MemoryStorage(var value: String? = null) : SessionStorage {
    override fun read() = value
    override fun write(value: String?) {
        this.value = value
    }
}

@Serializable
private data class Me(val name: String)

class ApiClientTest {
    private lateinit var server: MockWebServer
    private val storage = MemoryStorage()

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun stop() = server.close()

    private fun client(s: SessionStorage = storage) =
        ApiClient(server.url("/").toString(), ApiClient.httpClient(s, "1.2.3"))

    private fun response(code: Int = 200, body: String = "{}", vararg cookies: String) =
        MockResponse.Builder().code(code).body(body).apply { cookies.forEach { addHeader("Set-Cookie", it) } }.build()

    @Test
    fun sendsVersionAndOriginAndNoCookieWithoutSession() {
        server.enqueue(response())
        runBlocking { client().get<Unit>("/api/v1/ich") }
        val request = server.takeRequest()
        assertEquals("1.2.3", request.headers["X-App-Version"])
        assertEquals("cevi-os://", request.headers["Origin"])
        assertNull(request.headers["Cookie"])
    }

    @Test
    fun remembersCookieFromResponseAndSendsItAfterwards() {
        val cookie = "__Secure-better-auth.session_token=abc%3Ddef"
        server.enqueue(response(200, "{}", "$cookie; Path=/; Max-Age=7776000; HttpOnly; Secure; SameSite=Lax"))
        server.enqueue(response())
        runBlocking {
            val api = client()
            api.get<Unit>("/api/auth/get-session")
            assertEquals(cookie, storage.value)
            api.get<Unit>("/api/v1/ich")
        }
        server.takeRequest()
        assertEquals(cookie, server.takeRequest().headers["Cookie"])
    }

    @Test
    fun newCookieReplacesOldAndMaxAgeZeroDeletesIt() {
        storage.value = "a=1; b=2"
        server.enqueue(response(200, "{}", "a=neu; Path=/", "b=; Max-Age=0; Path=/"))
        runBlocking { client().get<Unit>("/api/auth/get-session") }
        assertEquals("a=neu", storage.value)
    }

    @Test
    fun lastCookieDeleted_storageEmpty() {
        storage.value = "a=1"
        server.enqueue(response(200, "{}", "a=; Max-Age=0; Path=/"))
        runBlocking { client().get<Unit>("/api/auth/sign-out") }
        assertNull(storage.value)
    }

    @Test
    fun decodesJsonAndIgnoresUnknownFields() {
        server.enqueue(response(200, """{"name":"Test","extra":1}"""))
        val result = runBlocking { client().get<Me>("/api/v1/ich") }
        assertEquals(ApiResult.Ok(Me("Test")), result)
    }

    @Test
    fun postSendsJson() {
        server.enqueue(response(200, """{"name":"Test"}"""))
        val result = runBlocking { client().post<Me, Me>("/api/v1/x", Me("Test")) }
        assertEquals(ApiResult.Ok(Me("Test")), result)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("""{"name":"Test"}""", request.body?.utf8())
        assertEquals("application/json; charset=utf-8", request.headers["Content-Type"])
    }

    @Test
    fun statusBecomesUniformError() {
        val expected = mapOf(
            401 to ApiError.NOT_SIGNED_IN,
            403 to ApiError.FORBIDDEN,
            404 to ApiError.NOT_FOUND,
            409 to ApiError.REGISTRATION_CLOSED,
            426 to ApiError.APP_OUTDATED,
            429 to ApiError.TOO_MANY_REQUESTS,
            500 to ApiError.UNKNOWN,
        )
        expected.forEach { (status, error) ->
            server.enqueue(response(status, """{"code":"X","message":"Server-Text"}"""))
            assertEquals("Status $status", ApiResult.Failure(error, if (status == 409) "X" else null), runBlocking { client().get<Me>("/api/v1/ich") })
        }
    }

    @Test
    fun brokenJsonIsUnknownError() {
        server.enqueue(response(200, "kein json"))
        assertEquals(ApiResult.Failure(ApiError.UNKNOWN), runBlocking { client().get<Me>("/api/v1/ich") })
    }

    @Test
    fun failedConnectionIsNetworkError() {
        val dead = MockWebServer().apply { start() }
        val url = dead.url("/").toString()
        dead.close()
        val api = ApiClient(url, ApiClient.httpClient(storage, "1.2.3"))
        assertEquals(ApiResult.Failure(ApiError.NETWORK), runBlocking { api.get<Me>("/api/v1/ich") })
    }

    @Test
    fun redirectIsNotFollowed() {
        storage.value = "a=1"
        server.enqueue(MockResponse.Builder().code(302).addHeader("Location", server.url("/anderswo").toString()).build())
        assertEquals(ApiResult.Failure(ApiError.UNKNOWN), runBlocking { client().get<Me>("/api/v1/ich") })
        assertEquals(1, server.requestCount)
    }

    @Test
    fun badRequestCarriesTheServerErrorCode() {
        server.enqueue(response(400, """{"code":"PASSWORD_COMPROMISED","message":"English"}"""))
        assertEquals(ApiResult.Failure(ApiError.UNKNOWN, "PASSWORD_COMPROMISED"), runBlocking { client().get<Me>("/x") })
        server.enqueue(response(400, "kein json"))
        assertEquals(ApiResult.Failure(ApiError.UNKNOWN), runBlocking { client().get<Me>("/x") })
    }

    @Test
    fun lostSessionIsAnnouncedButAFailedSignInIsNot() = runBlocking {
        val api = client()
        val announced = async(start = CoroutineStart.UNDISPATCHED) { api.unauthorized.first() }
        server.enqueue(response(401, "{}"))
        api.post<Unit, Me>("/api/auth/sign-in/username", Unit)
        assertNull(withTimeoutOrNull(200) { announced.await() })
        server.enqueue(response(401, "{}"))
        api.get<Me>("/api/v1/chats")
        assertEquals(Unit, withTimeout(5_000) { announced.await() })
    }

    @Test
    fun forgetSessionClearsTheCookies() {
        storage.value = "two_factor=zwischen"
        ApiClient(server.url("/").toString(), ApiClient.httpClient(storage, "1.2.3"), storage = storage).forgetSession()
        assertNull(storage.value)
    }

    @Test
    fun pageUrlPointsAtTheBackend() {
        assertEquals(server.url("/datenschutz").toString(), client().pageUrl("/datenschutz"))
    }

    @Test
    fun retryAfterComesFromTheHeaderAndIsCapped() {
        fun failure(header: String?): ApiResult<Me> {
            val builder = MockResponse.Builder().code(429).body("{}")
            header?.let { builder.addHeader("X-Retry-After", it) }
            server.enqueue(builder.build())
            return runBlocking { client().get<Me>("/x") }
        }
        assertEquals(ApiResult.Failure(ApiError.TOO_MANY_REQUESTS, null, 45), failure("45"))
        assertEquals(ApiResult.Failure(ApiError.TOO_MANY_REQUESTS, null, 3600), failure("99999"))
        assertEquals(ApiResult.Failure(ApiError.TOO_MANY_REQUESTS), failure("bald"))
        assertEquals(ApiResult.Failure(ApiError.TOO_MANY_REQUESTS), failure(null))
    }

    @Test
    fun startedProgramIsToldApartFromClosedProgram() {
        server.enqueue(response(409, """{"code":"PROGRAMM_BEGONNEN","message":"x"}"""))
        assertEquals(ApiResult.Failure(ApiError.PROGRAM_STARTED, "PROGRAMM_BEGONNEN"), runBlocking { client().get<Me>("/x") })
        server.enqueue(response(409, """{"code":"PROGRAMM_GESCHLOSSEN","message":"x"}"""))
        assertEquals(ApiResult.Failure(ApiError.REGISTRATION_CLOSED, "PROGRAMM_GESCHLOSSEN"), runBlocking { client().get<Me>("/x") })
    }

    @Test
    fun forbiddenIsAnnouncedOnlyForTheApiRoutes() = runBlocking {
        val api = client()
        val announced = async(start = CoroutineStart.UNDISPATCHED) { api.forbidden.first() }
        server.enqueue(response(403, """{"code":"INVALID_ORIGIN","message":"x"}"""))
        api.post<Unit, Me>("/api/auth/sign-out", Unit)
        server.enqueue(response(401, "{}"))
        api.get<Me>("/api/v1/chats")
        assertNull(withTimeoutOrNull(200) { announced.await() })
        server.enqueue(response(403, """{"code":"KEINE_BERECHTIGUNG","message":"x"}"""))
        api.get<Me>("/api/v1/chats")
        assertEquals(Unit, withTimeout(5_000) { announced.await() })
    }

    @Test
    fun pendingCountsRequestsOfThePersonOnly() = runBlocking {
        val client = client()
        server.enqueue(MockResponse.Builder().body("{\"name\":\"a\"}").headersDelay(300, java.util.concurrent.TimeUnit.MILLISECONDS).build())
        server.enqueue(MockResponse.Builder().body("{\"name\":\"b\"}").headersDelay(300, java.util.concurrent.TimeUnit.MILLISECONDS).build())
        val foreground = async { client.get<Me>("/api/v1/ich") }
        val background = async(Background()) { client.get<Me>("/api/v1/ich") }
        withTimeout(2_000) { client.pending.first { it == 1 } }
        foreground.await()
        background.await()
        assertEquals(0, client.pending.value)
    }

    @Test
    fun pendingFallsBackAfterAFailure() = runBlocking {
        val client = client()
        server.enqueue(MockResponse.Builder().code(500).build())
        client.get<Me>("/api/v1/ich")
        assertEquals(0, client.pending.value)
    }
}
