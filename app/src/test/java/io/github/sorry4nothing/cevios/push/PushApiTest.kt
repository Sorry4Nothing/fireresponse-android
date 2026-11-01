package io.github.sorry4nothing.cevios.push

import io.github.sorry4nothing.cevios.chat.Profile
import io.github.sorry4nothing.cevios.net.ApiClient
import io.github.sorry4nothing.cevios.net.ApiError
import io.github.sorry4nothing.cevios.net.ApiResult
import io.github.sorry4nothing.cevios.net.SessionStorage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PushApiTest {
    private lateinit var server: MockWebServer
    private lateinit var push: PushApi

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        val storage = object : SessionStorage {
            override fun read(): String? = null
            override fun write(value: String?) = Unit
        }
        push = PushApi(ApiClient(server.url("/").toString(), ApiClient.httpClient(storage, "1.0.0")))
    }

    @After
    fun stop() = server.close()

    private fun reply(code: Int, body: String = "") = MockResponse.Builder().code(code).body(body).build()

    @Test
    fun tokenGoesToTheServerWithPlatform() {
        server.enqueue(reply(204))
        val token = "cX1y2Z3a4B5c6D7e8F9g:APA91bxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
        assertEquals(ApiResult.Ok(Unit), runBlocking { push.registerToken(token) })
        val request = server.takeRequest()
        assertEquals("PUT /api/v1/push-token HTTP/1.1", request.requestLine)
        assertEquals("""{"token":"$token","plattform":"android"}""", request.body?.utf8())
    }

    @Test
    fun aRefusedTokenIsAFailure() {
        server.enqueue(reply(400, """{"code":"UNGUELTIGE_EINGABE","message":"x"}"""))
        assertTrue(runBlocking { push.registerToken("zu-kurz") } is ApiResult.Failure)
    }

    @Test
    fun oneSwitchPerRequestAndTheOtherStaysOut() {
        server.enqueue(reply(204))
        server.enqueue(reply(204))
        runBlocking {
            push.setSwitches(PushSwitchesRequest(chat = false))
            push.setSwitches(PushSwitchesRequest(programs = true))
        }
        val first = server.takeRequest()
        assertEquals("PATCH /api/v1/ich HTTP/1.1", first.requestLine)
        assertEquals("""{"pushChat":false}""", first.body?.utf8())
        assertEquals("""{"pushProgramme":true}""", server.takeRequest().body?.utf8())
    }

    @Test
    fun switchesFailWithTheErrorOfTheServer() {
        server.enqueue(reply(403, """{"code":"KEINE_BERECHTIGUNG","message":"x"}"""))
        val result = runBlocking { push.setSwitches(PushSwitchesRequest(chat = true)) }
        assertEquals(ApiError.FORBIDDEN, (result as ApiResult.Failure).error)
    }

    @Test
    fun theProfileCarriesTheSwitchesAndTheyAreOnByDefault() {
        val json = Json { ignoreUnknownKeys = true }
        val off = json.decodeFromString<Profile>("""{"id":"u1","pushChat":false,"pushProgramme":true}""")
        assertEquals(false, off.pushChat)
        assertEquals(true, off.pushPrograms)
        val plain = json.decodeFromString<Profile>("""{"id":"u1"}""")
        assertTrue(plain.pushChat && plain.pushPrograms)
    }

    @Test
    fun theRegistrationSwitchIsOnlyThereForPeopleWithARole() {
        val json = Json { ignoreUnknownKeys = true }
        assertEquals(false, json.decodeFromString<Profile>("""{"id":"u1","pushAnmeldungen":false}""").pushRegistrations)
        assertNull(json.decodeFromString<Profile>("""{"id":"u1"}""").pushRegistrations)
    }

    @Test
    fun theRegistrationSwitchGoesOutAsPushAnmeldungen() {
        server.enqueue(reply(204))
        runBlocking { push.setSwitches(PushSwitchesRequest(registrations = false)) }
        assertEquals("""{"pushAnmeldungen":false}""", server.takeRequest().body?.utf8())
    }

    @Test
    fun aMessageFromTheAdminLeadsNowhere() {
        assertNull(PushTarget.from("mitteilung", ""))
    }

    @Test
    fun aPushLeadsToChatOrProgram() {
        val id = "7c9e6679-7425-40de-944b-e07fc1f90ae7"
        assertEquals(PushTarget.Chat(id), PushTarget.from("chat", id))
        assertEquals(PushTarget.Program(id), PushTarget.from("programm", id))
    }

    @Test
    fun anUnknownTypeOrAnIdThatIsNoIdLeadsNowhere() {
        assertNull(PushTarget.from("person", "7c9e6679-7425-40de-944b-e07fc1f90ae7"))
        assertNull(PushTarget.from(null, "7c9e6679-7425-40de-944b-e07fc1f90ae7"))
        assertNull(PushTarget.from("chat", null))
        assertNull(PushTarget.from("chat", "../../ich"))
        assertNull(PushTarget.from("programm", "7c9e6679-7425-40de-944b-e07fc1f90ae7/teilnahmen"))
    }
}
