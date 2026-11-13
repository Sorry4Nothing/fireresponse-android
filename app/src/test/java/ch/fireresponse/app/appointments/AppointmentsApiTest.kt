package ch.fireresponse.app.appointments

import ch.fireresponse.app.net.ApiClient
import ch.fireresponse.app.net.ApiError
import ch.fireresponse.app.net.ApiResult
import ch.fireresponse.app.net.SessionStorage
import ch.fireresponse.app.profile.Badge
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
import java.time.LocalDate

class AppointmentsApiTest {
    private lateinit var server: MockWebServer
    private lateinit var appointments: AppointmentsApi

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        val storage = object : SessionStorage {
            override fun read(): String? = null
            override fun write(value: String?) = Unit
        }
        appointments = AppointmentsApi(ApiClient(server.url("/").toString(), ApiClient.httpClient(storage, "1.0.0")))
    }

    @After
    fun stop() = server.close()

    private fun json(code: Int, body: String) = MockResponse.Builder().code(code).body(body).build()

    /** Ein Listeneintrag im Format des Vertrags (`ProgrammListeneintrag`). */
    private val camp = """
        {"id":"7c9e6679-7425-40de-944b-e07fc1f90ae7","titel":"Sommerlager","status":"aktiv",
         "ganztaegig":true,"beginn":null,"ende":null,"tagVon":"2027-07-10","tagBis":"2027-07-17",
         "art":"Lager","anmeldbar":true,"anmeldungAb":"2027-05-29","teilnahmeOffen":false,
         "einheit":{"id":"e1","name":"JF Beispielstadt","typ":"jf"},
         "teilnehmende":[{"id":"k1","vorname":"Mia","status":"ja","abzeichen":{"kuerzel":"R2","sterne":2,"bildUrl":null}},
                         {"id":"k2","vorname":"Noah","status":null,"abzeichen":null}]}
    """.trimIndent()

    /** Das Detail dazu (`ProgrammDetail`): Thema, Info, Aufgebot und die Kontaktperson. */
    private val detail = camp.dropLast(1) + """
        ,"thema":"Lagerleben","info":"Schlafsack mitnehmen","aufgebot":"Treffpunkt Bahnhof, 08:00",
         "kontakt":{"id":"c1","name":"Petra Beispiel","rolle":"stellvertretung","bildUrl":"/api/v1/personen/c1/bild",
                    "abzeichen":{"kuerzel":"Oblt","sterne":null,"bildUrl":"/api/v1/raenge/r1/abzeichen"},"telefon":"079 000 00 00"},
         "eigeneAnmeldung":"offen"}
    """.trimIndent()

    @Test
    fun listAsksForWindowAndReadsAllFields() {
        server.enqueue(json(200, """{"programme":[$camp]}"""))
        val result = runBlocking { appointments.list(LocalDate.of(2027, 7, 1), LocalDate.of(2027, 7, 31)) }
        assertEquals("GET /api/v1/programme?von=2027-07-01&bis=2027-07-31 HTTP/1.1", server.takeRequest().requestLine)
        val p = (result as ApiResult.Ok).value.appointments.single()
        assertEquals("Sommerlager", p.title)
        assertEquals(AppointmentSquad("e1", "JF Beispielstadt", "jf"), p.squad)
        assertEquals("2027-05-29", p.registrationFrom)
        assertEquals("Lager", p.kind)
        assertFalse(p.attendanceOpen)
        assertEquals(
            listOf(AppointmentParticipant("k1", "Mia", "ja", Badge("R2", stars = 2)), AppointmentParticipant("k2", "Noah", null)),
            p.participants,
        )
        assertNull("die Liste kennt kein Detail", p.topic)
        assertNull(p.contact)
        assertNull(p.ownRegistration)
    }

    @Test
    fun theListEntryCarriesTheOwnRegistrationOfLeaders() {
        server.enqueue(json(200, """{"programme":[${camp.dropLast(1)},"eigeneAnmeldung":"offen"}]}"""))
        val p = (runBlocking { appointments.list(LocalDate.of(2027, 7, 1), LocalDate.of(2027, 7, 31)) } as ApiResult.Ok).value.appointments.single()
        assertEquals("offen", p.ownRegistration)
    }

    @Test
    fun detailReadsTopicInfoCallUpAndContact() {
        server.enqueue(json(200, detail))
        val p = (runBlocking { appointments.read("7c9e6679-7425-40de-944b-e07fc1f90ae7") } as ApiResult.Ok).value
        assertEquals("Lagerleben", p.topic)
        assertEquals("Schlafsack mitnehmen", p.info)
        assertEquals("Treffpunkt Bahnhof, 08:00", p.callUp)
        val contact = p.contact!!
        assertEquals("Petra Beispiel", contact.name)
        assertEquals("stellvertretung", contact.role)
        assertEquals("079 000 00 00", contact.phone)
        assertEquals("/api/v1/personen/c1/bild", contact.imageUrl)
        assertEquals(Badge("Oblt", stars = null, imageUrl = "/api/v1/raenge/r1/abzeichen"), contact.badge)
        assertEquals("offen", p.ownRegistration)
        assertEquals("GET /api/v1/programme/7c9e6679-7425-40de-944b-e07fc1f90ae7 HTTP/1.1", server.takeRequest().requestLine)
    }

    @Test
    fun detailWithoutContactAndTextsIsFine() {
        server.enqueue(json(200, camp.dropLast(1) + ""","thema":null,"info":null,"aufgebot":null,"kontakt":null}"""))
        val p = (runBlocking { appointments.read("x") } as ApiResult.Ok).value
        assertNull(p.contact)
        assertNull(p.topic)
    }

    @Test
    fun unknownAppointmentIsNotFound() {
        server.enqueue(json(404, """{"code":"NICHT_GEFUNDEN","message":"Nicht gefunden"}"""))
        val result = runBlocking { appointments.read("7c9e6679-7425-40de-944b-e07fc1f90ae7") }
        assertEquals(ApiResult.Failure(ApiError.NOT_FOUND), result)
    }

    @Test
    fun allDayAppointmentsHaveCalendarDays() {
        server.enqueue(json(200, camp))
        val p = (runBlocking { appointments.read("x") } as ApiResult.Ok).value
        assertEquals(Timing.AllDay(LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17)), p.time())
        assertTrue(!p.cancelled && !p.inReview)
    }

    @Test
    fun registrationOpensOnlyWhenTheWindowIsClosedAndInTheFuture() {
        server.enqueue(json(200, camp))
        val p = (runBlocking { appointments.read("x") } as ApiResult.Ok).value
        assertEquals(LocalDate.of(2027, 5, 29), p.registrationOpensOn(LocalDate.of(2027, 5, 28)))
        assertNull("am Tag selbst ist es offen", p.registrationOpensOn(LocalDate.of(2027, 5, 29)))
        assertNull("offen: nichts anzukündigen", p.copy(attendanceOpen = true).registrationOpensOn(LocalDate.of(2027, 5, 1)))
        assertNull("abgesagt: nichts anzukündigen", p.copy(status = "abgesagt").registrationOpensOn(LocalDate.of(2027, 5, 1)))
        assertNull("kein gültiger Tag", p.copy(registrationFrom = "bald").registrationOpensOn(LocalDate.of(2027, 5, 1)))
    }

    @Test
    fun attendanceSendsYesToParticipantPath() {
        server.enqueue(MockResponse.Builder().code(204).build())
        val result = runBlocking { appointments.setAttendance("p1", "k1", attending = true) }
        assertEquals(ApiResult.Ok(Unit), result)
        val request = server.takeRequest()
        assertEquals("PUT /api/v1/programme/p1/teilnahmen/k1 HTTP/1.1", request.requestLine)
        assertEquals("""{"status":"ja"}""", request.body?.utf8())
    }

    @Test
    fun leaderSendsNoToLeaderPath() {
        server.enqueue(MockResponse.Builder().code(204).build())
        // Ältere Server antworten mit 204 ohne Inhalt: Dann gilt, was die Person gewählt hat.
        assertEquals(ApiResult.Ok(LeaderAnswer("nein")), runBlocking { appointments.setLeaderRsvp("p1", attending = false) })
        val request = server.takeRequest()
        assertEquals("PUT /api/v1/programme/p1/leitung HTTP/1.1", request.requestLine)
        assertEquals("""{"status":"nein"}""", request.body?.utf8())
    }

    @Test
    fun leaderOnAFullAppointmentLandsOnTheWaitlist() {
        server.enqueue(json(200, """{"status":"warteliste","position":2}"""))
        assertEquals(ApiResult.Ok(LeaderAnswer("warteliste", 2)), runBlocking { appointments.setLeaderRsvp("p1", attending = true) })
        server.enqueue(json(200, """{"status":"ja","position":null}"""))
        assertEquals(ApiResult.Ok(LeaderAnswer("ja")), runBlocking { appointments.setLeaderRsvp("p1", attending = true) })
    }

    @Test
    fun leaderPlacesAreOnlyThereWhenTheServerSendsThem() {
        server.enqueue(json(200, detail))
        assertNull((runBlocking { appointments.read("x") } as ApiResult.Ok).value.places)
        server.enqueue(json(200, detail.dropLast(1) + ""","leitungPlaetze":{"hoechstzahl":4,"belegt":4,"frei":0,"voll":true,"wartende":2,"eigenePosition":2}}"""))
        val full = (runBlocking { appointments.read("x") } as ApiResult.Ok).value
        assertEquals(LeaderPlaces(max = 4, taken = 4, free = 0, full = true, waiting = 2, ownPosition = 2), full.places)
    }

    @Test
    fun registrationsCarryTheWaitlistAndThePlaces() {
        server.enqueue(
            json(
                200,
                """{"zahlen":{"teilnehmende":{"ja":1,"nein":0,"offen":0},"leitende":{"ja":4,"nein":0,"offen":0,"warteliste":2}},
                    "teilnehmende":[],"leitende":[{"id":"l1","name":"Lea Meier","rolle":"leitung","status":"ja","bildUrl":null,"abzeichen":null},
                    {"id":"l2","name":"Dario Huber","rolle":"leitung","status":"warteliste","position":1,"bildUrl":null,"abzeichen":null}],
                    "leitungPlaetze":{"hoechstzahl":4,"belegt":4,"frei":0,"voll":true,"wartende":2,"eigenePosition":null}}""",
            ),
        )
        val data = (runBlocking { appointments.registrations("x") } as ApiResult.Ok).value
        assertEquals(2, data.counts.leaders.warteliste)
        assertEquals(4, data.counts.leaders.total)
        assertEquals(1, data.leaders.last().position)
        assertEquals(ch.fireresponse.app.ui.ChipKind.WAITLIST, registrationChip(data.leaders.last().status))
        assertEquals(true, data.places?.full)
    }

    @Test
    fun namesAreShortenedToFirstNameAndInitial() {
        assertEquals("Lea M.", shortName("Lea Meier"))
        assertEquals("Marco K.", shortName("Marco von Keller"))
        assertEquals("Nina", shortName("Nina"))
    }

    @Test
    fun closedAppointmentGivesOwnError() {
        server.enqueue(json(409, """{"code":"PROGRAMM_GESCHLOSSEN","message":"zu"}"""))
        assertEquals(
            ApiResult.Failure(ApiError.REGISTRATION_CLOSED, "PROGRAMM_GESCHLOSSEN"),
            runBlocking { appointments.setAttendance("p1", "k1", attending = true) },
        )
    }

    @Test
    fun appointmentWithoutKindIsRead() {
        server.enqueue(json(200, camp.replace("\"art\":\"Lager\"", "\"art\":null")))
        val p = (runBlocking { appointments.read("x") } as ApiResult.Ok).value
        assertNull(p.kind)
    }

    @Test
    fun windowNotYetOpenGivesOwnError() {
        server.enqueue(json(409, """{"code":"ANMELDEFENSTER_NICHT_OFFEN","message":"Die Anmeldung öffnet am 23.01.2027."}"""))
        assertEquals(
            ApiResult.Failure(ApiError.REGISTRATION_NOT_OPEN, "ANMELDEFENSTER_NICHT_OFFEN"),
            runBlocking { appointments.setAttendance("p1", "k1", attending = true) },
        )
    }

    @Test
    fun startedAppointmentGivesOwnErrorForLeaders() {
        server.enqueue(json(409, """{"code":"PROGRAMM_BEGONNEN","message":"begonnen"}"""))
        assertEquals(
            ApiResult.Failure(ApiError.APPOINTMENT_STARTED, "PROGRAMM_BEGONNEN"),
            runBlocking { appointments.setLeaderRsvp("p1", attending = true) },
        )
    }

    @Test
    fun profileReadsUnitsRolesAndParticipants() {
        server.enqueue(
            json(
                200,
                """{"id":"u1","name":"Sandra Muster","benutzername":"smuster","kantonsadmin":false,"zweiterFaktor":false,
                    "einwilligung":true,"mussPasswortAendern":false,"telefon":null,"bildUrl":null,"pushProgramme":true,
                    "pushAnmeldungen":false,
                    "einheiten":[{"id":"e1","name":"JF Beispielstadt","typ":"jf","bezirk":"Bezirk Beispiel","rolle":"stellvertretung"}],
                    "teilnehmende":[{"id":"k1","vorname":"Mia","nachname":"Muster","einheitId":"e1","art":"eltern"}]}""",
            ),
        )
        val profile = (runBlocking { appointments.profile() } as ApiResult.Ok).value
        assertEquals("GET /api/v1/ich HTTP/1.1", server.takeRequest().requestLine)
        assertTrue(profile.consented)
        assertTrue(profile.isCommand("e1") && profile.isCommand && !profile.isLeader)
        assertFalse(profile.isCommand("e2"))
        assertEquals("Bezirk Beispiel", profile.squads.single().district)
        assertEquals("e1", profile.participants.single().squadId)
        assertEquals(false, profile.pushRegistrations)
        assertFalse(profile.hasNoSquad)
    }
}
