package ch.fireresponse.app.auth

import ch.fireresponse.app.R
import ch.fireresponse.app.net.ApiClient
import ch.fireresponse.app.net.SessionStorage
import ch.fireresponse.app.security.LockState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Der Ablauf von Anmeldung, Passwort und Einwilligung gegen einen Server, der nach Pfad antwortet. */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private lateinit var server: MockWebServer
    private lateinit var client: ApiClient
    private lateinit var viewModel: AuthViewModel
    private var cookie: String? = null

    @Volatile private var signedIn = false
    @Volatile private var mustChange = true
    @Volatile private var consented = false
    @Volatile private var loginAccepted = true

    private fun json(body: String, code: Int = 200, vararg cookies: String) =
        MockResponse.Builder().code(code).body(body).apply { cookies.forEach { addHeader("Set-Cookie", it) } }.build()

    private fun profile() =
        """{"id":"u1","name":"Sandra Muster","benutzername":"demo","kantonsadmin":false,"zweiterFaktor":false,
            "einwilligung":$consented,"mussPasswortAendern":$mustChange,"telefon":null,"bildUrl":null,"pushProgramme":true,
            "einheiten":[],"teilnehmende":[{"id":"k1","vorname":"Lena","nachname":"Muster","einheitId":"e1","art":"eltern"}]}"""

    @Before
    fun start() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.url.encodedPath
                return when {
                    path == "/api/auth/get-session" ->
                        json(if (signedIn) """{"session":{"id":"s"},"user":{"name":"Sandra Muster","mussPasswortAendern":$mustChange}}""" else "null")
                    path == "/api/auth/sign-in/username" ->
                        if (loginAccepted) {
                            signedIn = true
                            json("""{"redirect":false,"token":"t","user":{"name":"Sandra Muster"}}""", 200, "session=abc; Path=/")
                        } else {
                            json("""{"code":"ANMELDUNG_FEHLGESCHLAGEN","message":"x"}""", 401)
                        }
                    path == "/api/auth/change-password" -> { mustChange = false; json("{}") }
                    path == "/api/auth/sign-out" -> { signedIn = false; json("""{"success":true}""", 200, "session=; Max-Age=0; Path=/") }
                    !signedIn -> json("""{"code":"NICHT_ANGEMELDET","message":"x"}""", 401)
                    path == "/api/v1/ich" -> json(profile())
                    path == "/api/v1/einwilligung" && request.method == "GET" ->
                        json("""{"version":1,"text":"Wir speichern nur, was nötig ist.","gueltig":$consented}""")
                    path == "/api/v1/einwilligung" -> { consented = true; MockResponse.Builder().code(204).build() }
                    !consented -> json("""{"code":"EINWILLIGUNG_FEHLT","message":"x"}""", 403)
                    else -> json("{}")
                }
            }
        }
        server.start()
        val storage = object : SessionStorage {
            override fun read(): String? = cookie
            override fun write(value: String?) { cookie = value }
        }
        client = ApiClient(server.url("/").toString(), ApiClient.httpClient(storage, "1.0.0"), storage = storage)
        val lock = object : LockState {
            override val enabled = false
            override fun disable() = Unit
        }
        viewModel = AuthViewModel(AuthApi(client), client.unauthorized, client.forbidden, client::forgetSession, lock) { client.hasSession() }
    }

    @After
    fun stop() {
        Dispatchers.resetMain()
        server.close()
    }

    private fun <T> StateFlow<T>.await(condition: (T) -> Boolean): T = runBlocking { withTimeout(5_000) { first(condition) } }

    /** Meldet an, wählt das Passwort und stimmt zu: danach ist die Person in der App. */
    private fun signInCompletely() {
        viewModel.state.await { it is AuthState.SignedOut }
        viewModel.signIn("demo", "startpasswort123")
        viewModel.state.await { it is AuthState.MustChangePassword }
        viewModel.changePassword(null, "meinneuespasswort1")
        viewModel.state.await { it is AuthState.Welcome }
        viewModel.acceptConsent()
        viewModel.state.await { it is AuthState.SignedIn }
    }

    @Test
    fun withoutASessionTheStartIsTheLogin() {
        assertEquals(AuthState.SignedOut(expired = false), viewModel.state.await { it is AuthState.SignedOut })
    }

    @Test
    fun wrongCredentialsStayOnTheLoginWithTheUniformText() {
        loginAccepted = false
        viewModel.state.await { it is AuthState.SignedOut }
        viewModel.signIn("demo", "falsch")
        val form = viewModel.form.await { !it.running && it.error != null }
        assertEquals(R.string.login_failed, form.error)
        assertTrue(viewModel.state.value is AuthState.SignedOut)
        assertNull("kein Cookie bei einem Fehlschlag", cookie)
    }

    @Test
    fun aStartPasswordLeadsToTheChangeAndKeepsItOnlyInMemory() {
        viewModel.state.await { it is AuthState.SignedOut }
        viewModel.signIn("demo", "startpasswort123")
        viewModel.state.await { it is AuthState.MustChangePassword }
        assertTrue("das Startpasswort ist für den Wechsel bekannt", viewModel.startPasswordKnown.value)
    }

    @Test
    fun afterTheNewPasswordTheConsentTextFromTheServerIsShown() {
        viewModel.state.await { it is AuthState.SignedOut }
        viewModel.signIn("demo", "startpasswort123")
        viewModel.state.await { it is AuthState.MustChangePassword }
        viewModel.changePassword(null, "meinneuespasswort1")
        val welcome = viewModel.state.await { it is AuthState.Welcome } as AuthState.Welcome
        assertEquals("Wir speichern nur, was nötig ist.", welcome.consent.text)
        assertEquals("Lena", welcome.profile.participants.single().firstName)
        assertFalse("das Startpasswort ist weg, sobald es nicht mehr gebraucht wird", viewModel.startPasswordKnown.value)
    }

    @Test
    fun consentLeadsIntoTheApp() {
        signInCompletely()
        assertTrue(consented)
    }

    @Test
    fun aNewConsentVersionWhileTheAppIsOpenLeadsBackToTheConsent() {
        signInCompletely()
        consented = false
        // Ein Aufruf unter /api/v1 antwortet jetzt 403: Das Profil sagt, dass die Einwilligung fehlt.
        runBlocking { client.get<Unit>("/api/v1/programme") }
        val welcome = viewModel.state.await { it is AuthState.Welcome } as AuthState.Welcome
        assertEquals(1, welcome.consent.version)
    }

    @Test
    fun aLostSessionLeadsToTheLoginWithTheExpiredNotice() {
        signInCompletely()
        signedIn = false
        runBlocking { client.get<Unit>("/api/v1/programme") }
        assertEquals(AuthState.SignedOut(expired = true), viewModel.state.await { it is AuthState.SignedOut })
    }

    @Test
    fun signingOutEmptiesTheStateAndTheCookie() {
        signInCompletely()
        assertTrue(client.hasSession())
        viewModel.signOut()
        viewModel.state.await { it is AuthState.SignedOut }
        assertFalse(client.hasSession())
        assertNull(cookie)
    }
}
