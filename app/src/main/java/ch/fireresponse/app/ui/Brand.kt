package ch.fireresponse.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import ch.fireresponse.app.images.LocalRankImages
import ch.fireresponse.app.images.RankImages
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.font.FontWeight
import ch.fireresponse.app.profile.Badge
import ch.fireresponse.app.ui.theme.Pill
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

/**
 * Das Rang-Abzeichen einer Person als Bild: bei 0 bis 2 Sternen das mitgelieferte, sonst das Bild vom Server (SVG oder PNG, siehe
 * `RankImages`). Nur wenn es kein Bild gibt oder es nicht lädt, steht das Kürzel als kleiner Text da. So fehlt ein Rang nie und
 * wird nie falsch gezeigt.
 */
@Composable
fun RankMark(badge: Badge, modifier: Modifier = Modifier, height: Dp = 24.dp, detailed: Boolean = false) {
    val stars = badge.shownStars
    val images = LocalRankImages.current
    val url = badge.imageUrl
    if (stars != null) {
        RankBadge(stars, modifier, height, detailed)
    } else if (url != null && images != null) {
        RankImage(badge, url, images, modifier, height)
    } else {
        RankText(badge, modifier)
    }
}

/** Das Bild-Abzeichen vom Server. Es lädt kurz: Der Platz bleibt frei, damit nichts springt. Lädt es nicht, steht das Kürzel da. */
@Composable
private fun RankImage(badge: Badge, url: String, images: RankImages, modifier: Modifier, height: Dp) {
    var failed by remember(url) { mutableStateOf(false) }
    val bitmap by produceState<Bitmap?>(null, url) {
        value = images.get(url)
        failed = value == null
    }
    val shown = bitmap
    if (shown != null) {
        val description = if (badge.abbreviation.isBlank()) stringResource(R.string.rank_badge) else stringResource(R.string.rank_named, badge.abbreviation)
        Image(
            shown.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .height(height)
                .width(height * shown.width / shown.height)
                .semantics { contentDescription = description },
        )
    } else if (failed) {
        RankText(badge, modifier)
    } else {
        Spacer(modifier.height(height).width(height * 100f / 130f))
    }
}

/** Ersatz, wenn es kein Bild gibt oder es nicht lädt: das Kürzel als kleiner Text. */
@Composable
private fun RankText(badge: Badge, modifier: Modifier) {
    if (badge.abbreviation.isBlank()) return
    val description = stringResource(R.string.rank_named, badge.abbreviation)
    Text(
        badge.abbreviation,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, Pill)
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .semantics { contentDescription = description },
    )
}

/** Initialen in einem runden Feld, wie bei den Teilnehmenden: Token `avatar` mit `onAvatar`, 12 sp fett (Entwurf: 36 dp). */
@Composable
fun InitialsBadge(name: String, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Box(
        modifier.size(size).background(MaterialTheme.fire.avatar, CircleShape).clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials(name),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.fire.onAvatar,
        )
    }
}
