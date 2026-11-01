package io.github.sorry4nothing.cevios.chat

import io.github.sorry4nothing.cevios.net.ApiClient
import io.github.sorry4nothing.cevios.net.ApiError
import io.github.sorry4nothing.cevios.net.ApiResult
import io.github.sorry4nothing.cevios.net.SessionStorage
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChatApiTest {
    private lateinit var server: MockWebServer
    private lateinit var chat: ChatApi

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        val storage = object : SessionStorage {
            override fun read(): String? = null
            override fun write(value: String?) = Unit
        }
        chat = ChatApi(ApiClient(server.url("/").toString(), ApiClient.httpClient(storage, "1.0.0")))
    }

    @After
    fun stop() = server.close()

    private fun json(code: Int, body: String) = MockResponse.Builder().code(code).body(body).build()

    @Test
    fun chatsReadsGroupAndDirectChat() {
        server.enqueue(
            json(
                200,
                """{"chats":[
                {"id":"c1","typ":"stufe","stufe":{"id":"s1","name":"Pfadi","kuerzel":"PF"},"eltern":null,
                 "stufenleitung":[{"id":"l1","name":"Beispiel Leitung"}]},
                {"id":"c2","typ":"direkt","stufe":{"id":"s1","name":"Pfadi","kuerzel":"PF"},
                 "eltern":{"id":"e1","name":"Beispiel Eltern"},"stufenleitung":[]}]}""",
            ),
        )
        val chats = (runBlocking { chat.chats() } as ApiResult.Ok).value.chats
        assertEquals("GET /api/v1/chats HTTP/1.1", server.takeRequest().requestLine)
        assertTrue(chats[0].isGroup && chats[0].parent == null)
        assertEquals("Beispiel Leitung", chats[0].groupLeaders.single().name)
        assertEquals("Beispiel Eltern", chats[1].parent?.name)
        assertTrue(!chats[1].isGroup)
    }

    @Test
    fun messagesWithAndWithoutCursor() {
        server.enqueue(json(200, """{"nachrichten":[],"naechster":null}"""))
        server.enqueue(json(200, """{"nachrichten":[],"naechster":null}"""))
        runBlocking {
            chat.messages("c1")
            chat.messages("c1", before = "0190a1b2")
        }
        assertEquals("GET /api/v1/chats/c1/nachrichten HTTP/1.1", server.takeRequest().requestLine)
        assertEquals("GET /api/v1/chats/c1/nachrichten?vor=0190a1b2 HTTP/1.1", server.takeRequest().requestLine)
    }

    @Test
    fun messageWithoutTextIsTolerated() {
        server.enqueue(
            json(
                200,
                """{"nachrichten":[
                {"id":"m2","text":null,"erstelltAm":"2027-01-01T10:00:00.000Z","autor":{"id":"a","name":"Ich"},
                 "programm":null,"eigene":true},
                {"id":"m1","text":"Hallo","erstelltAm":"2027-01-01T09:00:00.000Z","autor":{"id":"b","name":"Du"},
                 "programm":{"id":"p1","titel":"Lager"},"eigene":false}],"naechster":"m1"}""",
            ),
        )
        val page = (runBlocking { chat.messages("c1") } as ApiResult.Ok).value
        assertNull(page.messages[0].text)
        assertTrue(page.messages[0].own)
        assertEquals("Lager", page.messages[1].program?.title)
        assertEquals("m1", page.next)
    }

    @Test
    fun sendPostsTextAndReadsId() {
        server.enqueue(json(201, """{"id":"m9"}"""))
        val result = runBlocking { chat.send("c1", "Hallo") }
        assertEquals(ApiResult.Ok(MessageIdResponse("m9")), result)
        val request = server.takeRequest()
        assertEquals("POST /api/v1/chats/c1/nachrichten HTTP/1.1", request.requestLine)
        assertEquals("""{"text":"Hallo"}""", request.body?.utf8())
    }

    @Test
    fun parentDirectChatSendsOnlyAgeGroup() {
        server.enqueue(json(200, """{"id":"c7"}"""))
        assertEquals(ApiResult.Ok(ChatIdResponse("c7")), runBlocking { chat.directChat("s1") })
        val request = server.takeRequest()
        assertEquals("POST /api/v1/chats/direkt HTTP/1.1", request.requestLine)
        assertEquals("""{"stufeId":"s1"}""", request.body?.utf8())
    }

    @Test
    fun groupLeaderDirectChatNamesParent() {
        server.enqueue(json(200, """{"id":"c7"}"""))
        runBlocking { chat.directChat("s1", "e1") }
        assertEquals("""{"stufeId":"s1","elternUserId":"e1"}""", server.takeRequest().body?.utf8())
    }

    @Test
    fun profileReadsRolesAgeGroupsAndChildren() {
        server.enqueue(
            json(
                200,
                """{"id":"u1","name":"Beispiel","benutzername":"b","mussPasswortAendern":false,"admin":false,
                "rollen":[{"stufeId":"s1","typ":"stufenleitung"}],
                "stufen":[{"id":"s1","name":"Pfadi","kuerzel":"PF"}],
                "kinder":[{"id":"k1","vorname":"Mia","nachname":"Muster","stufeId":"s1"}]}""",
            ),
        )
        val profile = (runBlocking { chat.profile() } as ApiResult.Ok).value
        assertEquals("GET /api/v1/ich HTTP/1.1", server.takeRequest().requestLine)
        assertTrue(profile.isGroupLeader("s1"))
        assertEquals(ProfileChild("k1", "s1", "Mia"), profile.children.single())
    }

    @Test
    fun tooManyMessagesGivesTooManyRequestsError() {
        server.enqueue(json(429, """{"code":"ZU_VIELE_ANFRAGEN","message":"x"}"""))
        assertEquals(ApiResult.Failure(ApiError.MESSAGE_TOO_FAST), runBlocking { chat.send("c1", "Hallo") })
    }

    @Test
    fun nonMemberIsNotFound() {
        server.enqueue(json(404, """{"code":"NICHT_GEFUNDEN","message":"x"}"""))
        assertEquals(ApiResult.Failure(ApiError.NOT_FOUND), runBlocking { chat.messages("fremd") })
    }

    @Test
    fun unreadCountIsReadAndDefaultsToZero() {
        server.enqueue(
            json(
                200,
                """{"chats":[
                {"id":"c1","typ":"stufe","stufe":{"id":"s1","name":"Pfadi","kuerzel":"PF"},"eltern":null,"stufenleitung":[],"ungelesen":3},
                {"id":"c2","typ":"stufe","stufe":{"id":"s2","name":"Wölfe","kuerzel":"WO"},"eltern":null,"stufenleitung":[]}]}""",
            ),
        )
        val chats = (runBlocking { chat.chats() } as ApiResult.Ok).value.chats
        assertEquals(listOf(3, 0), chats.map { it.unread })
    }

    @Test
    fun markReadIsPutWithoutBody() {
        server.enqueue(MockResponse.Builder().code(204).build())
        assertEquals(ApiResult.Ok(Unit), runBlocking { chat.markRead("c1") })
        val request = server.takeRequest()
        assertEquals("PUT /api/v1/chats/c1/gelesen HTTP/1.1", request.requestLine)
        assertEquals(0, request.body?.size ?: 0)
    }

    @Test
    fun badgeTextHidesZeroAndCapsAtNinetyNine() {
        assertNull(unreadBadgeText(0))
        assertNull(unreadBadgeText(-1))
        assertEquals("1", unreadBadgeText(1))
        assertEquals("99", unreadBadgeText(99))
        assertEquals("99+", unreadBadgeText(100))
        assertEquals("99+", unreadBadgeText(1234))
    }

    @Test
    fun phoneIsPatchedAndRemovedWithEmptyText() {
        server.enqueue(MockResponse.Builder().code(204).build())
        server.enqueue(MockResponse.Builder().code(204).build())
        assertEquals(ApiResult.Ok(Unit), runBlocking { chat.setPhone("+41 79 000 00 01") })
        val set = server.takeRequest()
        assertEquals("PATCH /api/v1/ich HTTP/1.1", set.requestLine)
        assertEquals("""{"telefon":"+41 79 000 00 01"}""", set.body?.utf8())
        assertEquals(ApiResult.Ok(Unit), runBlocking { chat.setPhone("") })
        assertEquals("""{"telefon":""}""", server.takeRequest().body?.utf8())
    }

    @Test
    fun parentSettingANumberIsForbidden() {
        server.enqueue(json(403, """{"code":"KEINE_BERECHTIGUNG","message":"nein"}"""))
        assertEquals(ApiResult.Failure(ApiError.FORBIDDEN), runBlocking { chat.setPhone("+41 79 000 00 01") })
    }

    @Test
    fun profileCarriesTheOwnNumber() {
        server.enqueue(json(200, """{"id":"u1","telefon":"+41 79 000 00 01","rollen":[],"stufen":[],"kinder":[]}"""))
        assertEquals("+41 79 000 00 01", (runBlocking { chat.profile() } as ApiResult.Ok).value.phone)
    }

    @Test
    fun chatListReadsLastMessageAndAllowsNone() {
        server.enqueue(
            json(
                200,
                """{"chats":[
                {"id":"c1","typ":"stufe","stufe":{"id":"s1","name":"Pfadi","kuerzel":"PF"},"eltern":null,"stufenleitung":[],"ungelesen":0,
                 "letzteNachricht":{"text":"Treffpunkt ist um 14 Uhr.","erstelltAm":"2027-01-01T12:30:00Z","eigene":false,
                                    "autor":{"id":"l1","name":"Beispiel Leitung"}}},
                {"id":"c2","typ":"stufe","stufe":{"id":"s2","name":"Wölfe","kuerzel":"WO"},"eltern":null,"stufenleitung":[],"ungelesen":0,
                 "letzteNachricht":null}]}""",
            ),
        )
        val chats = (runBlocking { chat.chats() } as ApiResult.Ok).value.chats
        val last = chats[0].lastMessage!!
        assertEquals("Treffpunkt ist um 14 Uhr.", last.text)
        assertEquals("Beispiel Leitung", last.author.name)
        assertFalse(last.own)
        assertNull(chats[1].lastMessage)
    }

    @Test
    fun messagesReadTheLeaderRoleOfTheAuthor() {
        server.enqueue(
            json(
                200,
                """{"nachrichten":[
                {"id":"m2","text":"Hallo","erstelltAm":"2027-01-01T10:00:00Z","autor":{"id":"l1","name":"Beispiel Leitung","stufenleitung":true},
                 "programm":null,"eigene":false},
                {"id":"m1","text":"Hi","erstelltAm":"2027-01-01T09:00:00Z","autor":{"id":"e1","name":"Beispiel Eltern","stufenleitung":false},
                 "programm":null,"eigene":false}],"naechster":null}""",
            ),
        )
        val messages = (runBlocking { chat.messages("c1") } as ApiResult.Ok).value.messages
        assertTrue(messages[0].author.groupLeader)
        assertFalse(messages[1].author.groupLeader)
    }

    @Test
    fun directChatNotFoundHasItsOwnText() {
        server.enqueue(json(404, """{"code":"NICHT_GEFUNDEN","message":"x"}"""))
        assertEquals(ApiResult.Failure(ApiError.DIRECT_CHAT_UNAVAILABLE), runBlocking { chat.directChat("s1") })
    }

    @Test
    fun otherChatNotFoundKeepsTheGeneralText() {
        server.enqueue(json(404, """{"code":"NICHT_GEFUNDEN","message":"x"}"""))
        assertEquals(ApiResult.Failure(ApiError.NOT_FOUND), runBlocking { chat.messages("c1") })
    }

    @Test
    fun authorImageUrlIsRead() {
        server.enqueue(json(200, """{"nachrichten":[{"id":"m1","text":"Hallo","erstelltAm":"2026-05-02T12:30:00Z","autor":{"id":"u1","name":"Anna","bildUrl":"/api/v1/personen/u1/bild?v=2","stufenleitung":true},"programm":null,"eigene":false}],"naechster":null}"""))
        val page = (runBlocking { chat.messages("c1") } as ApiResult.Ok).value
        assertEquals("/api/v1/personen/u1/bild?v=2", page.messages.single().author.imageUrl)
    }
}
