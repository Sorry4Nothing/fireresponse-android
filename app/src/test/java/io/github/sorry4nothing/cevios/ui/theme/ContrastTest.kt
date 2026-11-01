package io.github.sorry4nothing.cevios.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/** Das Design verlangt: Textpaare mindestens 4.5:1, `outline` und `focus` mindestens 3:1 auf allen Flächen (WCAG 2.2 AA). */
class ContrastTest {
    private fun contrast(a: Color, b: Color): Double {
        val lighter = maxOf(a.luminance(), b.luminance()).toDouble()
        val darker = minOf(a.luminance(), b.luminance()).toDouble()
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun assertContrast(name: String, foreground: Color, background: Color, minimum: Double, themes: String) {
        val value = contrast(foreground, background)
        assertTrue("$themes: $name hat nur ${"%.2f".format(value)}:1, verlangt $minimum:1", value >= minimum)
    }

    private fun allThemes(block: (Palette, String) -> Unit) {
        block(CeviNight, "Nacht")
        block(CeviDay, "Tag")
    }

    private fun surfaces(p: Palette) = mapOf(
        "surface" to p.scheme.surface,
        "surface-container-low" to p.scheme.surfaceContainerLow,
        "surface-container" to p.scheme.surfaceContainer,
        "surface-container-high" to p.scheme.surfaceContainerHigh,
    )

    @Test
    fun primaryAndSecondaryTextOnAllSurfaces() = allThemes { p, t ->
        surfaces(p).forEach { (n, background) ->
            assertContrast("on-surface auf $n", p.scheme.onSurface, background, 4.5, t)
            assertContrast("on-surface-variant auf $n", p.scheme.onSurfaceVariant, background, 4.5, t)
        }
    }

    @Test
    fun redAndAccentAsTextOnSurfaceAndCards() = allThemes { p, t ->
        listOf("surface" to p.scheme.surface, "surface-container-low" to p.scheme.surfaceContainerLow).forEach { (n, background) ->
            assertContrast("primary-text auf $n", p.extra.primaryText, background, 4.5, t)
            assertContrast("accent auf $n", p.extra.accent, background, 4.5, t)
        }
    }

    @Test
    fun textOnColorSurfaces() = allThemes { p, t ->
        assertContrast("on-primary auf primary", p.scheme.onPrimary, p.scheme.primary, 4.5, t)
        assertContrast("on-secondary auf secondary", p.scheme.onSecondary, p.scheme.secondary, 4.5, t)
        assertContrast("on-secondary-muted auf secondary", p.extra.onSecondaryMuted, p.scheme.secondary, 4.5, t)
        assertContrast("on-secondary-container auf secondary-container", p.scheme.onSecondaryContainer, p.scheme.secondaryContainer, 4.5, t)
        assertContrast("on-success-container auf success-container", p.extra.onSuccessContainer, p.extra.successContainer, 4.5, t)
        assertContrast("on-error-container auf error-container", p.scheme.onErrorContainer, p.scheme.errorContainer, 4.5, t)
        assertContrast("on-warning-container auf warning-container", p.extra.onWarningContainer, p.extra.warningContainer, 4.5, t)
        assertContrast("on-stufe-container auf stufe-container", p.extra.onAgeGroupContainer, p.extra.ageGroupContainer, 4.5, t)
        assertContrast("inverse-on-surface auf inverse-surface", p.scheme.inverseOnSurface, p.scheme.inverseSurface, 4.5, t)
    }

    @Test
    fun borderAndFocusOnAllSurfaces() = allThemes { p, t ->
        surfaces(p).forEach { (n, background) ->
            assertContrast("outline auf $n", p.scheme.outline, background, 3.0, t)
            assertContrast("focus auf $n", p.extra.focus, background, 3.0, t)
        }
    }

    @Test
    fun theFormulaIsCorrect() {
        assertTrue(contrast(Color.White, Color.Black) in 20.9..21.1)
        assertTrue(contrast(Color.White, Color.White) in 0.99..1.01)
    }

    @Test
    fun bothThemesShareCeviRedAndCeviBlue() {
        assertTrue(CeviNight.scheme.primary == CeviDay.scheme.primary)
        assertTrue(CeviNight.scheme.secondary == CeviDay.scheme.secondary)
    }
}
