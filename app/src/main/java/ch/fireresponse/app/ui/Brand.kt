package ch.fireresponse.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.fireresponse.app.R
import ch.fireresponse.app.profile.initials
import ch.fireresponse.app.ui.theme.Spacing
import ch.fireresponse.app.ui.theme.fire

/** Wappen des Kantons Zürich in der Grösse über der Überzeile (48 × 56 dp). Dekoration: Der Screenreader überspringt es. */
@Composable
fun Crest(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.ic_crest), contentDescription = null, modifier = modifier.size(48.dp, 56.dp))
}

/** Fusszeile von Login und Willkommen: kleines Wappen (15 × 18 dp) mit «Kanton Zürich». */
@Composable
fun CantonFooter(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_crest), contentDescription = null, modifier = Modifier.size(15.dp, 18.dp))
        Text(stringResource(R.string.canton), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Das Abzeichen eines Rangs, nur als Bild: kein Rangname, keine Nummer. Seitenverhältnis 100:130, nie verzerrt, nie umgefärbt.
 * `stars` (0 bis 2) wählt das Bild. `detailed` hängt «ein Stern» oder «zwei Sterne» an die Beschreibung (Profil und Willkommen).
 */
@Composable
fun RankBadge(stars: Int, modifier: Modifier = Modifier, height: Dp = 34.dp, detailed: Boolean = false) {
    val drawable = when (stars.coerceIn(0, 2)) {
        0 -> R.drawable.ic_rank_0
        1 -> R.drawable.ic_rank_1
        else -> R.drawable.ic_rank_2
    }
    val label = stringResource(R.string.rank_badge)
    val detail = if (!detailed) null else when (stars.coerceIn(0, 2)) {
        1 -> stringResource(R.string.rank_one_star)
        2 -> stringResource(R.string.rank_two_stars)
        else -> null
    }
    Image(
        painterResource(drawable),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .height(height)
            .width(height * 100f / 130f)
            .semantics { contentDescription = if (detail == null) label else "$label, $detail" },
    )
}

/** Initialen in einem runden Feld, wie bei den Teilnehmenden: `surface-container` mit `accent`, 12 sp fett (Entwurf: 36 dp). */
@Composable
fun InitialsBadge(name: String, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Box(
        modifier.size(size).background(MaterialTheme.colorScheme.surfaceContainer, CircleShape).clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials(name),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.fire.accent,
        )
    }
}
