package ch.fireresponse.app.appointments

import ch.fireresponse.app.net.ApiClient
import ch.fireresponse.app.net.ApiResult
import ch.fireresponse.app.net.SessionStorage
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/** Das ViewModel gegen einen Server, der nach Pfad antwortet; `Dispatchers.Main` läuft ohne Verzögerung. */
@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentsViewModelTest {
    private lateinit var server: MockWebServer
    private lateinit var viewModel: AppointmentsViewModel

    @Volatile private var lenaStatus: String? = null
    @Volatile private var listFails = false
    private val listCalls = AtomicInteger()

    private fun appointment(detail: Boolean) = """
        {"id":"p1","titel":"Schläuche kuppeln","status":"aktiv","ganztaegig":false,
         "beginn":"2027-03-06T13:00:00.000Z","ende":"2027-03-06T16:00:00.000Z","tagVon":null,"tagBis":null,
         "art":"Übung","anmeldbar":true,"anmeldungAb":"2027-01-23","teilnahmeOffen":true,
         "einheit":{"id":"e1","name":"JF Beispielstadt","typ":"jf"},
         "teilnehmende":[{"id":"k1","vorname":"Lena","status":${lenaStatus?.let { "\"$it\"" } ?: "null"},"abzeichen":null}]
         ${if (detail) ""","thema":"Schläuche","info":null,"aufgebot":null,"kontakt":null""" else ""}}
    """.trimIndent()

    private fun json(body: String, code: Int = 200) = MockResponse.Builder().code(code).body(body).build()

    @Before
    fun start() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.url.encodedPath
                return when {
                    path == "/api/v1/ich" -> json("""{"id":"u1","name":"Sandra","einwilligung":true,"einheiten":[],"teilnehmende":[]}""")
                    path == "/api/v1/programme" -> {
                        listCalls.incrementAndGet()
                        if (listFails) json("{}", 500) else json("""{"programme":[${appointment(detail = false)}]}""")
                    }
                    path == "/api/v1/programme/p1" -> json(appointment(detail = true))
                    path == "/api/v1/programme/p1/teilnahmen/k1" -> {
                        lenaStatus = "ja"
                        MockResponse.Builder().code(204).build()
                    }
                    else -> json("{}", 404)
                }
            }
        }
        server.start()
        val storage = object : SessionStorage {
            override fun read(): String? = null
            override fun write(value: String?) = Unit
        }
        val api = AppointmentsApi(ApiClient(server.url("/").toString(), ApiClient.httpClient(storage, "1.0.0")))
        viewModel = AppointmentsViewModel(api)
    }

    @After
    fun stop() {
        Dispatchers.resetMain()
        server.close()
    }

    private fun <T> StateFlow<T>.await(condition: (T) -> Boolean): T = runBlocking { withTimeout(5_000) { first(condition) } }

    private fun lenaInList(): String? =
        ((viewModel.list.value as? ApiResult.Ok)?.value?.appointments?.singleOrNull()?.participants?.singleOrNull())?.status

    @Test
    fun listAndProfileLoadOnStart() {
        val list = viewModel.list.await { it != null } as ApiResult.Ok
        assertEquals("Schläuche kuppeln", list.value.appointments.single().title)
        assertTrue(viewModel.profile.await { it != null } is ApiResult.Ok)
    }

    @Test
    fun aSavedAnswerReadsDetailAndListAgain() {
        viewModel.list.await { it != null }
        viewModel.loadDetail("p1")
        viewModel.detail.await { it != null }
        viewModel.rsvpParticipant("p1", "k1", attending = true)
        viewModel.list.await { (it as? ApiResult.Ok)?.value?.appointments?.single()?.participants?.single()?.status == "ja" }
        val detail = viewModel.detail.await { (it as? ApiResult.Ok)?.value?.participants?.single()?.status == "ja" } as ApiResult.Ok
        assertEquals("Schläuche", detail.value.topic)
        assertEquals(null, viewModel.rsvp.value.error)
    }

    @Test
    fun pullingReloadsWithoutLoadingStateAndAFailureKeepsTheListAndSaysSo() {
        viewModel.list.await { it != null }
        lenaStatus = "ja"
        viewModel.pullRefresh()
        viewModel.list.await { lenaInList() == "ja" }
        assertEquals(false, viewModel.refreshing.await { !it })
        assertEquals(null, viewModel.refreshError.value)

        listFails = true
        lenaStatus = "nein"
        viewModel.pullRefresh()
        viewModel.refreshError.await { it != null }
        assertEquals("die Liste bleibt, wie sie war", "ja", lenaInList())
        assertEquals(false, viewModel.refreshing.await { !it })

        listFails = false
        viewModel.pullRefresh()
        viewModel.refreshError.await { it == null }
        viewModel.list.await { lenaInList() == "nein" }
    }

    @Test
    fun aFailingRefreshKeepsTheOldListInsteadOfShowingAnError() {
        viewModel.list.await { it != null }
        viewModel.loadDetail("p1")
        viewModel.detail.await { it != null }
        val before = listCalls.get()
        listFails = true
        viewModel.rsvpParticipant("p1", "k1", attending = true)
        viewModel.detail.await { (it as? ApiResult.Ok)?.value?.participants?.single()?.status == "ja" }
        runBlocking { withTimeout(5_000) { while (listCalls.get() == before) kotlinx.coroutines.delay(20) } }
        runBlocking { kotlinx.coroutines.delay(200) }
        assertTrue("die Liste bleibt, wie sie war", viewModel.list.value is ApiResult.Ok)
        assertEquals(null, lenaInList())
    }

}
