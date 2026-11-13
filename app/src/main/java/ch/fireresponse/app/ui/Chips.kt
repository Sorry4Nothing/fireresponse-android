package ch.fireresponse.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ch.fireresponse.app.R
import ch.fireresponse.app.ui.theme.Spacing
import ch.fireresponse.app.ui.theme.Pill
import ch.fireresponse.app.ui.theme.fire

/** Zustand eines Etiketts. Immer Icon und Wort: Ein Zustand steht nie nur als Farbe da. */
enum class ChipKind(@DrawableRes val icon: Int) {
    YES(R.drawable.ic_check),
    NO(R.drawable.ic_cross),
    PENDING(R.drawable.ic_question),
    REVIEW(R.drawable.ic_warning),

    /** Auf der Warteliste (Leiter-Plätze). */
    WAITLIST(R.drawable.ic_hourglass),

    /** Alle Leiter-Plätze belegt. */
    FULL(R.drawable.ic_lock),

    /** Es sind noch Leiter-Plätze frei. */
    FREE(R.drawable.ic_group),
}

/** Kleines Etikett für einen Zustand. Nicht bedienbar; bedienbar ist die Karte, auf der es sitzt. */
@Composable
fun StatusChip(kind: ChipKind, text: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.fire
    val scheme = MaterialTheme.colorScheme
    val (surfaceColor, contentColor) = when (kind) {
        ChipKind.YES -> colors.successContainer to colors.onSuccessContainer
        ChipKind.NO -> scheme.errorContainer to scheme.onErrorContainer
        ChipKind.PENDING -> Color.Transparent to scheme.onSurfaceVariant
        ChipKind.REVIEW -> colors.warningContainer to colors.onWarningContainer
        ChipKind.WAITLIST -> Color.Transparent to scheme.onSurfaceVariant
        ChipKind.FULL -> scheme.surfaceContainerHighest to scheme.onSurface
        ChipKind.FREE -> scheme.surfaceContainerHigh to scheme.onSurfaceVariant
    }
    Row(
        modifier
            .heightIn(min = 28.dp)
            .background(surfaceColor, Pill)
            .then(if (kind == ChipKind.PENDING || kind == ChipKind.WAITLIST) Modifier.border(1.dp, scheme.outline, Pill) else Modifier)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
    ) {
        kind.icon.let { Icon(painterResource(it), contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp)) }
        Text(text, style = MaterialTheme.typography.labelMedium, color = contentColor)
    }
}

/** Der Zähler für Ungelesenes: Zahl im Kreis in `accent`. Der Name der Zahl steht am Element, auf dem er sitzt. */
@Composable
fun UnreadBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier.size(24.dp).background(MaterialTheme.fire.accent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (count > 99) "99+" else count.toString(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.surface,
        )
    }
}
