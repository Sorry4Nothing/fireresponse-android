package ch.fireresponse.app.auth

import ch.fireresponse.app.R
import ch.fireresponse.app.net.ApiClient
import ch.fireresponse.app.net.ApiError
import ch.fireresponse.app.net.ApiResult
import ch.fireresponse.app.net.SessionStorage
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class MemoryStorage(var value: String? = null) : SessionStorage {
    override fun read() = value
    override fun write(value: String?) {
        this.value = value
    }
}

class AuthApiTest {
    private lateinit var server: MockWebServer
    private val storage = MemoryStorage()
    private lateinit var auth: AuthApi
    private lateinit var client: ApiClient

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        client = ApiClient(server.url("/").toString(), ApiClient.httpClient(storage, "1.2.3"), storage = storage)
        auth = AuthApi(client)
    }

    @After
    fun stop() = server.close()

    private fun reply(code: Int = 200, body: String = "{}", vararg cookies: String) {
        server.enqueue(MockResponse.Builder().code(code).body(body).apply { cookies.forEach { addHeader("Set-Cookie", it) } }.build())
    }

    @Test
    fun noSessionIsNull() {
        reply(200, "null")
        assertEquals(ApiResult.Ok(null), runBlocking { auth.session() })
        assertEquals("/api/auth/get-session", server.takeRequest().url.encodedPath)
    }

    @Test
    fun sessionTellsWhetherThePasswordMustChange() {
        reply(200, """{"session":{"id":"s","token":"t"},"user":{"id":"u","name":"Marion Keller","username":"m.keller","mussPasswortAendern":true,"twoFactorEnabled":null}}""")
        assertEquals(ApiResult.Ok(AuthSession(AuthUser("Marion Keller", true))), runBlocking { auth.session() })
    }

    @Test
    fun signInSendsCredentialsAndKeepsTheSessionCookie() {
        val cookie = "__Secure-better-auth.session_token=abc"
        reply(200, """{"redirect":false,"token":"t","user":{"id":"u","name":"M"}}""", "$cookie; Path=/; Max-Age=7776000; HttpOnly")
        val result = runBlocking { auth.signIn("m.keller", "geheim") }
        assertEquals(ApiResult.Ok(SignInResponse(twoFactorRedirect = false)), result)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/auth/sign-in/username", request.url.encodedPath)
        assertEquals("""{"username":"m.keller","password":"geheim"}""", request.body?.utf8())
        assertEquals(cookie, storage.value)
    }

    @Test
    fun signInWithSecondFactorIsRecognised() {
        reply(200, """{"twoFactorRedirect":true,"twoFactorMethods":["totp"]}""", "two_factor=zwischen; Path=/; Max-Age=600")
        val result = runBlocking { auth.signIn("admin", "geheim") }
        assertEquals(ApiResult.Ok(SignInResponse(twoFactorRedirect = true)), result)
        // Das Zwischen-Cookie steht jetzt im Speicher: Der Code im nächsten Schritt schickt es mit.
        assertEquals("two_factor=zwischen", storage.value)
    }

    @Test
    fun theTotpCodeGoesWithTheIntermediateCookieAndBecomesTheSession() {
        storage.value = "two_factor=zwischen"
        reply(200, """{"token":"t","user":{"id":"u","name":"Petra"}}""", "session_token=abc; Path=/; Max-Age=7776000", "two_factor=; Path=/; Max-Age=0")
        assertEquals(ApiResult.Ok(Unit), runBlocking { auth.verifyTotp("012345") })
        val request = server.takeRequest()
        assertEquals("/api/auth/two-factor/verify-totp", request.url.encodedPath)
        assertEquals("two_factor=zwischen", request.headers["Cookie"])
        assertEquals("fireresponse://", request.headers["Origin"])
        assertEquals("""{"code":"012345"}""", request.body?.utf8())
        assertEquals("session_token=abc", storage.value)
    }

    @Test
    fun aBackupCodeHasItsOwnRoute() {
        storage.value = "two_factor=zwischen"
        reply(200, """{"token":"t","user":{"id":"u","name":"Petra"}}""", "session_token=abc; Path=/")
        assertEquals(ApiResult.Ok(Unit), runBlocking { auth.verifyBackupCode("abcde-fghij") })
        assertEquals("/api/auth/two-factor/verify-backup-code", server.takeRequest().url.encodedPath)
    }

    @Test
    fun aWrongCodeKeepsItsCodeAndDoesNotEndTheSession() = runBlocking {
        storage.value = "two_factor=zwischen"
        val ended = async(start = CoroutineStart.UNDISPATCHED) { withTimeoutOrNull(300) { client.unauthorized.first() } }
        reply(401, """{"code":"INVALID_CODE","message":"Invalid code"}""")
        val failure = auth.verifyTotp("000000") as ApiResult.Failure
        assertEquals("INVALID_CODE", failure.code)
        assertNull("ein falscher Code ist keine abgelaufene Sitzung", ended.await())
    }

    @Test
    fun everyFailedSignInIsTheSameText() {
        reply(401, """{"code":"ANMELDUNG_FEHLGESCHLAGEN","message":"x"}""")
        val failure = runBlocking { auth.signIn("m.keller", "falsch") } as ApiResult.Failure
        assertEquals(R.string.login_failed, signInErrorText(failure))
        reply(429)
        assertEquals(ApiError.TOO_MANY_REQUESTS.text, signInErrorText(runBlocking { auth.signIn("m", "p") } as ApiResult.Failure))
    }

    @Test
    fun changePasswordSendsBothPasswordsAndTakesTheNewCookie() {
        reply(200, """{"token":null,"user":{"id":"u","name":"M"}}""", "__Secure-better-auth.session_token=neu; Path=/; Max-Age=7776000")
        assertEquals(ApiResult.Ok(Unit), runBlocking { auth.changePassword("start", "ein langes neues Passwort") })
        val request = server.takeRequest()
        assertEquals("/api/auth/change-password", request.url.encodedPath)
        assertEquals("""{"currentPassword":"start","newPassword":"ein langes neues Passwort"}""", request.body?.utf8())
        assertEquals("__Secure-better-auth.session_token=neu", storage.value)
    }

    @Test
    fun changePasswordErrorsHaveTheirOwnTexts() {
        val expected = mapOf(
            "INVALID_PASSWORD" to R.string.password_err_current,
            "PASSWORT_UNVERAENDERT" to R.string.password_err_same,
            "PASSWORD_COMPROMISED" to R.string.password_err_leaked,
            "PASSWORD_TOO_SHORT" to R.string.password_err_short,
            "PASSWORD_TOO_LONG" to R.string.password_err_long,
        )
        expected.forEach { (code, text) ->
            reply(400, """{"code":"$code","message":"English text"}""")
            val failure = runBlocking { auth.changePassword("a", "b") } as ApiResult.Failure
            assertEquals(code, failure.code)
            assertEquals(code, text, PasswordRules.errorText(failure))
        }
    }

    @Test
    fun unknownPasswordErrorFallsBackToTheGeneralText() {
        reply(400, """{"code":"SONSTWAS","message":"x"}""")
        val failure = runBlocking { auth.changePassword("a", "b") } as ApiResult.Failure
        assertEquals(ApiError.UNKNOWN.text, PasswordRules.errorText(failure))
    }

    @Test
    fun signOutSendsTheCookieAndNoBody() {
        storage.value = "__Secure-better-auth.session_token=abc"
        reply(200, """{"success":true}""", "__Secure-better-auth.session_token=; Path=/; Max-Age=0")
        assertEquals(ApiResult.Ok(Unit), runBlocking { auth.signOut() })
        val request = server.takeRequest()
        assertEquals("/api/auth/sign-out", request.url.encodedPath)
        assertEquals("__Secure-better-auth.session_token=abc", request.headers["Cookie"])
        assertEquals("fireresponse://", request.headers["Origin"])
        assertNull(storage.value)
    }

    @Test
    fun theConsentTextComesFromTheServer() {
        reply(200, """{"version":2,"text":"Wir speichern nur, was nötig ist.","gueltig":false}""")
        val consent = (runBlocking { auth.consent() } as ApiResult.Ok).value
        assertEquals(Consent(2, "Wir speichern nur, was nötig ist.", valid = false), consent)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/einwilligung", request.url.encodedPath)
    }

    
    fun givingConsentIsAnEmptyPutBecauseTheServerSetsTheVersion() {
        reply(204, "")
        assertEquals(ApiResult.Ok(Unit), runBlocking { auth.giveConsent() })
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/einwilligung", request.url.encodedPath)
        assertEquals(0L, request.body?.size ?: 0L)
    }

    @Test
    fun profileCarriesWhatTheStartNeeds() {
        reply(
            200,
            """{"id":"u","name":"Marion Keller","benutzername":"m.keller","mussPasswortAendern":false,"einwilligung":false,
               "telefon":null,"bildUrl":"/api/v1/personen/u/bild?v=3","pushProgramme":true,"kantonsadmin":false,"zweiterFaktor":false,
               "einheiten":[],"teilnehmende":[{"id":"k","vorname":"Lena","nachname":"Keller","einheitId":"s","art":"eltern"}]}""",
        )
        val profile = (runBlocking { auth.profile() } as ApiResult.Ok).value
        assertEquals("Marion Keller", profile.name)
        assertEquals("m.keller", profile.username)
        assertFalse(profile.consented)
        assertEquals("Lena", profile.participants.single().firstName)
        assertEquals("/api/v1/personen/u/bild?v=3", profile.imageUrl)
    }

    @Test
    fun privacyPageLivesOnTheBackend() {
        assertTrue(auth.privacyUrl().endsWith("/datenschutz"))
        assertTrue(auth.privacyUrl().startsWith(server.url("/").toString()))
    }

    @Test
    fun tooManySignInsCarryTheWaitTime() {
        server.enqueue(MockResponse.Builder().code(429).addHeader("X-Retry-After", "60").body("{}").build())
        val failure = runBlocking { auth.signIn("m.keller", "x") } as ApiResult.Failure
        assertEquals(ApiError.TOO_MANY_REQUESTS, failure.error)
        assertEquals(60, failure.retryAfterSeconds)
    }

    @Test
    fun uploadSendsTheJpegAsBytes() {
        reply(204, "")
        assertEquals(ApiResult.Ok(Unit), runBlocking { auth.uploadImage(byteArrayOf(1, 2, 3)) })
        val request = server.takeRequest()
        assertEquals("PUT /api/v1/ich/bild HTTP/1.1", request.requestLine)
        assertEquals("image/jpeg", request.headers["Content-Type"])
        assertEquals(3L, request.body?.size?.toLong())
    }

    @Test
    fun removeIsADelete() {
        reply(204, "")
        assertEquals(ApiResult.Ok(Unit), runBlocking { auth.removeImage() })
        assertEquals("DELETE /api/v1/ich/bild HTTP/1.1", server.takeRequest().requestLine)
    }

    @Test
    fun tooManyUploadsAreRateLimited() {
        reply(429, "{}")
        assertEquals(ApiResult.Failure(ApiError.TOO_MANY_REQUESTS), runBlocking { auth.uploadImage(byteArrayOf(1)) })
    }
}
