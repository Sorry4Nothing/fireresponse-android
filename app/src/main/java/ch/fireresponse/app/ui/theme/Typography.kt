package ch.fireresponse.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ch.fireresponse.app.R

/**
 * Barlow Condensed für Titel und Inter für Text, beide mitgeliefert (Open Font License, siehe `assets/licenses`). Es wird nichts
 * nachgeladen. Grössen in sp: Die Schriftgrösse des Systems greift bis 200 %.
 */
val BarlowCondensed = FontFamily(Font(R.font.barlow_condensed_bold, weight = FontWeight.Bold))

/** Inter ist eine variable Schrift; der Entwurf nutzt die Gewichte 400 bis 800. */
val Inter = FontFamily(
    listOf(400, 500, 600, 700, 800).map { weight ->
        Font(
            R.font.inter,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
        )
    },
)

private fun title(size: Int, lineHeight: Int, spacing: Float = 0.005f) = TextStyle(
    fontFamily = BarlowCondensed,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = spacing.em,
)

private fun text(size: Int, lineHeight: Int, weight: Int, spacing: Float = 0f) = TextStyle(
    fontFamily = Inter,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = FontWeight(weight),
    letterSpacing = spacing.em,
)

/** Die Stile aus dem Design (`tokens.json`, `type`), den Material-Rollen zugeordnet. */
internal val FireResponseTypography = Typography(
    displayLarge = title(40, 44, -0.01f), // display: Begrüssung auf Login und Willkommen (die Bildschirme setzen 40/44)
    displayMedium = title(40, 44, -0.01f),
    displaySmall = title(34, 36),
    headlineLarge = title(34, 36), // headline: Screen-Titel im Kopf
    headlineMedium = title(28, 32), // headline-small: Titel auf Detail-Screens
    headlineSmall = title(28, 32),
    titleLarge = title(22, 28), // title-large: Dialog-Titel
    titleMedium = text(16, 22, 700), // title: Kartentitel, Namen, Abschnittstitel
    titleSmall = text(16, 22, 700),
    bodyLarge = text(16, 22, 400), // body
    bodyMedium = text(14, 20, 400), // body-small: Metadaten
    bodySmall = text(12, 16, 400), // caption
    labelLarge = text(15, 20, 700), // label: Buttons und Pillen
    labelMedium = text(12, 16, 700), // label-small: Footer, Chips, Zähler
    labelSmall = text(12, 16, 700),
)

/** Überzeile über Titeln: in Grossbuchstaben setzen und in `accent` färben. */
val Eyebrow: TextStyle = text(13, 16, 700, 0.08f)
