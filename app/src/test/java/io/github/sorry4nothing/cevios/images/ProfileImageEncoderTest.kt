package io.github.sorry4nothing.cevios.images

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
    fun sampleSizeKeepsTheShorterSideAtLeastTheTarget() {
        assertEquals(1, ProfileImageEncoder.sampleSize(800, 600, 512))
        assertEquals(2, ProfileImageEncoder.sampleSize(4000, 1100, 512))
        assertEquals(4, ProfileImageEncoder.sampleSize(4000, 3000, 512))
        assertEquals(1, ProfileImageEncoder.sampleSize(300, 300, 512))
    }
}
