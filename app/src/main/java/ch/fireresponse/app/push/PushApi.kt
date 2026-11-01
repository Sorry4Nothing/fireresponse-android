package ch.fireresponse.app.push

import ch.fireresponse.app.net.ApiClient
import ch.fireresponse.app.net.ApiResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Kein Standardwert für `platform`: `ApiClient` lässt Standardwerte beim Senden weg, der Server verlangt das Feld. */
@Serializable
data class PushTokenRequest(val token: String, @SerialName("plattform") val platform: String)

/** Genau ein Schalter pro Anfrage; der Server verlangt mindestens ein Feld, `null` bleibt weg. */
@Serializable
data class PushSwitchesRequest(
    @SerialName("pushProgramme") val programs: Boolean? = null,
    @SerialName("pushAnmeldungen") val registrations: Boolean? = null,
)

class PushApi(private val api: ApiClient) {
    /** Hinterlegt das FCM-Token dieses Geräts bei der Sitzung; ein neues ersetzt das alte. */
    suspend fun registerToken(token: String): ApiResult<Unit> =
        api.put("/api/v1/push-token", PushTokenRequest(token, "android"))

    suspend fun setSwitches(request: PushSwitchesRequest): ApiResult<Unit> = api.patch("/api/v1/ich", request)
}
