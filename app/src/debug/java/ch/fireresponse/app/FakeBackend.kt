package ch.fireresponse.app

import android.app.Application
import ch.fireresponse.app.net.NetworkDebug
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Nur im Debug-Build, nur wenn keine echte Adresse eingestellt ist (`fireresponseApiBaseUrl`): Die App läuft dann gegen diesen
 * Fake-Server und lässt sich ohne Backend durchspielen. Er antwortet im Format des bisherigen Vertrags, hält seinen Stand im
 * Arbeitsspeicher und kennt ein erfundenes Konto: Benutzername `demo`, Startpasswort `startpasswort123`. Mit dem neuen Vertrag (E4)
 * wird er angepasst oder entfällt.
 */
class DebugApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.API_BASE_URL == "https://api.example.org") NetworkDebug.interceptor = FakeBackend()
    }
}

class FakeBackend : Interceptor {
    private var signedIn = false
    private var mustChange = true
    private var privacyRead = false
    private var appointmentsOpen = true
    private val participantStatus = mutableMapOf("k1" to "offen", "k2" to "ja")
    private var leaderStatus = "offen"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val method = request.method
        val body = request.body?.let { b -> okio.Buffer().also { b.writeTo(it) }.readUtf8() }.orEmpty()
        fun ok(json: String, cookie: String? = null) = reply(request, 200, json, cookie)
        fun noContent() = reply(request, 204, "")
        return when {
            path == "/api/auth/get-session" ->
                ok(if (signedIn) """{"user":{"name":"Sandra Muster","mussPasswortAendern":$mustChange}}""" else "null")
            path == "/api/auth/sign-in/username" -> {
                if ("\"demo\"" in body && "startpasswort123" in body || (!mustChange && "\"demo\"" in body)) {
                    signedIn = true
                    ok("""{"twoFactorRedirect":false}""", "fake_session=1; Path=/")
                } else {
                    reply(request, 401, """{"code":"INVALID_USERNAME_OR_PASSWORD","message":"x"}""")
                }
            }
            path == "/api/auth/change-password" -> { mustChange = false; ok("{}") }
            path == "/api/auth/sign-out" -> { signedIn = false; ok("{}", "fake_session=; Max-Age=0; Path=/") }
            !signedIn -> reply(request, 401, """{"code":"NICHT_ANGEMELDET","message":"x"}""")
            path == "/api/v1/ich" && method == "GET" -> ok(profile())
            path == "/api/v1/ich" && method == "PATCH" -> noContent()
            path == "/api/v1/ich/datenschutz" -> { privacyRead = true; noContent() }
            path == "/api/v1/push-token" -> noContent()
            path == "/api/v1/programme" -> ok("""{"programme":[${appointment("a1", 3)},${appointment("a2", 3, hour = 17)},${appointment("a3", 10, cancelled = true)}]}""")
            Regex("/api/v1/programme/(\\w+)").matchEntire(path) != null && method == "GET" ->
                ok(appointment(Regex("/api/v1/programme/(\\w+)").matchEntire(path)!!.groupValues[1], 3))
            path.endsWith("/kontakte") -> ok("""{"kontakte":[{"id":"c1","name":"Petra Beispiel","telefon":"079 000 00 00"}]}""")
            Regex("/api/v1/programme/\\w+/teilnahmen/(\\w+)").matchEntire(path) != null && method == "PUT" -> {
                participantStatus[Regex("/teilnahmen/(\\w+)").find(path)!!.groupValues[1]] = if ("\"ja\"" in body) "ja" else "nein"
                noContent()
            }
            path.endsWith("/leitung") -> { leaderStatus = if ("\"ja\"" in body) "ja" else "nein"; noContent() }
            path.endsWith("/teilnahmen") -> ok(
                """{"zahlen":{"kinder":{"ja":1,"nein":0,"offen":1},"leitende":{"ja":1,"nein":0,"offen":0}},""" +
                    """"kinder":[{"id":"k1","vorname":"Lena","nachname":"Muster","status":"${participantStatus["k1"]}"},""" +
                    """{"id":"k2","vorname":"Noah","nachname":"Beispiel","status":"${participantStatus["k2"]}"}],""" +
                    """"leitende":[{"id":"l1","name":"Petra Beispiel","rolle":"leitung","status":"ja"}]}""",
            )
            path == "/api/v1/stufen" -> ok("""{"stufen":[{"id":"s1","name":"Jugendfeuerwehr Musterdorf"}]}""")
            path.startsWith("/api/v1/personen/") -> reply(request, 404, """{"code":"NICHT_GEFUNDEN","message":"x"}""")
            else -> reply(request, 404, """{"code":"NICHT_GEFUNDEN","message":"x"}""")
        }
    }

    private fun profile() =
        """{"id":"u1","admin":false,"name":"Sandra Muster","benutzername":"demo","mussPasswortAendern":$mustChange,""" +
            """"datenschutzGelesenAm":${if (privacyRead) "\"2026-11-01T10:00:00.000Z\"" else "null"},""" +
            """"rollen":[],"stufen":[{"id":"s1","name":"Jugendfeuerwehr Musterdorf","kuerzel":"JF"}],""" +
            """"kinder":[{"id":"k1","stufeId":"s1","vorname":"Lena"},{"id":"k2","stufeId":"s1","vorname":"Noah"}],""" +
            """"pushProgramme":true}"""

    private fun appointment(id: String, inDays: Long, hour: Int = 13, cancelled: Boolean = false): String {
        val day = LocalDate.now().plusDays(inDays)
        val start = day.atTime(hour, 0).toInstant(ZoneOffset.UTC)
        val title = when (id) { "a1" -> "Schläuche kuppeln"; "a2" -> "Knotenkunde"; else -> "Leiterübung" }
        return """{"id":"$id","titel":"$title","status":"${if (cancelled) "abgesagt" else "aktiv"}","sichtbarkeit":"eltern","ganztaegig":false,""" +
            """"beginn":"$start","ende":"${start.plusSeconds(3 * 3600)}","googleOrt":"Feuerwehrmagazin","treffpunkt":"${hour + 1}:45 Uhr\nParkplatz beim Magazin",""" +
            """"schlussOrt":"Feuerwehrmagazin","schlussZeit":"${hour + 4}:00","mitnehmen":"Festes Schuhwerk",""" +
            """"stufen":[{"id":"s1","name":"Jugendfeuerwehr Musterdorf"}],"teilnahmeOffen":$appointmentsOpen,""" +
            """"kinder":[{"id":"k1","vorname":"Lena","status":${json("k1")}},""" +
            """{"id":"k2","vorname":"Noah","status":${json("k2")}}],"eigeneAnmeldung":null}"""
    }

    /** «offen» heisst in der Liste `null`, sonst «ja» oder «nein». */
    private fun json(child: String): String = participantStatus[child]?.takeIf { it != "offen" }?.let { "\"$it\"" } ?: "null"

    private fun reply(request: okhttp3.Request, code: Int, json: String, cookie: String? = null): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Fake")
            .body(json.toResponseBody("application/json".toMediaType()))
            .apply { cookie?.let { addHeader("Set-Cookie", it) } }
            .build()
}
