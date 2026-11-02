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
 * Fake-Server und lässt sich ohne Backend durchspielen. Er antwortet im Format des Vertrags (`api-vertrag/openapi.yaml`), hält
 * seinen Stand im Arbeitsspeicher und kennt zwei erfundene Konten, beide mit Startpasswort `startpasswort123`:
 * - `demo`: Elternteil von Lena und Noah, muss das Passwort ändern.
 * - `kommando`: Kommandantin der Einheit (Ansichten für das Kommando, eigene Anmeldung, Anmeldungsübersicht).
 */
class DebugApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.API_BASE_URL == "https://api.example.org") NetworkDebug.interceptor = FakeBackend()
    }
}

class FakeBackend : Interceptor {
    private var signedIn = false
    private var user = "demo"
    private var mustChange = true
    private var consented = false
    private val participantStatus = mutableMapOf("k1" to "offen", "k2" to "ja")
    private var ownStatus = "offen"

    private val leader get() = user == "kommando"
    private val mustChangeNow get() = !leader && mustChange

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val method = request.method
        val body = request.body?.let { b -> okio.Buffer().also { b.writeTo(it) }.readUtf8() }.orEmpty()
        fun ok(json: String, vararg cookies: String) = reply(request, 200, json, *cookies)
        fun noContent() = reply(request, 204, "")
        fun session(name: String) = """{"redirect":false,"token":"x","user":{"name":"$name","mussPasswortAendern":$mustChangeNow}}"""
        return when {
            path == "/api/auth/get-session" ->
                ok(if (signedIn) """{"session":{"id":"s1"},"user":{"name":"${if (leader) "Petra Beispiel" else "Sandra Muster"}","mussPasswortAendern":$mustChangeNow}}""" else "null")
            path == "/api/auth/sign-in/username" -> when {
                "\"kommando\"" in body && "startpasswort123" in body -> {
                    signedIn = true
                    user = "kommando"
                    ok(session("Petra Beispiel"), "fake_session=1; Path=/")
                }
                "\"demo\"" in body && "startpasswort123" in body || (!mustChange && "\"demo\"" in body) -> {
                    signedIn = true
                    user = "demo"
                    ok(session("Sandra Muster"), "fake_session=1; Path=/")
                }
                else -> reply(request, 401, """{"code":"ANMELDUNG_FEHLGESCHLAGEN","message":"x"}""")
            }
            path == "/api/auth/change-password" -> { mustChange = false; ok("{}") }
            path == "/api/auth/sign-out" -> { signedIn = false; ok("""{"success":true}""", "fake_session=; Max-Age=0; Path=/") }
            !signedIn -> reply(request, 401, """{"code":"NICHT_ANGEMELDET","message":"x"}""")
            path == "/api/v1/ich" && method == "GET" -> ok(profile())
            path == "/api/v1/einwilligung" && method == "GET" -> ok(consent())
            path == "/api/v1/einwilligung" && method == "PUT" -> { consented = true; noContent() }
            mustChangeNow -> reply(request, 403, """{"code":"PASSWORT_AENDERN","message":"x"}""")
            !consented -> reply(request, 403, """{"code":"EINWILLIGUNG_FEHLT","message":"x"}""")
            path == "/api/v1/ich" && method == "PATCH" -> noContent()
            path == "/api/v1/push-token" -> noContent()
            path == "/api/v1/programme" ->
                ok("""{"programme":[${appointment("a1", 3)},${appointment("a2", 3, hour = 17)},${appointment("a3", 10, cancelled = true)},${appointment("a4", 60)}]}""")
            Regex("/api/v1/programme/(\\w+)").matchEntire(path) != null && method == "GET" ->
                ok(appointment(Regex("/api/v1/programme/(\\w+)").matchEntire(path)!!.groupValues[1], 3, detail = true))
            Regex("/api/v1/programme/\\w+/teilnahmen/(\\w+)").matchEntire(path) != null && method == "PUT" -> {
                participantStatus[Regex("/teilnahmen/(\\w+)").find(path)!!.groupValues[1]] = if ("\"ja\"" in body) "ja" else "nein"
                noContent()
            }
            path.endsWith("/leitung") ->
                if (leader) { ownStatus = if ("\"ja\"" in body) "ja" else "nein"; noContent() } else reply(request, 403, """{"code":"KEINE_BERECHTIGUNG","message":"x"}""")
            path.endsWith("/teilnahmen") -> ok(
                """{"zahlen":{"teilnehmende":{"ja":1,"nein":0,"offen":1},"leitende":{"ja":${if (ownStatus == "ja") 1 else 0},"nein":${if (ownStatus == "nein") 2 else 1},"offen":${if (ownStatus == "offen") 2 else 1}}},""" +
                    """"teilnehmende":[{"id":"k1","vorname":"Lena","nachname":"Muster","status":"${participantStatus["k1"]}"},""" +
                    """{"id":"k2","vorname":"Noah","nachname":"Beispiel","status":"${participantStatus["k2"]}"}],""" +
                    (if (leader) leaders() else """"leitende":[]""") + "}",
            )
            else -> reply(request, 404, """{"code":"NICHT_GEFUNDEN","message":"x"}""")
        }
    }

    private fun leaders() =
        """"leitende":[{"id":"l1","name":"Petra Beispiel","bildUrl":null,"rolle":"kommandant","status":"$ownStatus"},""" +
            """{"id":"l2","name":"Tim Weber","bildUrl":null,"rolle":"stellvertretung","status":"offen"},""" +
            """{"id":"l3","name":"Noah Keller","bildUrl":null,"rolle":"leitung","status":"nein"}]"""

    private fun consent() =
        """{"version":1,"gueltig":$consented,"text":"Wir speichern Namen, Telefonnummer und Antworten nur für den Betrieb der Einheit. """ +
            """Die Daten bleiben in der Schweiz und werden nicht weitergegeben."}"""

    private fun profile() = if (leader) {
        """{"id":"u2","name":"Petra Beispiel","benutzername":"kommando","kantonsadmin":false,"zweiterFaktor":false,"einwilligung":$consented,""" +
            """"mussPasswortAendern":false,"telefon":"079 000 00 00","bildUrl":null,"pushProgramme":true,"pushAnmeldungen":true,""" +
            """"einheiten":[{"id":"s1","name":"Jugendfeuerwehr Musterdorf","typ":"jf","bezirk":"Bezirk Beispiel","rolle":"kommandant","logoUrl":null}],""" +
            """"teilnehmende":[]}"""
    } else {
        """{"id":"u1","name":"Sandra Muster","benutzername":"demo","kantonsadmin":false,"zweiterFaktor":false,"einwilligung":$consented,""" +
            """"mussPasswortAendern":$mustChange,"telefon":null,"bildUrl":null,"pushProgramme":true,""" +
            """"einheiten":[],"teilnehmende":[""" +
            """{"id":"k1","vorname":"Lena","nachname":"Muster","einheitId":"s1","art":"eltern"},""" +
            """{"id":"k2","vorname":"Noah","nachname":"Beispiel","einheitId":"s1","art":"eltern"}]}"""
    }

    private fun appointment(id: String, inDays: Long, hour: Int = 13, cancelled: Boolean = false, detail: Boolean = false): String {
        val day = LocalDate.now().plusDays(inDays)
        val start = day.atTime(hour, 0).toInstant(ZoneOffset.UTC)
        val title = when (id) { "a1" -> "Schläuche kuppeln"; "a2" -> "Knotenkunde"; "a4" -> "Winterlager"; else -> "Leiterübung" }
        val opensOn = day.minusDays(42)
        val open = !cancelled && !opensOn.isAfter(LocalDate.now())
        val extra = if (detail) {
            ""","thema":"Schläuche kuppeln und ausrollen","info":"Festes Schuhwerk und eine Trinkflasche mitnehmen.",""" +
                """"aufgebot":"Treffpunkt ${hour}:45 Uhr, Parkplatz beim Magazin",""" +
                """"kontakt":{"id":"c1","name":"Petra Beispiel","rolle":"kommandant","bildUrl":null,"abzeichen":{"sterne":2,"bildUrl":null},"telefon":"079 000 00 00"}"""
        } else {
            ""
        }
        // Liste und Detail tragen den eigenen Stand von Kommando und Leitenden.
        val own = if (leader) ""","eigeneAnmeldung":"$ownStatus"""" else ""
        val participants = if (leader) {
            "[]"
        } else {
            """[{"id":"k1","vorname":"Lena","status":${json("k1")},"abzeichen":{"sterne":1,"bildUrl":null}},""" +
                """{"id":"k2","vorname":"Noah","status":${json("k2")},"abzeichen":null}]"""
        }
        return """{"id":"$id","titel":"$title","status":"${if (cancelled) "abgesagt" else "aktiv"}","ganztaegig":false,""" +
            """"beginn":"$start","ende":"${start.plusSeconds(3 * 3600)}","tagVon":null,"tagBis":null,""" +
            """"art":"Übung","anmeldbar":true,"anmeldungAb":"$opensOn","teilnahmeOffen":$open,""" +
            """"einheit":{"id":"s1","name":"Jugendfeuerwehr Musterdorf","typ":"jf"},"teilnehmende":$participants$own$extra}"""
    }

    /** «offen» heisst in der Liste `null`, sonst «ja» oder «nein». */
    private fun json(child: String): String = participantStatus[child]?.takeIf { it != "offen" }?.let { "\"$it\"" } ?: "null"

    private fun reply(request: okhttp3.Request, code: Int, json: String, vararg cookies: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Fake")
            .body(json.toResponseBody("application/json".toMediaType()))
            .apply { cookies.forEach { addHeader("Set-Cookie", it) } }
            .build()
}
