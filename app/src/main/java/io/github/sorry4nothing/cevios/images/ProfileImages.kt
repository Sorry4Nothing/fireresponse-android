package io.github.sorry4nothing.cevios.images

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sorry4nothing.cevios.net.ApiClient
import io.github.sorry4nothing.cevios.net.ApiError
import io.github.sorry4nothing.cevios.net.ApiResult
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/** Das Profilbild ist ein JPEG von 256 × 256 Pixeln (rund 20 bis 80 KB), das Stufenlogo ein WebP bis 512 Pixel; alles darüber ist kein Bild dieser Art. */
private const val MAX_IMAGE_BYTES = 512 * 1024

/** Bilder im Arbeitsspeicher der Sitzung: gut 60 Bilder zu 256 × 256 Pixeln. */
private const val CACHE_BYTES = 16 * 1024 * 1024

/**
 * Nur Adressen, wie der Vertrag sie für ein Profilbild oder Stufenlogo liefert: `/api/v1/personen/{id}/bild` und
 * `/api/v1/stufen/{id}/logo`, mit `?v=…` für die Fassung.
 * Alles andere (anderer Host, andere Route) lädt die App nicht: Ein fremder Host bekäme sonst das Sitzungscookie.
 */
private val IMAGE_PATH = Regex("^/api/v1/(personen/[0-9a-fA-F-]{36}/bild|stufen/[0-9a-fA-F-]{36}/logo)(\\?[A-Za-z0-9=&._-]{0,64})?$")

fun isProfileImagePath(path: String): Boolean = IMAGE_PATH.matches(path)

/** Der Zugang zu den Profilbildern des Backends. */
class ProfileImageApi(private val api: ApiClient) {
    /** Lädt ein Bild mit der Sitzung. Eine Adresse, die weder Profilbild noch Stufenlogo nennt, gilt als nicht gefunden. */
    suspend fun load(path: String): ApiResult<ByteArray> =
        if (isProfileImagePath(path)) {
            api.getBytes(path, MAX_IMAGE_BYTES)
        } else {
            ApiResult.Failure(ApiError.NOT_FOUND)
        }
}

/**
 * Die Profilbilder, die in dieser Sitzung schon gebraucht wurden, nur im Arbeitsspeicher. Das Backend verbietet das
 * Zwischenspeichern auf der Platte (`Cache-Control: private, no-store`); mit dem Ende der Sitzung ist alles weg, denn das
 * ViewModel liegt im Speicher der Sitzung. Gleiche Adressen laden gleichzeitig nur einmal (viele Nachrichten derselben Person).
 */
class ProfileImages(private val api: ProfileImageApi) : ViewModel() {
    private val cache = object : LruCache<String, Bitmap>(CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val failed = mutableSetOf<String>()
    private val loading = mutableMapOf<String, Deferred<Bitmap?>>()

    /** Das Bild, oder `null`, wenn es keines gibt oder es nicht lädt (dann bleibt die Initiale). */
    suspend fun get(path: String): Bitmap? {
        cache.get(path)?.let { return it }
        if (path in failed) return null
        return loading.getOrPut(path) { viewModelScope.async { fetch(path).also { loading.remove(path) } } }.await()
    }

    private suspend fun fetch(path: String): Bitmap? {
        val bytes = (api.load(path) as? ApiResult.Ok)?.value
        val bitmap = bytes?.let { withContext(Dispatchers.Default) { BitmapFactory.decodeByteArray(it, 0, it.size) } }
        if (bitmap == null) failed += path else cache.put(path, bitmap)
        return bitmap
    }

    override fun onCleared() {
        cache.evictAll()
        failed.clear()
    }
}

/** Die Bilder der angemeldeten Person; `null` ausserhalb einer Sitzung (dann zeigen Avatare die Initiale). */
val LocalProfileImages = staticCompositionLocalOf<ProfileImages?> { null }
