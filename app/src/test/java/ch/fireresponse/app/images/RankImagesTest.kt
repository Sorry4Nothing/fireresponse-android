package ch.fireresponse.app.images

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RankImagesTest {
    private val id = "7c9e6679-7425-40de-944b-e07fc1f90ae7"

    @Test
    fun acceptsOnlyTheBadgeRouteOfTheContract() {
        assertTrue(isRankImagePath("/api/v1/raenge/$id/abzeichen"))
        assertTrue(isRankImagePath("/api/v1/raenge/$id/abzeichen?v=3"))
    }

    @Test
    fun rejectsEverythingElse() {
        assertFalse(isRankImagePath("https://fremd.example/api/v1/raenge/$id/abzeichen"))
        assertFalse(isRankImagePath("/api/v1/personen/$id/bild"))
        assertFalse(isRankImagePath("/api/v1/raenge/../ich/abzeichen"))
        assertFalse(isRankImagePath("/api/v1/raenge/$id/abzeichen/x"))
        assertFalse(isRankImagePath("/api/v1/raenge/$id/abzeichen?v=3&<script>"))
    }
}
