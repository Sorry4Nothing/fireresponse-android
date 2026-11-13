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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Leiter-Plätze mit Warteliste im ViewModel: der verlorene letzte Platz und das Nachrücken. */
@OptIn(ExperimentalCoroutinesApi::class)
class LeaderPlacesViewModelTest {
    private lateinit var server: MockWebServer
    private lateinit var viewModel: AppointmentsViewModel

    @Volatile private var own = "offen"
    @Volatile private var free = 1
    @Volatile private var answer = """{"status":"warteliste","position":1}"""

    private fun appointment() = """
        {"id":"p1","titel":"Löschen","status":"aktiv","ganztaegig":false,
         "beginn":"2027-03-06T13:00:00.000Z","ende":"2027-03-06T16:00:00.000Z","tagVon":null,"tagBis":null,
         "anmeldungAb":"2027-01-23","teilnahmeOffen":true,"einheit":{"id":"e1","name":"JF Beispielstadt","typ":"jf"},
         "teilnehmende":[],"eigeneAnmeldung":"$own",
         "leitungPlaetze":{"hoechstzahl":4,"belegt":${4 - free},"frei":$free,"voll":${free == 0},"wartende":1,"eigenePosition":${if (own == "warteliste") 1 else "null"}}}
    """.trimIndent()

    private fun json(body: String, code: Int = 200) = MockResponse.Builder().code(code).body(body).build()

    @Before
    fun start() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.url.encodedPath) {
                "/api/v1/ich" -> json("""{"id":"u1","name":"Sandra","einwilligung":true,"einheiten":[],"teilnehmende":[]}""")
                "/api/v1/programme" -> json("""{"programme":[${appointment()}]}""")
                "/api/v1/programme/p1" -> json(appointment())
                "/api/v1/programme/p1/leitung" -> json(answer)
                "/api/v1/programme/p1/teilnahmen" -> json(
                    """{"zahlen":{"teilnehmende":{},"leitende":{"ja":3}},"teilnehmende":[],"leitende":[
                        {"id":"l1","name":"Lea Meier","rolle":"leitung","status":"ja"}]}""",
                )
                else -> json("{}", 404)
            }
        }
        server.start()
        val storage = object : SessionStorage {
            override fun read(): String? = null
            override fun write(value: String?) = Unit
        }
        viewModel = AppointmentsViewModel(AppointmentsApi(ApiClient(server.url("/").toString(), ApiClient.httpClient(storage, "1.0.0"))))
    }

    @After
    fun stop() {
        Dispatchers.resetMain()
        server.close()
    }

    private fun <T> StateFlow<T>.await(condition: (T) -> Boolean): T = runBlocking { withTimeout(5_000) { first(condition) } }

    private fun open() {
        viewModel.loadDetail("p1")
        viewModel.detail.await { it != null }
    }

    @Test
    fun aFreePlaceThatIsGoneByTheTimeOfTheAnswerIsSaidSo() {
        open()
        viewModel.rsvpLeader("p1", attending = true)
        val state = viewModel.rsvp.await { !it.running && it.lastSeatTaken }
        assertEquals(1, state.waitlistPosition)
    }

    @Test
    fun anAnswerOnAnAlreadyFullAppointmentIsNoSurprise() {
        free = 0
        open()
        viewModel.rsvpLeader("p1", attending = true)
        viewModel.rsvp.await { !it.running && it.sentAt.containsKey(LEADER_KEY) }
        assertFalse(viewModel.rsvp.value.lastSeatTaken)
        assertNull(viewModel.rsvp.value.waitlistPosition)
    }

    @Test
    fun anOldServerAnswerWithoutContentKeepsWhatThePersonChose() {
        answer = ""
        free = 2
        open()
        viewModel.rsvpLeader("p1", attending = true)
        viewModel.rsvp.await { !it.running && it.sentAt.containsKey(LEADER_KEY) }
        assertFalse(viewModel.rsvp.value.lastSeatTaken)
        assertNull(viewModel.rsvp.value.error)
    }

    @Test
    fun movingUpFromTheWaitlistIsNoticedOnTheNextReadAndCanBeDismissed() {
        free = 0
        own = "warteliste"
        open()
        assertFalse(viewModel.movedUp.value)
        own = "ja"
        viewModel.refreshDetail()
        assertTrue(viewModel.movedUp.await { it })
        viewModel.dismissMovedUp()
        assertFalse(viewModel.movedUp.value)
    }

    @Test
    fun namesOfThePlacesLoadWithTheDetailAndWithoutShowingALoadingState() {
        open()
        val data = viewModel.registrations.await { it != null } as ApiResult.Ok
        assertEquals("Lea Meier", data.value.leaders.single().name)
    }
}
