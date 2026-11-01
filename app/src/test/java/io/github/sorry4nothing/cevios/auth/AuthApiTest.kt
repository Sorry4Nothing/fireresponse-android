package io.github.sorry4nothing.cevios.auth

import io.github.sorry4nothing.cevios.R
import io.github.sorry4nothing.cevios.net.ApiClient
import io.github.sorry4nothing.cevios.net.ApiError
import io.github.sorry4nothing.cevios.net.ApiResult
import io.github.sorry4nothing.cevios.net.SessionStorage
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
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

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        auth = AuthApi(ApiClient(server.url("/").toString(), ApiClient.httpClient(storage, "1.2.3"), storage = storage))
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
        // Das Zwischen-Cookie steht jetzt im Speicher; die App wirft es weg (ApiClient.forgetSession).
        assertEquals("two_factor=zwischen", storage.value)
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
        assertEquals("cevi-os://", request.headers["Origin"])
        assertNull(storage.value)
    }

    @Test
    fun acceptingThePrivacyNoticeIsAnEmptyPost() {
        reply(204, "")
        assertEquals(ApiResult.Ok(Unit), runBlocking { auth.acceptPrivacy() })
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/ich/datenschutz", request.url.encodedPath)
    }

    @Test
    fun profileCarriesWhatTheStartNeeds() {
        reply(
            200,
            """{"id":"u","name":"Marion Keller","benutzername":"m.keller","mussPasswortAendern":false,"datenschutzGelesenAm":null,
               "telefon":null,"bildUrl":"/api/v1/personen/u/bild?v=3","pushChat":true,"pushProgramme":true,"admin":false,"zweiterFaktor":false,
               "rollen":[],"stufen":[],"kinder":[{"id":"k","vorname":"Lena","nachname":"Keller","stufeId":"s"}]}""",
        )
        val profile = (runBlocking { auth.profile() } as ApiResult.Ok).value
        assertEquals("Marion Keller", profile.name)
        assertEquals("m.keller", profile.username)
        assertNull(profile.privacyReadAt)
        assertEquals("Lena", profile.children.single().firstName)
        assertEquals("/api/v1/personen/u/bild?v=3", profile.imageUrl)
    }

    @Test
    fun privacyPageLivesOnTheBackend() {
        assertTrue(auth.privacyUrl().endsWith("/datenschutz"))
        assertTrue(auth.privacyUrl().startsWith(server.url("/").toString()))
    }

    @Test
    fun supportAndDeleteAccountPagesLiveOnTheBackend() {
        assertEquals(server.url("/support").toString(), auth.supportUrl())
        assertEquals(server.url("/konto-loeschen").toString(), auth.deleteAccountUrl())
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
