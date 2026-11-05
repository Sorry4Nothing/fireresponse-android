package ch.fireresponse.app.images

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileImageEncoderTest {
    @Test
    fun squareCropTakesTheMiddle() {
        assertEquals(ProfileImageEncoder.Square(x = 500, y = 0, side = 3000), ProfileImageEncoder.squareCrop(4000, 3000))
        assertEquals(ProfileImageEncoder.Square(x = 0, y = 1000, side = 1000), ProfileImageEncoder.squareCrop(1000, 3000))
        assertEquals(ProfileImageEncoder.Square(x = 0, y = 0, side = 400), ProfileImageEncoder.squareCrop(400, 400))
    }

    @Test
    fun zoomShrinksTheSquareAroundTheChosenCenter() {
        val crop = ProfileImageEncoder.Crop(centerX = 0.5f, centerY = 0.25f, zoom = 2f)
        assertEquals(ProfileImageEncoder.Square(x = 1250, y = 0, side = 1500), ProfileImageEncoder.squareCrop(4000, 3000, crop))
        assertEquals(ProfileImageEncoder.Square(x = 250, y = 1250, side = 500), ProfileImageEncoder.squareCrop(1000, 3000, ProfileImageEncoder.Crop(0.5f, 0.5f, 2f)))
    }

    @Test
    fun theSquareNeverLeavesThePhoto() {
        val corner = ProfileImageEncoder.Crop(centerX = 0f, centerY = 1f, zoom = 4f)
        val square = ProfileImageEncoder.squareCrop(4000, 3000, corner)
        assertEquals(ProfileImageEncoder.Square(x = 0, y = 2250, side = 750), square)
        // Zoom über dem Höchstwert und unter 1 wird begrenzt.
        assertEquals(750, ProfileImageEncoder.squareCrop(4000, 3000, ProfileImageEncoder.Crop(zoom = 99f)).side)
        assertEquals(3000, ProfileImageEncoder.squareCrop(4000, 3000, ProfileImageEncoder.Crop(zoom = 0.2f)).side)
    }

    @Test
    fun movingStopsAtTheEdgeAndKeepsTheChosenZoom() {
        val start = ProfileImageEncoder.Crop(zoom = 2f)
        val moved = start.moved(dx = 100_000f, dy = 0f, width = 4000, height = 3000)
        assertEquals(ProfileImageEncoder.squareCrop(4000, 3000, moved).x + 1500, 4000)
        assertEquals(2f, moved.zoom, 0f)
        assertEquals(start.normalized(4000, 3000), start.moved(0f, 0f, 4000, 3000))
        assertEquals(4f, ProfileImageEncoder.Crop().zoomed(10f, 4000, 3000).zoom, 0f)
        assertEquals(1f, ProfileImageEncoder.Crop().zoomed(0.1f, 4000, 3000).zoom, 0f)
    }

    @Test
    fun sampleSizeKeepsTheShorterSideAtLeastTheTarget() {
        assertEquals(1, ProfileImageEncoder.sampleSize(800, 600, 512))
        assertEquals(2, ProfileImageEncoder.sampleSize(4000, 1100, 512))
        assertEquals(4, ProfileImageEncoder.sampleSize(4000, 3000, 512))
        assertEquals(1, ProfileImageEncoder.sampleSize(300, 300, 512))
    }
}
