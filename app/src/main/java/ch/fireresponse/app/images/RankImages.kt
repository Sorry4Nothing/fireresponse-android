package ch.fireresponse.app.images

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.util.LruCache
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.fireresponse.app.net.ApiClient
import ch.fireresponse.app.net.ApiError
import ch.fireresponse.app.net.ApiResult
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Der Server begrenzt ein Abzeichen auf 500 KB; alles darüber ist kein Abzeichen. */
private const val MAX_BADGE_BYTES = 512 * 1024

/** Höhe in Pixeln, auf die ein Abzeichen gezeichnet wird: Die App zeigt es höchstens 28 dp hoch (gut 100 Pixel bei xxhdpi). */
private const val RENDER_HEIGHT = 156

/** Gezeichnete Abzeichen im Arbeitsspeicher: Ein Abzeichen braucht rund 40 KB, es gibt je Einheit eine Handvoll. */
private const val CACHE_BYTES = 4 * 1024 * 1024

/** Seitenverhältnis des Vertrags (Breite zu Höhe), wenn ein SVG keines nennt. */
private const val DEFAULT_ASPECT = 100f / 130f

/**
 * Nur Adressen, wie der Vertrag sie für ein Abzeichen liefert: `/api/v1/raenge/{id}/abzeichen`, mit `?v=…` für die Fassung.
 * Alles andere lädt die App nicht: Ein fremder Host bekäme sonst das Sitzungscookie.
 */
private val BADGE_PATH = Regex("^/api/v1/raenge/[0-9a-fA-F-]{36}/abzeichen(\\?[A-Za-z0-9=&._-]{0,64})?$")

fun isRankImagePath(path: String): Boolean = BADGE_PATH.matches(path)

/** Das Logo einer Einheit: `/api/v1/einheiten/{id}/logo` (PNG oder SVG, wie die Abzeichen höchstens 500 KB; 404 ohne Logo). */
private val LOGO_PATH = Regex("^/api/v1/einheiten/[0-9a-fA-F-]{36}/logo(\\?[A-Za-z0-9=&._-]{0,64})?$")

fun isSquadLogoPath(path: String): Boolean = LOGO_PATH.matches(path)

/** Die Adresse des Logos einer Einheit aus ihrer Kennung; die App kennt sie aus dem Termin, auch ohne Rolle in der Einheit (Eltern). */
fun squadLogoPath(squadId: String) = "/api/v1/einheiten/$squadId/logo"

/** Der Zugang zu den Bildern des Backends, die die App zeichnet: Abzeichen der Ränge und Logos der Einheiten. */
class RankImageApi(private val api: ApiClient) {
    /** Lädt ein Bild mit der Sitzung. Eine Adresse, die weder Abzeichen noch Logo nennt, gilt als nicht gefunden. */
    suspend fun load(path: String): ApiResult<ByteArray> =
        if (isRankImagePath(path) || isSquadLogoPath(path)) api.getBytes(path, MAX_BADGE_BYTES) else ApiResult.Failure(ApiError.NOT_FOUND)
}

/**
 * Die Abzeichen-Bilder, die in dieser Sitzung schon gebraucht wurden, nur im Arbeitsspeicher (das Backend verbietet das
 * Zwischenspeichern auf der Platte). Die Bilder kommen als SVG oder PNG und werden hier einmal in ein Bitmap verwandelt; gleiche
 * Adressen laden gleichzeitig nur einmal (viele Personen mit demselben Rang).
 */
class RankImages(private val load: suspend (String) -> ApiResult<ByteArray>) : ViewModel() {
    private val cache = object : LruCache<String, Bitmap>(CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val failed = mutableSetOf<String>()
    private val loading = mutableMapOf<String, Deferred<Bitmap?>>()

    /** Das Bild, oder `null`, wenn es keines gibt oder es nicht lädt (dann bleibt das Kürzel). */
    suspend fun get(path: String): Bitmap? {
        cache.get(path)?.let { return it }
        if (path in failed) return null
        return loading.getOrPut(path) { viewModelScope.async { fetch(path).also { loading.remove(path) } } }.await()
    }

    private suspend fun fetch(path: String): Bitmap? {
        val bytes = (load(path) as? ApiResult.Ok)?.value
        val bitmap = bytes?.let { withContext(Dispatchers.Default) { decodeBadge(it) } }
        if (bitmap == null) failed += path else cache.put(path, bitmap)
        return bitmap
    }

    override fun onCleared() {
        cache.evictAll()
        failed.clear()
    }
}

/** Die Abzeichen der angemeldeten Person; `null` ausserhalb einer Sitzung (dann zeigt die App das Kürzel). */
val LocalRankImages = staticCompositionLocalOf<RankImages?> { null }

private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte())

/** Ein PNG oder SVG als Bitmap von [RENDER_HEIGHT] Pixeln Höhe; `null`, wenn die Bytes keines von beiden sind oder nicht lesbar. */
internal fun decodeBadge(bytes: ByteArray): Bitmap? = try {
    if (bytes.size > PNG_MAGIC.size && bytes.copyOf(PNG_MAGIC.size).contentEquals(PNG_MAGIC)) decodePng(bytes) else decodeSvg(bytes)
} catch (e: Exception) {
    null
}

/** Ein PNG kann bis 4000 × 5200 Pixel gross sein (80 MB als Bitmap): Es wird beim Lesen verkleinert. */
private fun decodePng(bytes: ByteArray): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outHeight / (sample * 2) >= RENDER_HEIGHT) sample *= 2
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
}

private fun decodeSvg(bytes: ByteArray): Bitmap? {
    // Der Server bereinigt das SVG schon (keine Skripte, keine Entitäten); die App liest trotzdem keine.
    SVG.setInternalEntitiesEnabled(false)
    val svg = SVG.getFromString(bytes.toString(Charsets.UTF_8))
    val aspect = svg.documentAspectRatio.takeIf { it > 0f } ?: DEFAULT_ASPECT
    val width = (RENDER_HEIGHT * aspect).roundToInt().coerceIn(1, RENDER_HEIGHT * 2)
    val bitmap = Bitmap.createBitmap(width, RENDER_HEIGHT, Bitmap.Config.ARGB_8888)
    svg.renderToCanvas(Canvas(bitmap), RectF(0f, 0f, width.toFloat(), RENDER_HEIGHT.toFloat()))
    return bitmap
}
