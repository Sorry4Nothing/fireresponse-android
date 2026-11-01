package io.github.sorry4nothing.cevios.images

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

    /** Seitenlänge und linke obere Ecke des grössten Quadrats in der Mitte. */
    data class Square(val x: Int, val y: Int, val side: Int)

    fun squareCrop(width: Int, height: Int): Square {
        val side = minOf(width, height)
        return Square((width - side) / 2, (height - side) / 2, side)
    }

    /** Grösster Teiler (Potenz von 2), bei dem die kürzere Seite noch mindestens `target` Pixel hat. */
    fun sampleSize(width: Int, height: Int, target: Int): Int {
        var sample = 1
        val shorter = minOf(width, height)
        while (shorter / (sample * 2) >= target) sample *= 2
        return sample
    }

    /** `null`, wenn die Datei sich nicht lesen oder nicht als Bild öffnen lässt. */
    fun encode(resolver: ContentResolver, uri: Uri): ByteArray? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            null
        } else {
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, SIZE) }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            val orientation = readOrientation(resolver, uri)
            decoded?.let { toJpeg(it, orientation) }
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

    private fun toJpeg(source: Bitmap, orientation: Int): ByteArray {
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrixFor(orientation), true)
        val crop = squareCrop(rotated.width, rotated.height)
        val side = minOf(crop.side, SIZE)
        val canvasBitmap = createBitmap(side, side)
        val canvas = Canvas(canvasBitmap)
        canvas.drawColor(Color.WHITE)
        val src = Rect(crop.x, crop.y, crop.x + crop.side, crop.y + crop.side)
        canvas.drawBitmap(rotated, src, Rect(0, 0, side, side), Paint(Paint.FILTER_BITMAP_FLAG))
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
