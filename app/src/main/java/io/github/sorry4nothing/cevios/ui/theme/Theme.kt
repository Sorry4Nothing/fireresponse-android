package io.github.sorry4nothing.cevios.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Das Aussehen «Nachthimmel» aus dem Design:
 * - Design System «Cevi App» (Farben, Schrift, Bausteine)
 * - Bildschirme «Cevi App Screens» (Nacht und Tag, alle Zustände)
 *
 * Zwei Themen, [CeviNight] (Dunkelmodus, Hauptrichtung) und [CeviDay] (Hellmodus). Die App folgt dem System, es gibt
 * kein Dynamic Color. Die Zuordnung Baustein → Compose steht in `docs/design.md`.
 */
@Composable
fun CeviTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val palette = if (dark) CeviNight else CeviDay
    CompositionLocalProvider(LocalCeviColors provides palette.extra) {
        MaterialTheme(
            colorScheme = palette.scheme,
            typography = CeviTypography,
            shapes = CeviShapes,
            content = content,
        )
    }
}
