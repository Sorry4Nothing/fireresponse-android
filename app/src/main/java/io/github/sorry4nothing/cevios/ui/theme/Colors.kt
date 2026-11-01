package io.github.sorry4nothing.cevios.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Rollen, die Material 3 nicht kennt (Design System «Cevi App», `tokens.json`).
 * Kein Zustand nur über Farbe: Jede dieser Farben steht immer mit Icon und Wort.
 */
@Immutable
data class CeviColors(
    /** Links, Überzeilen und Icons in Detailzeilen. */
    val accent: Color,
    /** Rot als Text oder Linie; `primary` ist nur als Fläche gedacht. */
    val primaryText: Color,
    val onSecondaryMuted: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val ageGroupContainer: Color,
    val onAgeGroupContainer: Color,
    /** Fokusring: 3 dp, 2 dp Abstand. */
    val focus: Color,
)

val LocalCeviColors = staticCompositionLocalOf { CeviNight.extra }

/** Die zusätzlichen Rollen des aktuellen Themes: `MaterialTheme.cevi.accent`. */
val MaterialTheme.cevi: CeviColors
    @Composable @ReadOnlyComposable get() = LocalCeviColors.current

internal class Palette(val scheme: ColorScheme, val extra: CeviColors)

private val CeviRed = Color(0xFFC41333)
private val CeviBlue = Color(0xFF323394)
private val PureWhite = Color(0xFFFFFFFF)

/** «Nacht»: blau getöntes Dunkel, die Hauptrichtung. */
internal val CeviNight: Palette = run {
    val surface = Color(0xFF161620)
    val container = Color(0xFF1E1F38)
    val containerHigh = Color(0xFF23244A)
    Palette(
        scheme = darkColorScheme(
            primary = CeviRed,
            onPrimary = PureWhite,
            primaryContainer = CeviRed,
            onPrimaryContainer = PureWhite,
            inversePrimary = Color(0xFFFF7A8A),
            secondary = CeviBlue,
            onSecondary = PureWhite,
            secondaryContainer = containerHigh,
            onSecondaryContainer = Color(0xFFDCDDF9),
            tertiary = CeviBlue,
            onTertiary = PureWhite,
            background = surface,
            onBackground = Color(0xFFEDEDEB),
            surface = surface,
            onSurface = Color(0xFFEDEDEB),
            surfaceVariant = containerHigh,
            onSurfaceVariant = Color(0xFFABABA6),
            surfaceTint = Color.Transparent,
            inverseSurface = Color(0xFFEDEDEB),
            inverseOnSurface = surface,
            error = Color(0xFFFF8E9A),
            onError = Color(0xFF2E1519),
            errorContainer = Color(0xFF2E1519),
            onErrorContainer = Color(0xFFFF8E9A),
            outline = Color(0xFF8A8A90),
            outlineVariant = Color(0xFF2C2C44),
            scrim = Color.Black,
            surfaceBright = containerHigh,
            surfaceDim = surface,
            surfaceContainerLowest = surface,
            surfaceContainerLow = Color(0xFF17172A),
            surfaceContainer = container,
            surfaceContainerHigh = containerHigh,
            surfaceContainerHighest = containerHigh,
        ),
        extra = CeviColors(
            accent = Color(0xFFA9ABF2),
            primaryText = Color(0xFFFF7A8A),
            onSecondaryMuted = Color(0xFFDCDDF9),
            successContainer = Color(0xFF16271D),
            onSuccessContainer = Color(0xFF6FD39A),
            warningContainer = Color(0xFF2A2312),
            onWarningContainer = Color(0xFFF2C063),
            ageGroupContainer = Color(0xFF0F2A31),
            onAgeGroupContainer = Color(0xFF7FD0E3),
            focus = Color(0xFFA9ABF2),
        ),
    )
}

/** «Tag»: der Hellmodus. Cevi-Rot und Cevi-Blau bleiben gleich. */
internal val CeviDay: Palette = run {
    val surface = Color(0xFFF7F7FC)
    val container = Color(0xFFECECF8)
    val containerHigh = Color(0xFFDFDFF3)
    Palette(
        scheme = lightColorScheme(
            primary = CeviRed,
            onPrimary = PureWhite,
            primaryContainer = CeviRed,
            onPrimaryContainer = PureWhite,
            inversePrimary = Color(0xFFA30F2A),
            secondary = CeviBlue,
            onSecondary = PureWhite,
            secondaryContainer = container,
            onSecondaryContainer = CeviBlue,
            tertiary = CeviBlue,
            onTertiary = PureWhite,
            background = surface,
            onBackground = Color(0xFF141412),
            surface = surface,
            onSurface = Color(0xFF141412),
            surfaceVariant = containerHigh,
            onSurfaceVariant = Color(0xFF5C5C57),
            surfaceTint = Color.Transparent,
            inverseSurface = Color(0xFF141412),
            inverseOnSurface = surface,
            error = Color(0xFF9E1B2C),
            onError = PureWhite,
            errorContainer = Color(0xFFFCE9EC),
            onErrorContainer = Color(0xFF9E1B2C),
            outline = Color(0xFF7A7A74),
            outlineVariant = Color(0xFFE2E2EC),
            scrim = Color.Black,
            surfaceBright = PureWhite,
            surfaceDim = containerHigh,
            surfaceContainerLowest = PureWhite,
            surfaceContainerLow = PureWhite,
            surfaceContainer = container,
            surfaceContainerHigh = containerHigh,
            surfaceContainerHighest = containerHigh,
        ),
        extra = CeviColors(
            accent = CeviBlue,
            primaryText = Color(0xFFA30F2A),
            onSecondaryMuted = Color(0xFFDCDDF9),
            successContainer = Color(0xFFE7F4EC),
            onSuccessContainer = Color(0xFF17703A),
            warningContainer = Color(0xFFFFF4D6),
            onWarningContainer = Color(0xFF8A5A00),
            ageGroupContainer = Color(0xFFE3F2F6),
            onAgeGroupContainer = Color(0xFF1D6274),
            focus = CeviBlue,
        ),
    )
}
