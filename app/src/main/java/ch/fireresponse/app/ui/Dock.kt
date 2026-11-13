package ch.fireresponse.app.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import ch.fireresponse.app.R
import ch.fireresponse.app.ui.theme.Spacing
import ch.fireresponse.app.ui.theme.WideBreakpoint
import ch.fireresponse.app.ui.theme.fire
import ch.fireresponse.app.ui.theme.Sizes

/** Die Hauptziele des Docks. */
enum class Destination(@StringRes val text: Int, @DrawableRes val icon: Int) {
    APPOINTMENTS(R.string.tab_appointments, R.drawable.ic_calendar),

    /** Der Infokanal; der Reiter erscheint nur, wenn die Person mindestens einen hat. */
    INFO(R.string.tab_info, R.drawable.ic_chat),
    PROFILE(R.string.tab_profile, R.drawable.ic_profile),
}

private val DockShape = RoundedCornerShape(24.dp)
private val DestinationShape = RoundedCornerShape(18.dp)

/**
 * Gerüst mit schwebendem Dock: unten im Hochformat, ab [WideBreakpoint] senkrecht links (Querformat, Tablet).
 * Das Dock sitzt unter dem Inhalt, nichts scrollt dahinter durch.
 */
@Composable
fun MainScaffold(
    active: Destination,
    onDestination: (Destination) -> Unit,
    destinations: List<Destination> = Destination.entries.filter { it != Destination.INFO },
    /** Zahl am Reiter, zum Beispiel ungelesene Mitteilungen. */
    badges: Map<Destination, Int> = emptyMap(),
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        if (maxWidth >= WideBreakpoint) {
            Row(Modifier.fillMaxSize().padding(start = Spacing.s3)) {
                Dock(active, onDestination, destinations, badges, vertical = true, Modifier.padding(vertical = Spacing.s3).widthIn(min = Sizes.dockWidth).width(IntrinsicSize.Max))
                Spacer(Modifier.width(Spacing.s4))
                content()
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f)) { content() }
                Dock(
                    active, onDestination, destinations, badges, vertical = false,
                    Modifier.padding(start = Spacing.s3, end = Spacing.s3, top = Spacing.s2, bottom = Spacing.s3),
                )
            }
        }
    }
}

@Composable
private fun Dock(
    active: Destination,
    onDestination: (Destination) -> Unit,
    destinations: List<Destination>,
    badges: Map<Destination, Int>,
    vertical: Boolean,
    modifier: Modifier,
) {
    val shell = modifier
        .clip(DockShape)
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, DockShape)
        .padding(6.dp)
        .selectableGroup()
    if (vertical) {
        Column(shell, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            destinations.forEach { DockDestination(it, it == active, onDestination, Modifier.fillMaxWidth().heightIn(min = 64.dp), badges[it] ?: 0) }
        }
    } else {
        Row(shell.heightIn(min = Sizes.footer).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            destinations.forEach { DockDestination(it, it == active, onDestination, Modifier.weight(1f).fillMaxHeight(), badges[it] ?: 0) }
        }
    }
}

/** Ein Ziel über die ganze Fläche (mindestens 48 dp): aktiv mit Fläche `tab-selected` und `accent` für Icon und Wort, inaktiv ruhig. Das Wort steht immer da. */
@Composable
private fun DockDestination(destination: Destination, active: Boolean, onDestination: (Destination) -> Unit, modifier: Modifier, badge: Int = 0) {
    val badgeText = stringResource(R.string.info_tab_unread, badge)
    val color = if (active) MaterialTheme.fire.accent else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier
            .focusRing(DestinationShape)
            .clip(DestinationShape)
            .then(if (active) Modifier.background(MaterialTheme.fire.tabSelected) else Modifier)
            .selectable(selected = active, role = Role.Tab, onClick = { onDestination(destination) })
            .semantics { if (badge > 0) stateDescription = badgeText },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Box {
            Icon(painterResource(destination.icon), contentDescription = null, tint = color, modifier = Modifier.padding(0.dp))
            if (badge > 0) UnreadBadge(badge, Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-6).dp).size(18.dp))
        }
        Text(
            stringResource(destination.text),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = color,
            textAlign = TextAlign.Center,
            // Drei Reiter teilen die Breite: Bei grösster Schrift schrumpft das Wort bis 8 sp (immer noch deutlich grösser als
            // bei Standardschrift), statt mitten im Wort umzubrechen.
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = MaterialTheme.typography.labelMedium.fontSize),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
