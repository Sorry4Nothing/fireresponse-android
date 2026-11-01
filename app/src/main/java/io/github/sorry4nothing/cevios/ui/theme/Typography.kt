package io.github.sorry4nothing.cevios.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.sorry4nothing.cevios.R

/**
 * Archivo, mitgeliefert (variable Schrift, Open Font License, siehe `assets/licenses`). Es wird nichts nachgeladen.
 * Grössen in sp: Die Schriftgrösse des Systems greift bis 200 %.
 */
val Archivo = FontFamily(
    listOf(400, 700, 800).map { weight ->
        Font(
            R.font.archivo,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
        )
    },
)

private fun textStyle(size: Int, lineHeight: Int, weight: Int, spacing: Float = 0f) = TextStyle(
    fontFamily = Archivo,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = FontWeight(weight),
    letterSpacing = spacing.em,
)

/** Die Stile aus dem Design (`tokens.json`, `type`), den Material-Rollen zugeordnet. */
internal val CeviTypography = Typography(
    displayLarge = textStyle(40, 44, 800, -0.01f), // display: Begrüssung auf dem Login
    displayMedium = textStyle(40, 44, 800, -0.01f),
    displaySmall = textStyle(32, 38, 800),
    headlineLarge = textStyle(32, 38, 800), // headline: Screen-Titel im Kopf
    headlineMedium = textStyle(28, 34, 800), // headline-small: Titel auf Detail-Screens
    headlineSmall = textStyle(28, 34, 800),
    titleLarge = textStyle(22, 28, 800), // title-large: Dialog-Titel
    titleMedium = textStyle(16, 22, 700), // title: Kartentitel, Namen, Abschnittstitel
    titleSmall = textStyle(16, 22, 700),
    bodyLarge = textStyle(16, 22, 400), // body
    bodyMedium = textStyle(14, 20, 400), // body-small: Metadaten
    bodySmall = textStyle(12, 16, 400), // caption
    labelLarge = textStyle(15, 20, 700), // label: Buttons und Pillen
    labelMedium = textStyle(12, 16, 700), // label-small: Footer, Chips, Zähler
    labelSmall = textStyle(12, 16, 700),
)

/** Überzeile über Titeln: in Grossbuchstaben setzen und in `accent` färben. */
val Eyebrow: TextStyle = textStyle(13, 16, 700, 0.08f)
