package ch.fireresponse.app.images

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.media.ExifInterface
import androidx.core.graphics.createBitmap
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Bereitet ein gewähltes Foto für den Upload vor: nach EXIF drehen, die Mitte quadratisch zuschneiden, auf 512 Pixel
 * verkleinern, als JPEG speichern. Aufnahmeort und andere Metadaten verlassen das Gerät so nie; Transparenz wird weiss.
 * Der Server skaliert danach auf 256 Pixel und schneidet nicht noch einmal zu.
 */
// Das Rahmenwerk genügt: Das Foto stammt aus der Auswahl der Person selbst, und ein beschädigter EXIF-Block fällt unten auf «nicht gedreht». Die
// Bibliothek androidx.exifinterface wäre robuster, brächte aber eine Abhängigkeit für einen Sonderfall.
@SuppressLint("ExifInterface")
object ProfileImageEncoder {
    const val SIZE = 512
    private const val QUALITY = 88

    /** Seitenlänge und linke obere Ecke eines Quadrats im Foto. */
    data class Square(val x: Int, val y: Int, val side: Int)

    const val MAX_ZOOM = 4f

    /** Die Höhe, auf die vergrössert wird, wenn die Person den Ausschnitt wählt (Platz für den Bildschirm, nicht für den Upload). */
    private const val PREVIEW_SIZE = 1024

    /**
     * Der Ausschnitt, den die Person wählt: Mitte als Anteil der Breite und Höhe (0 bis 1) und Vergrösserung ab 1 (1 = das grösste
     * Quadrat). Ohne Angabe gilt die Mitte, wie bevor es einen Zuschnitt gab.
     */
    data class Crop(val centerX: Float = 0.5f, val centerY: Float = 0.5f, val zoom: Float = 1f) {
        /** Mitte und Zoom so, dass der Ausschnitt im Foto liegt: Wer über den Rand schiebt, bleibt am Rand. */
        fun normalized(width: Int, height: Int): Crop {
            val square = squareCrop(width, height, this)
            return Crop((square.x + square.side / 2f) / width, (square.y + square.side / 2f) / height, zoom.coerceIn(1f, MAX_ZOOM))
        }

        /** Verschiebt den Ausschnitt um Pixel des Fotos. */
        fun moved(dx: Float, dy: Float, width: Int, height: Int): Crop =
            Crop(centerX + dx / width, centerY + dy / height, zoom).normalized(width, height)

        fun zoomed(factor: Float, width: Int, height: Int): Crop = copy(zoom = zoom * factor).normalized(width, height)
    }

    fun squareCrop(width: Int, height: Int, crop: Crop = Crop()): Square {
        val side = (minOf(width, height) / crop.zoom.coerceIn(1f, MAX_ZOOM)).toInt().coerceAtLeast(1)
        val x = (crop.centerX * width - side / 2f).toInt().coerceIn(0, width - side)
        val y = (crop.centerY * height - side / 2f).toInt().coerceIn(0, height - side)
        return Square(x, y, side)
    }

    /** Grösster Teiler (Potenz von 2), bei dem die kürzere Seite noch mindestens `target` Pixel hat. */
    fun sampleSize(width: Int, height: Int, target: Int): Int {
        var sample = 1
        val shorter = minOf(width, height)
        while (shorter / (sample * 2) >= target) sample *= 2
        return sample
    }

    /** `null`, wenn die Datei sich nicht lesen oder nicht als Bild öffnen lässt. Der Ausschnitt gilt für das aufrecht gedrehte Foto. */
    fun encode(resolver: ContentResolver, uri: Uri, crop: Crop = Crop()): ByteArray? {
        // Vergrössert die Person, muss das Foto feiner gelesen werden, damit der Ausschnitt noch 512 Pixel hat.
        val target = Math.ceil((SIZE * crop.zoom.coerceIn(1f, MAX_ZOOM)).toDouble()).toInt()
        return decodeUpright(resolver, uri, target)?.let { toJpeg(it, crop) }
    }

    /** Das Foto für den Zuschneide-Bildschirm: aufrecht gedreht, nicht grösser als nötig. */
    fun preview(resolver: ContentResolver, uri: Uri): Bitmap? = decodeUpright(resolver, uri, PREVIEW_SIZE)

    /** Das Foto nach EXIF gedreht, mit einer kürzeren Seite von mindestens `target` Pixeln (oder weniger, wenn es kleiner ist). */
    private fun decodeUpright(resolver: ContentResolver, uri: Uri, target: Int): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            null
        } else {
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, target) }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            val orientation = readOrientation(resolver, uri)
            decoded?.let { Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrixFor(orientation), true) }
        }
    } catch (e: IOException) {
        null
    } catch (e: SecurityException) {
        null
    } catch (e: OutOfMemoryError) {
        null
    }

    /** Die Drehung aus dem Foto; unlesbar oder ohne Angabe heisst: nicht drehen. */
    private fun readOrientation(resolver: ContentResolver, uri: Uri): Int = try {
        resolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
    } catch (e: IOException) {
        null
    } catch (e: RuntimeException) {
        null
    } ?: ExifInterface.ORIENTATION_NORMAL

    private fun toJpeg(upright: Bitmap, chosen: Crop): ByteArray {
        val crop = squareCrop(upright.width, upright.height, chosen)
        val side = minOf(crop.side, SIZE)
        val canvasBitmap = createBitmap(side, side)
        val canvas = Canvas(canvasBitmap)
        canvas.drawColor(Color.WHITE)
        val src = Rect(crop.x, crop.y, crop.x + crop.side, crop.y + crop.side)
        canvas.drawBitmap(upright, src, Rect(0, 0, side, side), Paint(Paint.FILTER_BITMAP_FLAG))
        val out = ByteArrayOutputStream()
        canvasBitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
        return out.toByteArray()
    }

    /** Die Drehung und Spiegelung, die das Foto laut EXIF noch braucht, damit es aufrecht steht. */
    private fun matrixFor(orientation: Int): Matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
        }
    }
}
