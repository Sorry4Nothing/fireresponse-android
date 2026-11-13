package ch.fireresponse.app.infochannel

import ch.fireresponse.app.net.ApiClient
import ch.fireresponse.app.net.ApiResult
import ch.fireresponse.app.net.SessionStorage
import ch.fireresponse.app.push.PushTarget
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
import java.time.LocalDate
import java.util.concurrent.CopyOnWriteArrayList

private fun message(id: String, at: String, text: String? = "Hallo", own: Boolean = false, deleted: Boolean = false) = InfoMessage(
    id = id, text = text, createdAt = at, own = own, deleted = deleted,
    author = if (deleted) null else MessageAuthor("Sarah Meier", "leitung"), canEdit = own, canDelete = own,
)

/** Das Modell und die reinen Funktionen des Infokanals. */
class InfoChannelModelTest {
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    @Test
    fun readsTheChannelListAndTheHistoryPageOfTheContract() {
        val list = json.decodeFromString<InfoChannels>(
            """{"infokanaele":[{"einheit":{"id":"e1","name":"Jugendfeuerwehr Meilen","typ":"jf"},"name":"Infokanal Jugendfeuerwehr Meilen",
                "darfSchreiben":true,"ungelesen":3,"letzteMitteilungAm":"2026-11-13T14:30:00+01:00"},
               {"einheit":{"id":"e2","name":"Kinderfeuerwehr Meilen","typ":"kf"},"name":"Infokanal Kinderfeuerwehr Meilen",
                "darfSchreiben":false,"ungelesen":0,"letzteMitteilungAm":null}]}""",
        )
        assertEquals(3, unreadTotal(list.channels))
        assertTrue(list.channels.first().canWrite)
        assertNull(list.channels.last().lastAt)
        val page = json.decodeFromString<MessagePage>(
            """{"darfSchreiben":true,"mitteilungen":[
                {"id":"m2","text":null,"geloescht":true,"bearbeitet":false,"erstelltAm":"2026-11-13T13:00:00+01:00","autor":null,
                 "eigene":false,"darfBearbeiten":false,"darfLoeschen":false},
                {"id":"m1","text":"Heute Training","geloescht":false,"bearbeitet":true,"erstelltAm":"2026-11-12T18:00:00+01:00",
                 "autor":{"name":"Petra Beispiel","rolle":"kommando","bildUrl":"/api/v1/personen/p1/bild"},
                 "eigene":true,"darfBearbeiten":true,"darfLoeschen":true}],"naechste":"m1"}""",
        )
        assertTrue(page.messages.first().deleted)
        assertNull(page.messages.first().author)
        assertEquals("kommando", page.messages.last().author?.role)
        assertEquals("m1", page.next)
    }

    @Test
    fun timesAreShownInSwissTime() {
        assertEquals("14:30", clockText("2026-11-13T13:30:00Z"))
        assertEquals(LocalDate.of(2026, 11, 14), messageDay("2026-11-13T23:30:00Z")) // nach Mitternacht in der Schweiz
        assertNull(clockText("gestern"))
    }

    @Test
    fun theDaySeparatorSitsAboveTheOldestMessageOfEachDay() {
        // Neueste zuerst: zwei am 13., eine am 12.
        val rows = historyRows(
            listOf(message("a", "2026-11-13T15:00:00+01:00"), message("b", "2026-11-13T09:00:00+01:00"), message("c", "2026-11-12T18:00:00+01:00")),
        )
        assertEquals(
            listOf("m:a", "m:b", "d:2026-11-13", "m:c", "d:2026-11-12"),
            rows.map { if (it is HistoryRow.Message) "m:${it.message.id}" else "d:${(it as HistoryRow.Day).date}" },
        )
    }

    @Test
    fun theFirstPageMergesWithLoadedOlderPagesAndFreshOnesWin() {
        val old = listOf(
            message("m3", "2026-11-13T10:00:00+01:00", text = "alt"),
            message("m2", "2026-11-12T10:00:00+01:00"),
            message("m1", "2026-11-11T10:00:00+01:00"),
        )
        val fresh = listOf(message("m4", "2026-11-13T11:00:00+01:00"), message("m3", "2026-11-13T10:00:00+01:00", text = "neu"))
        val merged = mergeFirstPage(fresh, old)
        assertEquals(listOf("m4", "m3", "m2", "m1"), merged.map { it.id })
        assertEquals("neu", merged[1].text)
        assertEquals(old, mergeFirstPage(emptyList(), old))
    }

    @Test
    fun aPushForAChannelLeadsToTheChannelAndAnythingElseNowhere() {
        val id = "7c9e6679-7425-40de-944b-e07fc1f90ae7"
        assertEquals(PushTarget.InfoChannel(id), PushTarget.from("infokanal", id))
        assertNull(PushTarget.from("infokanal", null))
        assertNull(PushTarget.from("infokanal", "../ich"))
    }
}

/** Der Verlauf gegen einen Server, der nach Pfad antwortet. */
@OptIn(ExperimentalCoroutinesApi::class)
class InfoChannelViewModelTest {
    private lateinit var server: MockWebServer
    private lateinit var viewModel: InfoChannelViewModel
    private val requests = CopyOnWriteArrayList<String>()

    @Volatile private var canWrite = true
    @Volatile private var historyBody = """{"darfSchreiben":true,"mitteilungen":[
        {"id":"m1","text":"Hallo","geloescht":false,"bearbeitet":false,"erstelltAm":"2026-11-13T10:00:00+01:00",
         "autor":{"name":"Petra Beispiel","rolle":"kommando","bildUrl":null},"eigene":true,"darfBearbeiten":true,"darfLoeschen":true}],"naechste":null}"""

