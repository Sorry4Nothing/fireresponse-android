package io.github.sorry4nothing.cevios.net

import android.content.Context
import io.github.sorry4nothing.cevios.BuildConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.security.GeneralSecurityException
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

/**
 * Marke für Anfragen im Hintergrund (regelmässiges Nachladen, Bilder, «gelesen» melden): Sie zählen nicht in
 * [ApiClient.pending] und sperren die App nicht. Aufruf: `launch(Background()) { … }`.
 */
class Background : AbstractCoroutineContextElement(Background) {
    companion object Key : CoroutineContext.Key<Background>
}

/** Dünner Zugang zur Cevi-OS-API. Antworten kommen als [ApiResult], nie als Exception. */
class ApiClient(
    baseUrl: String,
    private val http: OkHttpClient,
    val json: Json = Json { ignoreUnknownKeys = true },
    private val storage: SessionStorage? = null,
) {
    private val base: HttpUrl = baseUrl.toHttpUrl()

    private val _unauthorized = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Meldet jedes 401 ausser dem der Anmeldung: Die Sitzung ist abgelaufen oder beendet. */
    val unauthorized: SharedFlow<Unit> = _unauthorized.asSharedFlow()

    private val _forbidden = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Meldet jedes 403 unter `/api/v1`: Es kann «kein Recht» heissen oder «Passwortwechsel offen» (der Server sagt es nicht im Status). */
    val forbidden: SharedFlow<Unit> = _forbidden.asSharedFlow()

    private val _pending = MutableStateFlow(0)

    /** Anzahl der laufenden Anfragen, die eine Person ausgelöst hat (ohne [Background]); über 0 sperrt die App und zeigt den Ladekreis. */
    val pending: StateFlow<Int> = _pending.asStateFlow()

    /** Ob ein Sitzungs-Cookie da ist; ohne eins gibt es nichts zu entsperren. */
    fun hasSession(): Boolean = storage?.read() != null

    /** Vergisst das gespeicherte Cookie, auch ein halbes (`two_factor`). */
    fun forgetSession() {
        storage?.write(null)
    }

    /** Adresse einer Seite des Backends (z. B. `/datenschutz`), zum Öffnen im Browser. */
    fun pageUrl(path: String): String = request(path).build().url.toString()

    /** Anfrage an `path` (mit führendem `/`, Query erlaubt). Header und Cookie setzt der [SessionInterceptor]. */
    fun request(path: String): Request.Builder =
        Request.Builder().url(requireNotNull(base.resolve(path)) { "Ungültiger Pfad" })

    fun jsonBody(text: String): RequestBody = text.toRequestBody(JSON_TYPE)

    suspend inline fun <reified T> get(path: String): ApiResult<T> =
        send(request(path).get().build()) { json.decodeFromString<T>(it) }

    suspend inline fun <reified B, reified T> post(path: String, body: B): ApiResult<T> =
        send(request(path).post(jsonBody(json.encodeToString(body))).build()) { json.decodeFromString<T>(it) }

    /** PUT mit JSON-Body; die Antwort hat keinen Inhalt (204). */
    suspend inline fun <reified B> put(path: String, body: B): ApiResult<Unit> =
        send(request(path).put(jsonBody(json.encodeToString(body))).build()) { }

    /** PATCH mit JSON-Body; die Antwort hat keinen Inhalt (204). */
    suspend inline fun <reified B> patch(path: String, body: B): ApiResult<Unit> =
        send(request(path).patch(jsonBody(json.encodeToString(body))).build()) { }

    /** POST mit JSON-Body, ohne Interesse an der Antwort. */
    suspend inline fun <reified B> postAction(path: String, body: B): ApiResult<Unit> =
        send(request(path).post(jsonBody(json.encodeToString(body))).build()) { }

    /** PUT ohne Body; die Antwort hat keinen Inhalt (204). */
    suspend fun putWithoutBody(path: String): ApiResult<Unit> =
        send(request(path).put(ByteArray(0).toRequestBody()).build()) { }

    /** POST ohne Body; die Antwort hat keinen Inhalt (204). */
    suspend fun postWithoutBody(path: String): ApiResult<Unit> =
        send(request(path).post(ByteArray(0).toRequestBody()).build()) { }

    /** PUT mit den Bytes eines Bildes (der Typ ergibt sich beim Server aus den ersten Bytes); die Antwort hat keinen Inhalt (204). */
    suspend fun putBytes(path: String, bytes: ByteArray, type: String): ApiResult<Unit> =
        send(request(path).put(bytes.toRequestBody(type.toMediaType())).build()) { }

    /** DELETE ohne Body; die Antwort hat keinen Inhalt (204). */
    suspend fun delete(path: String): ApiResult<Unit> =
        send(request(path).delete().build()) { }

    /** GET einer Datei (Bild) als Bytes; mehr als `maxBytes` gilt als Fehler. */
    suspend fun getBytes(path: String, maxBytes: Int): ApiResult<ByteArray> =
        withContext(Background()) {
            sendRaw(request(path).get().build()) { response ->
                val source = response.body.source()
                source.request(maxBytes + 1L)
                if (source.buffer.size > maxBytes) throw IllegalArgumentException("Antwort zu gross")
                source.readByteArray()
            }
        }

    /** Für Antworten ohne Inhalt oder ohne Interesse am Inhalt: `decode = { }`. */
    suspend fun <T> send(request: Request, decode: (String) -> T): ApiResult<T> =
        sendRaw(request) { decode(it.body.string()) }

    private suspend fun <T> sendRaw(request: Request, decode: (Response) -> T): ApiResult<T> {
        val counted = coroutineContext[Background] == null
        if (counted) _pending.update { it + 1 }
        try {
            return suspendCancellableCoroutine { continuation ->
                val httpCall = http.newCall(request)
                continuation.invokeOnCancellation { httpCall.cancel() }
                httpCall.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        val error = if (e.cause is GeneralSecurityException) ApiError.UNKNOWN else ApiError.NETWORK
                        continuation.resume(ApiResult.Failure(error))
                    }

                    // Darf nie werfen, sonst bleibt die Fortsetzung hängen.
                    override fun onResponse(call: Call, response: Response) {
                        continuation.resume(response.use { evaluate(it, decode) })
                    }
                })
            }
        } finally {
            if (counted) _pending.update { it - 1 }
        }
    }

    private fun <T> evaluate(response: Response, decode: (Response) -> T): ApiResult<T> {
        if (!response.isSuccessful) {
            if (response.code == 401 && !response.request.url.encodedPath.startsWith(SIGN_IN_PATH)) _unauthorized.tryEmit(Unit)
            if (response.code == 403 && response.request.url.encodedPath.startsWith(API_PATH)) _forbidden.tryEmit(Unit)
            val code = if (response.code == 400 || response.code == 409) errorCode(response) else null
            val retryAfter = if (response.code == 429) response.header(RETRY_AFTER)?.toIntOrNull()?.coerceIn(1, MAX_RETRY_AFTER) else null
            return ApiResult.Failure(ApiError.from(response.code, code), code, retryAfter)
        }
        return try {
            ApiResult.Ok(decode(response))
        } catch (e: IOException) {
            ApiResult.Failure(ApiError.NETWORK)
        } catch (e: IllegalArgumentException) { // auch SerializationException
            ApiResult.Failure(ApiError.UNKNOWN)
        }
    }

    /** Der Fehlercode `{code, message}` des Servers; sonst `null`. Nur der Code zählt, `message` zeigt die App nie. */
    private fun errorCode(response: Response): String? = try {
        json.decodeFromString<ErrorBody>(response.body.string()).code
    } catch (e: IOException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    @Serializable
    private class ErrorBody(val code: String? = null)

    companion object {
        private const val SIGN_IN_PATH = "/api/auth/sign-in"
        private const val API_PATH = "/api/v1/"

        /** Wartezeit in Sekunden bei 429; eine Stunde ist die Obergrenze, was der Server schickt, ist nicht vertrauenswürdig. */
        private const val RETRY_AFTER = "X-Retry-After"
        private const val MAX_RETRY_AFTER = 3600
        private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

        /** Ohne Weiterleitungen: OkHttp würde den Cookie-Header sonst auch an fremde Hosts mitschicken. */
        fun httpClient(
            storage: SessionStorage,
            appVersion: String,
        ): OkHttpClient =
            OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .addInterceptor(SessionInterceptor(storage, appVersion))
                .build()

        fun create(context: Context): ApiClient {
            val storage = KeystoreSessionStorage(context)
            return ApiClient(BuildConfig.API_BASE_URL, httpClient(storage, BuildConfig.VERSION_NAME), storage = storage)
        }
    }
}
