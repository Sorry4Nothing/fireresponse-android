package io.github.sorry4nothing.cevios.net

import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/** Ort, an dem die Sitzung als `name=wert; name2=wert2` liegt (auf dem Gerät verschlüsselt). */
interface SessionStorage {
    fun read(): String?
    fun write(value: String?)
}

/**
 * Setzt `X-App-Version`, `Origin` und das gespeicherte Cookie bei jedem Aufruf und merkt sich die Cookies
 * aus den Antworten. Wirft nur IOException, sonst stürzt OkHttp auf seinem Thread ab.
 */
class SessionInterceptor(
    private val storage: SessionStorage,
    private val appVersion: String,
) : Interceptor {
    private val lock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("X-App-Version", appVersion)
            .header("Origin", ORIGIN)
        synchronized(lock) { storage.read() }?.let { request.header("Cookie", it) }

        val response = chain.proceed(request.build())

        val received = response.headers("Set-Cookie")
        if (received.isNotEmpty()) {
            synchronized(lock) {
                storage.write(mergeCookies(storage.read(), response.request.url, received))
            }
        }
        return response
    }

    companion object {
        /** Das App-Schema, dem das Backend für POSTs mit Cookie vertraut. */
        const val ORIGIN = "cevi-os://"
    }
}

/** Übernimmt `name=wert` aus den `Set-Cookie`-Headern, ohne Attribute. Abgelaufene Cookies (`Max-Age=0`) fallen weg. */
internal fun mergeCookies(current: String?, url: HttpUrl, setCookies: List<String>): String? {
    val pairs = linkedMapOf<String, String>()
    current?.split("; ")?.forEach { pair ->
        val parts = pair.split("=", limit = 2)
        if (parts.size == 2) pairs[parts[0]] = parts[1]
    }
    for (header in setCookies) {
        val cookie = Cookie.parse(url, header) ?: continue
        if (cookie.expiresAt < System.currentTimeMillis()) pairs.remove(cookie.name) else pairs[cookie.name] = cookie.value
    }
    return pairs.entries.joinToString("; ") { "${it.key}=${it.value}" }.ifEmpty { null }
}