    private fun json(body: String, code: Int = 200) = MockResponse.Builder().code(code).body(body).build()

    @Before
    fun start() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.url.encodedPath
                requests += "${request.method} $path"
                return when {
                    path == "/api/v1/infokanaele" -> json(
                        """{"infokanaele":[{"einheit":{"id":"e1","name":"JF Meilen","typ":"jf"},"name":"Infokanal JF Meilen",
                            "darfSchreiben":$canWrite,"ungelesen":2,"letzteMitteilungAm":null}]}""",
                    )
                    path == "/api/v1/infokanaele/e1/mitteilungen" && request.method == "GET" -> json(historyBody)
                    path == "/api/v1/infokanaele/e1/mitteilungen" && request.method == "POST" -> json(
                        """{"id":"m2","text":"Neu","geloescht":false,"bearbeitet":false,"erstelltAm":"2026-11-13T11:00:00+01:00",
                            "autor":{"name":"Petra Beispiel","rolle":"kommando","bildUrl":null},"eigene":true,"darfBearbeiten":true,"darfLoeschen":true}""",
                        201,
                    )
                    path == "/api/v1/infokanaele/e1/mitteilungen/m1" && request.method == "PUT" -> json(
                        """{"id":"m1","text":"Geändert","geloescht":false,"bearbeitet":true,"erstelltAm":"2026-11-13T10:00:00+01:00",
                            "autor":{"name":"Petra Beispiel","rolle":"kommando","bildUrl":null},"eigene":true,"darfBearbeiten":true,"darfLoeschen":true}""",
                    )
                    path == "/api/v1/infokanaele/e1/mitteilungen/m1" && request.method == "DELETE" -> MockResponse.Builder().code(204).build()
                    path == "/api/v1/infokanaele/e1/gelesen" -> MockResponse.Builder().code(204).build()
                    else -> json("{}", 404)
                }
            }
        }
        server.start()
        val storage = object : SessionStorage {
            override fun read(): String? = null
            override fun write(value: String?) = Unit
        }
        viewModel = InfoChannelViewModel(InfoChannelApi(ApiClient(server.url("/").toString(), ApiClient.httpClient(storage, "1.0.0"))))
    }

    @After
    fun stop() {
        Dispatchers.resetMain()
        server.close()
    }

    private fun <T> StateFlow<T>.await(condition: (T) -> Boolean): T = runBlocking { withTimeout(5_000) { first(condition) } }

    @Test
    fun channelsLoadOnStartAndGiveTheCountForTheTab() {
        val list = viewModel.channels.await { it != null } as ApiResult.Ok
        assertEquals(2, unreadTotal(list.value))
        assertEquals("Infokanal JF Meilen", list.value.single().name)
    }

    @Test
    fun openingTheHistoryLoadsItAndReportsItAsRead() {
        viewModel.open("e1")
        val history = viewModel.history.await { it?.loading == false }!!
        assertEquals(listOf("m1"), history.messages.map { it.id })
        assertTrue(history.canWrite)
        runBlocking { withTimeout(5_000) { while ("PUT /api/v1/infokanaele/e1/gelesen" !in requests) kotlinx.coroutines.delay(20) } }
    }

    @Test
    fun writingPutsTheNewMessageOnTopAndTheInputMayClearAfterwards() {
        viewModel.open("e1")
        viewModel.history.await { it?.loading == false }
        var done = false
        viewModel.send("Neu") { done = true }
        viewModel.history.await { it?.messages?.firstOrNull()?.id == "m2" }
        assertTrue(done)
        assertNull(viewModel.action.value.error)
    }

    @Test
    fun aFailedSendKeepsTheInputAndSaysWhy() {
        viewModel.open("e1")
        viewModel.history.await { it?.loading == false }
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = json("""{"code":"X"}""", 403)
        }
        var done = false
        viewModel.send("Neu") { done = true }
        viewModel.action.await { !it.running && it.error != null }
        assertFalse("die Eingabe bleibt stehen", done)
    }

    @Test
    fun editingReplacesTheMessageAndDeletingLeavesAPlaceholder() {
        viewModel.open("e1")
        viewModel.history.await { it?.loading == false }
        viewModel.edit("m1", "Geändert") {}
        viewModel.history.await { it?.messages?.single()?.text == "Geändert" && it.messages.single().edited }
        viewModel.delete("m1")
        val placeholder = viewModel.history.await { it?.messages?.single()?.deleted == true }!!.messages.single()
        assertNull(placeholder.text)
        assertNull(placeholder.author)
        assertFalse(placeholder.canDelete)
    }

    @Test
    fun aQuietRefreshBringsNewMessagesAndReportsThemAsRead() {
        viewModel.open("e1")
        viewModel.history.await { it?.loading == false }
        historyBody = """{"darfSchreiben":false,"mitteilungen":[
            {"id":"m9","text":"Neu","geloescht":false,"bearbeitet":false,"erstelltAm":"2026-11-13T12:00:00+01:00","autor":null,"eigene":false,"darfBearbeiten":false,"darfLoeschen":false},
            {"id":"m1","text":"Hallo","geloescht":false,"bearbeitet":false,"erstelltAm":"2026-11-13T10:00:00+01:00","autor":null,"eigene":false,"darfBearbeiten":false,"darfLoeschen":false}],"naechste":null}"""
        viewModel.refreshHistory()
        val history = viewModel.history.await { it?.messages?.size == 2 }!!
        assertEquals("m9", history.messages.first().id)
        assertFalse("das Schreibrecht kommt vom Server", history.canWrite)
    }
}
