package io.github.sorry4nothing.cevios.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Abstände aus dem Design (4-dp-Raster). */
object Spacing {
    val s1 = 4.dp // Icon und Text in Chips
    val s2 = 8.dp // zwischen Kacheln und Pillen
    val s3 = 12.dp // zwischen Karten in Listen, Footer-Rand
    val s4 = 16.dp // Karten-Innenabstand
    val s5 = 20.dp // Seitenrand im Hochformat
    val s6 = 24.dp // zwischen Abschnitten, Dialog-Innenabstand
    val s8 = 32.dp // über grossen Titeln
}

/** Feste Grössen aus dem Design. Jedes bedienbare Element ist mindestens `touchTarget` gross. */
object Sizes {
    val touchTarget = 48.dp
    val icon = 24.dp
    val footer = 68.dp
    val button = 56.dp // Haupt-Buttons
    val textField = 52.dp // Textfelder
    val contentMaxWidth = 640.dp // maximale Textbreite im Querformat
    val dockWidth = 96.dp // Dock links ab 600 dp
}

/** Ab dieser Breite steht das Dock links und Liste und Detail stehen nebeneinander. */
val WideBreakpoint = 600.dp

/** Ecken: Felder `sm`, Karten `md`, Chat-Blasen `lg`, Dock, Login und Dialoge `xl`; Buttons, Pillen und Avatare [Pill]. */
internal val CeviShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

val Pill = CircleShape
