package ch.fireresponse.app.ui.theme

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
 * Rollen, die Material 3 nicht kennt (Design System «Fire-Response Android», `tokens.json`).
 * Kein Zustand nur über Farbe: Jede dieser Farben steht immer mit Icon und Wort.
 */
@Immutable
data class FireResponseColors(
    /** App-Akzent: Zurück-Pfeil, aktiver Dock-Punkt, Links, Überzeilen, Symbole in Listen. Nacht Warngelb, Tag Signalrot. */
    val accent: Color,
    /** Gelb als Text auf hellem Grund ist zu schwach: dort Ocker. Für gelb gesetzte Schrift, nicht für den App-Akzent. */
    val accentText: Color,
    /** Gelbe Flächen; sie tragen dunklen Text ([onAccentFill]). */
    val accentFill: Color,
    val onAccentFill: Color,
    /** Rot als Text oder Symbol (z. B. «Abmelden»); `primary` ist nur als Fläche gedacht. */
    val primaryText: Color,
    val onSecondaryMuted: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    /** Fokusring: 3 dp, 2 dp Abstand. */
    val focus: Color,
    /** Schalter eingeschaltet. */
    val switchOn: Color,
    /** Fläche des aktiven Dock-Punkts, mit [accent] als Icon und Wort. */
    val tabSelected: Color,
)

val LocalFireResponseColors = staticCompositionLocalOf { FireResponseNight.extra }

/** Die zusätzlichen Rollen des aktuellen Themes: `MaterialTheme.fire.accent`. */
val MaterialTheme.fire: FireResponseColors
    @Composable @ReadOnlyComposable get() = LocalFireResponseColors.current

internal class Palette(val scheme: ColorScheme, val extra: FireResponseColors)

private val SignalRed = Color(0xFFC8141A)
private val PureWhite = Color(0xFFFFFFFF)

/** «Nacht»: warmes Graphit, die Hauptrichtung. Akzent Warngelb. */
internal val FireResponseNight: Palette = run {
    val surface = Color(0xFF0F1114)
    val containerLow = Color(0xFF181B20)
    val container = Color(0xFF22262D)
    val containerHigh = Color(0xFF2B3039)
    Palette(
        scheme = darkColorScheme(
            primary = SignalRed,
            onPrimary = PureWhite,
            primaryContainer = SignalRed,
            onPrimaryContainer = PureWhite,
            inversePrimary = Color(0xFFFF8A82),
            secondary = Color(0xFF465060),
            onSecondary = PureWhite,
            secondaryContainer = containerHigh,
            onSecondaryContainer = Color(0xFFE3E6EB),
            tertiary = Color(0xFF465060),
            onTertiary = PureWhite,
            background = surface,
            onBackground = Color(0xFFF2F3F5),
            surface = surface,
            onSurface = Color(0xFFF2F3F5),
            surfaceVariant = containerHigh,
            onSurfaceVariant = Color(0xFF9BA0AA),
            surfaceTint = Color.Transparent,
            inverseSurface = Color(0xFFF2F3F5),
            inverseOnSurface = surface,
            error = Color(0xFFFF8A86),
            onError = Color(0xFF3A1214),
            errorContainer = Color(0xFF3A1214),
            onErrorContainer = Color(0xFFFF8A86),
            outline = Color(0xFF7D8493),
            outlineVariant = Color(0xFF323741),
            scrim = Color.Black,
            surfaceBright = containerHigh,
            surfaceDim = surface,
            surfaceContainerLowest = surface,
            surfaceContainerLow = containerLow,
            surfaceContainer = container,
            surfaceContainerHigh = containerHigh,
            surfaceContainerHighest = containerHigh,
        ),
        extra = FireResponseColors(
            accent = Color(0xFFFFD21F),
            accentText = Color(0xFFFFD21F),
            accentFill = Color(0xFFFFC400),
            onAccentFill = Color(0xFF1A1B1E),
            primaryText = Color(0xFFFF8A82),
            onSecondaryMuted = Color(0xFFDADEE5),
            successContainer = Color(0xFF10301F),
            onSuccessContainer = Color(0xFF34D17A),
            warningContainer = Color(0xFF3A3000),
            onWarningContainer = Color(0xFFFFD21F),
            focus = Color(0xFFFFD21F),
            switchOn = Color(0xFFF2B800),
            tabSelected = Color(0x29FFD21F),
        ),
    )
}

/** «Tag»: der Hellmodus, warmes Grau. Akzent Signalrot. */
internal val FireResponseDay: Palette = run {
    val surface = Color(0xFFF5F3F0)
    val container = Color(0xFFECE8E3)
    val containerHigh = Color(0xFFE2DDD6)
    Palette(
        scheme = lightColorScheme(
            primary = SignalRed,
            onPrimary = PureWhite,
            primaryContainer = SignalRed,
            onPrimaryContainer = PureWhite,
            inversePrimary = Color(0xFFB30F15),
            secondary = Color(0xFF3A3F49),
            onSecondary = PureWhite,
            secondaryContainer = container,
            onSecondaryContainer = Color(0xFF1A1B1E),
            tertiary = Color(0xFF3A3F49),
            onTertiary = PureWhite,
            background = surface,
            onBackground = Color(0xFF1A1B1E),
            surface = surface,
            onSurface = Color(0xFF1A1B1E),
            surfaceVariant = containerHigh,
            onSurfaceVariant = Color(0xFF5E6066),
            surfaceTint = Color.Transparent,
            inverseSurface = Color(0xFF1A1B1E),
            inverseOnSurface = surface,
            error = Color(0xFFA60F14),
            onError = PureWhite,
            errorContainer = Color(0xFFFDE7E8),
            onErrorContainer = Color(0xFFA60F14),
            outline = Color(0xFF7D7870),
            outlineVariant = Color(0xFFD9D3CC),
            scrim = Color.Black,
            surfaceBright = PureWhite,
            surfaceDim = containerHigh,
            surfaceContainerLowest = PureWhite,
            surfaceContainerLow = PureWhite,
            surfaceContainer = container,
            surfaceContainerHigh = containerHigh,
            surfaceContainerHighest = containerHigh,
        ),
        extra = FireResponseColors(
            accent = Color(0xFFB30F15),
            accentText = Color(0xFF7A5A00),
            accentFill = Color(0xFFFFC400),
            onAccentFill = Color(0xFF1A1B1E),
            primaryText = Color(0xFFB30F15),
            onSecondaryMuted = Color(0xFFDADEE5),
            successContainer = Color(0xFFE3F4EA),
            onSuccessContainer = Color(0xFF136B3A),
            warningContainer = Color(0xFFFFF1BF),
            onWarningContainer = Color(0xFF5C4400),
            focus = Color(0xFFB30F15),
            switchOn = Color(0xFFB30F15),
            tabSelected = Color(0x1AB30F15),
        ),
    )
}
