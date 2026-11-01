package io.github.sorry4nothing.cevios.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.sorry4nothing.cevios.R
import io.github.sorry4nothing.cevios.ui.theme.Spacing
import io.github.sorry4nothing.cevios.ui.theme.cevi

enum class NoticeKind(@DrawableRes val icon: Int) {
    /** Bleibt stehen und wird angesagt. */
    ERROR(R.drawable.ic_warning),
    INFO(R.drawable.ic_info),
    WARNING(R.drawable.ic_warning),
}

/** Banner im Inhalt: erklärt, warum etwas so ist, oder sagt, was schiefging. Immer Icon und Text. */
@Composable
fun NoticeBanner(kind: NoticeKind, text: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val (surfaceColor, contentColor) = when (kind) {
        NoticeKind.ERROR -> scheme.errorContainer to scheme.onErrorContainer
        NoticeKind.INFO -> scheme.secondaryContainer to scheme.onSecondaryContainer
        NoticeKind.WARNING -> MaterialTheme.cevi.warningContainer to MaterialTheme.cevi.onWarningContainer
    }
    Row(
        modifier
            .fillMaxWidth()
            .background(surfaceColor, MaterialTheme.shapes.medium)
            .padding(horizontal = Spacing.s4, vertical = Spacing.s3)
            .semantics { if (kind == NoticeKind.ERROR) liveRegion = LiveRegionMode.Assertive },
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(painterResource(kind.icon), contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = contentColor)
    }
}
