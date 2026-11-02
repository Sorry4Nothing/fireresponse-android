package ch.fireresponse.app.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyTextTest {
    @Test
    fun paragraphsAreSeparatedByBlankLines() {
        assertEquals(
            listOf(PrivacyBlock.Paragraph("Erster Absatz."), PrivacyBlock.Paragraph("Zweiter Absatz\nüber zwei Zeilen.")),
            privacyBlocks("Erster Absatz.\n\nZweiter Absatz\nüber zwei Zeilen."),
        )
    }

    @Test
    fun consecutiveBulletsFormOneList() {
        assertEquals(
            listOf(PrivacyBlock.Bullets(listOf("Namen", "Zusagen", "Profilbild"))),
            privacyBlocks("– Namen\n\n– Zusagen\n\n– Profilbild"),
        )
    }

    @Test
    fun aListAfterAParagraphStaysSeparate() {
        assertEquals(
            listOf(
                PrivacyBlock.Paragraph("Wir speichern:"),
                PrivacyBlock.Bullets(listOf("Namen", "Zusagen")),
                PrivacyBlock.Paragraph("Mehr nicht."),
            ),
            privacyBlocks("Wir speichern:\n\n– Namen\n\n– Zusagen\n\nMehr nicht."),
        )
    }

    @Test
    fun surroundingSpaceAndEmptyTextGiveNoEmptyBlocks() {
        assertTrue(privacyBlocks("").isEmpty())
        assertTrue(privacyBlocks(" \n\n ").isEmpty())
        assertEquals(listOf(PrivacyBlock.Paragraph("Text")), privacyBlocks("\n  Text  \n"))
    }

    @Test
    fun aDashWithoutSpaceIsNoBullet() {
        assertEquals(listOf(PrivacyBlock.Paragraph("–Gedankenstrich ohne Leerzeichen")), privacyBlocks("–Gedankenstrich ohne Leerzeichen"))
    }
}
