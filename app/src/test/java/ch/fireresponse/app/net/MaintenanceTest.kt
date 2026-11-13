package ch.fireresponse.app.net

import ch.fireresponse.app.auth.AuthApi
import ch.fireresponse.app.maintenance.maintenanceText
import kotlinx.coroutines.flow.first
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

private object NoStorage : SessionStorage {
    override fun read(): String? = null
    override fun write(value: String?) = Unit
}

class MaintenanceTest {
    private lateinit var server: MockWebServer

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun stop() = server.close()

    private fun client() = ApiClient(server.url("/").toString(), ApiClient.httpClient(NoStorage, "1.2.3"))

    private fun reply(code: Int, body: String) = MockResponse.Builder().code(code).body(body).build()

    @Test
    fun a503WithTheMaintenanceCodeIsMaintenanceAndRaisesTheFlag() {
        server.enqueue(reply(503, """{"code":"WARTUNG","message":"Wir wechseln den Server.","ende":null}"""))
        val client = client()
        assertFalse(client.maintenance.value)
        val result = runBlocking { client.get<Unit>("/api/v1/ich") }
        assertEquals(ApiResult.Failure(ApiError.MAINTENANCE, "WARTUNG"), result)
        assertTrue(client.maintenance.value)
        client.maintenanceOver()
        assertFalse(client.maintenance.value)
    }

    @Test
    fun a503WithoutTheCodeStaysAnOrdinaryError() {
        server.enqueue(reply(503, """{"message":"Bad Gateway"}"""))
        server.enqueue(reply(503, "<html>nginx</html>"))
        val client = client()
        assertEquals(ApiResult.Failure(ApiError.UNKNOWN), runBlocking { client.get<Unit>("/api/v1/ich") })
        assertEquals(ApiResult.Failure(ApiError.UNKNOWN), runBlocking { client.get<Unit>("/api/v1/ich") })
        assertFalse(client.maintenance.value)
    }

    @Test
    fun signInDuringMaintenanceIsMaintenanceToo() {
        server.enqueue(reply(503, """{"code":"WARTUNG","message":"x"}"""))
        val client = client()
        val result = runBlocking { AuthApi(client).signIn("demo", "pw") }
        assertEquals(ApiError.MAINTENANCE, (result as ApiResult.Failure).error)
        assertTrue(client.maintenance.value)
    }

    @Test
    fun readsTheStatusWithAndWithoutTextAndEnd() {
        server.enqueue(reply(200, """{"aktiv":false,"text":null,"ende":null,"seit":null}"""))
        server.enqueue(reply(200, """{"aktiv":true,"text":"Neue Version","ende":"2026-11-13T21:00:00+01:00","seit":"2026-11-13T19:00:00+01:00"}"""))
        val api = MaintenanceApi(client())
        assertEquals(ApiResult.Ok(MaintenanceStatus(aktiv = false)), runBlocking { api.status() })
        val active = (runBlocking { api.status() } as ApiResult.Ok).value
        assertTrue(active.aktiv)
        assertEquals("Neue Version", active.text)
        assertEquals("/api/v1/wartung", server.takeRequest().url.encodedPath)
    }

    @Test
    fun theEndIsShownInSwissTimeWithTheDateOnlyOnAnotherDay() {
        val today = LocalDate.of(2026, 11, 13)
        assertEquals("22:00 Uhr", maintenanceEnd("2026-11-13T21:00:00+00:00", today)) // Winterzeit: 21:00 UTC = 22:00
        assertEquals("14. November, 06:30 Uhr", maintenanceEnd("2026-11-14T06:30:00+01:00", today))
        assertEquals("02:00 Uhr", maintenanceEnd("2026-07-01T00:00:00Z", LocalDate.of(2026, 7, 1)))
        assertNull(maintenanceEnd(null, today))
        assertNull("nicht lesbar", maintenanceEnd("bald", today))
    }

    @Test
    fun theTextUsesTheServerTextOrTheDefaultAndNamesTheEnd() {
        val today = LocalDate.of(2026, 11, 13)
        val standard = "Standard."
        val saved = "Gespeichert."
        assertEquals("Standard. Gespeichert.", maintenanceText(standard, "Bis %1\$s.", saved, null, today))
        assertEquals("Standard. Gespeichert.", maintenanceText(standard, "Bis %1\$s.", saved, MaintenanceStatus(true, "  "), today))
        assertEquals(
            "Neue Version. Bis 22:00 Uhr. Gespeichert.",
            maintenanceText(standard, "Bis %1\$s.", saved, MaintenanceStatus(true, "Neue Version.", "2026-11-13T22:00:00+01:00"), today),
        )
    }
}
