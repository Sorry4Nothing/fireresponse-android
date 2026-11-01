package io.github.sorry4nothing.cevios.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import io.github.sorry4nothing.cevios.R
import io.github.sorry4nothing.cevios.ui.theme.Spacing
import io.github.sorry4nothing.cevios.ui.theme.Pill
import io.github.sorry4nothing.cevios.ui.theme.cevi

/** Zustand eines Etiketts. Immer Icon und Wort: Ein Zustand steht nie nur als Farbe da. */
enum class ChipKind(@DrawableRes val icon: Int?) {
    YES(R.drawable.ic_check),
    NO(R.drawable.ic_cross),
    PENDING(R.drawable.ic_question),
    REVIEW(R.drawable.ic_warning),
    UNASSIGNED(R.drawable.ic_unlinked),
    AGE_GROUP(null),
}

/** Kleines Etikett für einen Zustand. Nicht bedienbar; bedienbar ist die Karte, auf der es sitzt. */
@Composable
fun StatusChip(kind: ChipKind, text: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.cevi
    val scheme = MaterialTheme.colorScheme
    val (surfaceColor, contentColor) = when (kind) {
        ChipKind.YES -> colors.successContainer to colors.onSuccessContainer
        ChipKind.NO -> scheme.errorContainer to scheme.onErrorContainer
        ChipKind.PENDING -> Color.Transparent to scheme.onSurfaceVariant
        ChipKind.REVIEW -> colors.warningContainer to colors.onWarningContainer
        ChipKind.UNASSIGNED -> scheme.secondaryContainer to scheme.onSecondaryContainer
        ChipKind.AGE_GROUP -> colors.ageGroupContainer to colors.onAgeGroupContainer
    }
    Row(
        modifier
            .heightIn(min = 28.dp)
            .background(surfaceColor, Pill)
            .then(if (kind == ChipKind.PENDING) Modifier.border(1.dp, scheme.outline, Pill) else Modifier)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
    ) {
        kind.icon?.let { Icon(painterResource(it), contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp)) }
        Text(text, style = MaterialTheme.typography.labelMedium, color = contentColor)
    }
}
