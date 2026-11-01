package io.github.sorry4nothing.cevios.programs

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sorry4nothing.cevios.R
import io.github.sorry4nothing.cevios.net.ApiResult
import io.github.sorry4nothing.cevios.ui.Avatar
import io.github.sorry4nothing.cevios.ui.ChipKind
import io.github.sorry4nothing.cevios.ui.ErrorState
import io.github.sorry4nothing.cevios.ui.Header
import io.github.sorry4nothing.cevios.ui.HeaderButton
import io.github.sorry4nothing.cevios.ui.LoadingSkeleton
import io.github.sorry4nothing.cevios.ui.NoticeBanner
import io.github.sorry4nothing.cevios.ui.NoticeKind
import io.github.sorry4nothing.cevios.ui.StatusChip
import io.github.sorry4nothing.cevios.ui.focusRing
import io.github.sorry4nothing.cevios.ui.theme.Pill
import io.github.sorry4nothing.cevios.ui.theme.Sizes
import io.github.sorry4nothing.cevios.ui.theme.Spacing
import io.github.sorry4nothing.cevios.ui.theme.cevi

/**
 * Wer zu einem Programm ja, nein oder noch nichts gesagt hat, für Leitende, Stufenleitungen und Admins. Nur zum Ansehen: Eine
 * fehlende Antwort bleibt offen, niemand ändert sie hier (Entscheid des Auftraggebers). `eyebrow` nennt Programm und Tag.
 */
@Composable
fun RegistrationsScreen(
    eyebrow: String?,
    state: ApiResult<Registrations>?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Header(
            title = stringResource(R.string.registrations_title),
            eyebrow = eyebrow,
            back = HeaderButton(R.drawable.ic_back, stringResource(R.string.back_to_program), onBack),
            focusOnTitle = true,
        )
        when (state) {
            null -> LoadingSkeleton(stringResource(R.string.registrations_loading))
            is ApiResult.Failure -> ErrorState(state.error, onRetry, onToPrograms = onBack)
            is ApiResult.Ok -> RegistrationsContent(state.value)
        }
    }
}

private enum class Tab { CHILDREN, LEADERS }

@Composable
private fun RegistrationsContent(data: Registrations) {
    var tab by rememberSaveable { mutableIntStateOf(Tab.CHILDREN.ordinal) }
    val selected = Tab.entries[tab]
    val counts = if (selected == Tab.CHILDREN) data.counts.children else data.counts.leaders
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
        Summary(counts, Modifier.padding(horizontal = 28.dp))
        Row(Modifier.padding(horizontal = Spacing.s5).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            TabPill(stringResource(R.string.registrations_tab_children, data.counts.children.total), selected == Tab.CHILDREN) { tab = Tab.CHILDREN.ordinal }
            TabPill(stringResource(R.string.registrations_tab_leaders, data.counts.leaders.total), selected == Tab.LEADERS) { tab = Tab.LEADERS.ordinal }
        }
        LazyColumn(
            contentPadding = PaddingValues(start = Spacing.s4, end = Spacing.s4, bottom = Spacing.s4),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (selected == Tab.CHILDREN) {
                if (data.children.isEmpty()) item { NoticeBanner(NoticeKind.INFO, stringResource(R.string.registrations_empty_children)) }
                items(data.children, key = { it.id }) { PersonRow(it.name, null, null, it.status) }
            } else {
                if (data.leaders.isEmpty()) item { NoticeBanner(NoticeKind.INFO, stringResource(R.string.registrations_empty_leaders)) }
                items(data.leaders, key = { it.id }) {
                    val role = stringResource(if (it.isGroupLeader) R.string.role_group_leader else R.string.role_leader)
                    PersonRow(it.title, listOfNotNull(it.fullName, role).joinToString(" · "), it.imageUrl, it.status)
                }
            }
        }
    }
}

/** Der Balken mit den Anteilen und die drei Zahlen. Der Screenreader hört ihn als einen Satz, nicht Segment für Segment. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Summary(counts: Counts, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val yes = MaterialTheme.cevi.onSuccessContainer
    val no = scheme.onErrorContainer
    val open = scheme.onSurfaceVariant
    val description = stringResource(R.string.registrations_summary, counts.ja, counts.nein, counts.offen, counts.total)
    Column(modifier.clearAndSetSemantics { contentDescription = description }, verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Row(Modifier.fillMaxWidth().height(14.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            if (counts.total == 0) {
                Box(Modifier.weight(1f).fillMaxSize().clip(Pill).background(scheme.surfaceContainerHigh))
            } else {
                Segment(counts.ja, counts.total, yes)
                Segment(counts.nein, counts.total, no)
                Segment(counts.offen, counts.total, scheme.surfaceContainerHigh)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s4), verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
            Count(R.drawable.ic_check, stringResource(R.string.registrations_yes, counts.ja), yes)
            Count(R.drawable.ic_cross, stringResource(R.string.registrations_no, counts.nein), no)
            Count(R.drawable.ic_question, stringResource(R.string.registrations_open, counts.offen), open)
        }
    }
}

/** Ein Abschnitt des Balkens; Null zählt nicht (ein Gewicht von 0 wäre ungültig). */
@Composable
private fun RowScope.Segment(count: Int, total: Int, color: Color) {
    val share = RegistrationsRules.share(count, total)
    if (share > 0f) Box(Modifier.weight(share).fillMaxSize().clip(Pill).background(color))
}

@Composable
private fun Count(@DrawableRes icon: Int, text: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s1), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.Bold)
    }
}

/** Reiter als Pille (mindestens 48 dp hoch): gewählt hell gefüllt, sonst mit Rand. Rolle «Reiter», Auswahl wird angesagt. */
@Composable
internal fun TabPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .heightIn(min = Sizes.touchTarget)
            .focusRing(Pill)
            .clip(Pill)
            .then(
                if (selected) Modifier.background(scheme.onSecondaryContainer)
                else Modifier.border(1.dp, scheme.outline, Pill),
            )
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (selected) scheme.secondaryContainer else scheme.onSurfaceVariant,
        )
    }
}

/** Eine Person: Profilbild oder Initiale, Name (bei Leitenden mit vollem Namen und Rolle darunter) und der Stand als Chip. Name und Stand lesen sich als ein Element. */
@Composable
private fun PersonRow(name: String, subtitle: String?, imageUrl: String?, status: String) {
    val scheme = MaterialTheme.colorScheme
    val kind = registrationChip(status)
    val word = stringResource(
        when (kind) {
            ChipKind.YES -> R.string.button_yes
            ChipKind.NO -> R.string.button_no
            else -> R.string.registrations_chip_open
        },
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLow, MaterialTheme.shapes.medium)
            .border(1.dp, scheme.outlineVariant, MaterialTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(name, size = 36.dp, imageUrl = imageUrl, initialSize = 13.sp)
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant) }
        }
        StatusChip(kind, word)
    }
}
