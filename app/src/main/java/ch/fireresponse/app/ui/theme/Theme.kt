package ch.fireresponse.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Das Aussehen «Nachthimmel» aus dem Design:
 * - Design System «Cevi App» (Farben, Schrift, Bausteine)
 * - Bildschirme «Cevi App Screens» (Nacht und Tag, alle Zustände)
 *
 * Zwei Themen, [FireResponseNight] (Dunkelmodus, Hauptrichtung) und [FireResponseDay] (Hellmodus). Die App folgt dem System, es gibt
 * kein Dynamic Color. Die Zuordnung Baustein → Compose steht in `docs/design.md`.
 */
@Composable
fun FireResponseTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val palette = if (dark) FireResponseNight else FireResponseDay
    CompositionLocalProvider(LocalFireResponseColors provides palette.extra) {
        MaterialTheme(
            colorScheme = palette.scheme,
            typography = FireResponseTypography,
            shapes = FireResponseShapes,
            content = content,
        )
    }
}
