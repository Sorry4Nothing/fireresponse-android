package ch.fireresponse.app.infochannel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.fireresponse.app.R
import ch.fireresponse.app.appointments.SWISS_ZONE
import ch.fireresponse.app.appointments.spokenDay
import ch.fireresponse.app.net.ApiResult
import ch.fireresponse.app.ui.Avatar
import ch.fireresponse.app.ui.DangerButton
import ch.fireresponse.app.ui.DialogText
import ch.fireresponse.app.ui.EmptyState
import ch.fireresponse.app.ui.ErrorState
import ch.fireresponse.app.ui.FireResponseDialog
import ch.fireresponse.app.ui.Header
import ch.fireresponse.app.ui.HeaderButton
import ch.fireresponse.app.ui.LoadingSkeleton
import ch.fireresponse.app.ui.NoticeBanner
import ch.fireresponse.app.ui.NoticeKind
import ch.fireresponse.app.ui.SquadMark
import ch.fireresponse.app.ui.SquadWatermark
import ch.fireresponse.app.ui.TextActionButton
import ch.fireresponse.app.ui.UnreadBadge
import ch.fireresponse.app.ui.focusRing
import ch.fireresponse.app.ui.theme.Spacing
import ch.fireresponse.app.ui.theme.fire
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.input.KeyboardCapitalization
import ch.fireresponse.app.net.ApiError
import ch.fireresponse.app.ui.Spinner
import ch.fireresponse.app.ui.theme.Sizes
import java.time.LocalDate

/** Die Liste der Infokanäle, wenn die Person mehrere hat: eine Zeile je Einheit mit Zähler und Zeit der letzten Mitteilung. */
@Composable
fun InfoChannelListScreen(
    state: ApiResult<List<InfoChannel>>?,
    onOpen: (String) -> Unit,
    onRetry: () -> Unit,
    refreshing: Boolean = false,
    refreshError: ApiError? = null,
    onRefresh: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize()) {
        Header(title = stringResource(R.string.tab_info))
        when (state) {
            null -> LoadingSkeleton(stringResource(R.string.info_loading))
            is ApiResult.Failure -> ErrorState(state.error, onRetry)
            is ApiResult.Ok -> PullToRefreshBox(refreshing, onRefresh, pullAction(onRefresh)) {
                Column {
                    RefreshError(refreshError)
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.s4, vertical = Spacing.s2),
                        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
                    ) {
                        items(state.value, key = { it.unit.id }) { ChannelRow(it, onOpen) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(channel: InfoChannel, onOpen: (String) -> Unit) {
    val unread = stringResource(R.string.info_unread, channel.unread)
    val last = channel.lastAt?.let { clockOrDay(it) }
    val spoken = listOfNotNull(channel.name, unread.takeIf { channel.unread > 0 }, last?.let { stringResource(R.string.info_last, it) }).joinToString(", ")
    Row(
        Modifier
            .fillMaxWidth()
            .focusRing(MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(role = Role.Button) { onOpen(channel.unit.id) }
            .padding(horizontal = Spacing.s4, vertical = 14.dp)
            .semantics(mergeDescendants = true) { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SquadMark(channel.unit.id, channel.unit.type, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(channel.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            last?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (channel.unread > 0) UnreadBadge(channel.unread)
    }
}

/** «14:30» heute, sonst der Tag («Samstag, 14. November»); für die Zeit der letzten Mitteilung. */
@Composable
private fun clockOrDay(iso: String): String? {
    val time = messageTime(iso) ?: return null
    return if (time.toLocalDate() == LocalDate.now(SWISS_ZONE)) clockText(iso) else spokenDay(time.toLocalDate())
}

/**
 * Der Verlauf einer Einheit (neueste Mitteilung unten) mit der Eingabe darunter, wenn die Person schreiben darf. `onBack` fehlt, wenn
 * es nur einen Infokanal gibt (der Reiter öffnet ihn direkt).
 */
@Composable
fun InfoChannelScreen(
    channel: InfoChannel?,
    history: History?,
    action: ChannelAction,
    onBack: (() -> Unit)?,
    onSend: (String, () -> Unit) -> Unit,
    onEdit: (String, String, () -> Unit) -> Unit,
    onDelete: (String) -> Unit,
    onLoadOlder: () -> Unit,
    onRetry: () -> Unit,
    onClearError: () -> Unit,
    refreshing: Boolean = false,
    refreshError: ApiError? = null,
    onRefresh: () -> Unit = {},
) {
    var editing by remember { mutableStateOf<InfoMessage?>(null) }
    var deleting by remember { mutableStateOf<InfoMessage?>(null) }
    deleting?.let { message ->
        FireResponseDialog(
            title = stringResource(R.string.info_delete_title),
            onDismiss = { deleting = null },
            dismissButton = { TextActionButton(stringResource(R.string.cancel), { deleting = null }) },
            confirmButton = { DangerButton(stringResource(R.string.info_delete_confirm), { deleting = null; onDelete(message.id) }) },
        ) { DialogText(stringResource(R.string.info_delete_text)) }
    }
    Column(Modifier.fillMaxSize()) {
        Header(
            title = channel?.name ?: stringResource(R.string.tab_info),
            back = onBack?.let { HeaderButton(R.drawable.ic_back, stringResource(R.string.info_back), it) },
        )
        when {
            history == null || history.loading -> LoadingSkeleton(stringResource(R.string.info_loading))
            history.error != null -> ErrorState(history.error, onRetry)
            // Ziehen lädt neu, auch im leeren Verlauf: Es gibt keinen Aktualisieren-Knopf.
            history.messages.isEmpty() -> PullToRefreshBox(refreshing, onRefresh, Modifier.weight(1f).then(pullAction(onRefresh))) {
                SquadWatermark(history.unitId, Modifier.align(Alignment.Center))
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        EmptyState(
                            R.drawable.ic_chat,
                            stringResource(R.string.info_empty_title),
                            stringResource(if (history.canWrite) R.string.info_empty_writer else R.string.info_empty_reader),
                            onRefresh = null,
                            Modifier.fillParentMaxSize(),
                        )
                    }
                }
            }
            else -> PullToRefreshBox(refreshing, onRefresh, Modifier.weight(1f).then(pullAction(onRefresh))) {
                SquadWatermark(history.unitId, Modifier.align(Alignment.Center))
                Column {
                    RefreshError(refreshError)
                    val rows = remember(history.messages) { historyRows(history.messages) }
                    LazyColumn(
                        Modifier.weight(1f).fillMaxWidth(),
                        reverseLayout = true,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.s4, vertical = Spacing.s2),
                        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
                    ) {
                        items(rows, key = { if (it is HistoryRow.Message) it.message.id else "tag-${(it as HistoryRow.Day).date}" }) { row ->
                            when (row) {
                                is HistoryRow.Day -> DaySeparator(row.date)
                                is HistoryRow.Message -> MessageRow(row.message, onEdit = { editing = it }, onDelete = { deleting = it })
                            }
                        }
                        if (history.next != null) {
                            // Ältere Mitteilungen laden von selbst, sobald das Ende der geladenen erreicht ist.
                            item(key = "aelter") {
                                LaunchedEffect(history.messages.size) { onLoadOlder() }
                                if (history.loadingMore) {
                                    Box(Modifier.fillMaxWidth().padding(vertical = Spacing.s2), contentAlignment = Alignment.Center) { Spinner() }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (history != null && !history.loading && history.error == null) {
            if (history.canWrite) {
                Composer(editing, action, onSend, onEdit, onCancelEdit = { editing = null; onClearError() }, onDone = { editing = null })
            } else {
                Text(
                    stringResource(R.string.info_readonly),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(Spacing.s4),
                )
            }
        }
    }
}

/** Der Tag über den Mitteilungen: «Heute», «Gestern» oder «Samstag, 14. November». */
@Composable
private fun DaySeparator(date: LocalDate) {
    val today = LocalDate.now(SWISS_ZONE)
    val text = when (date) {
        today -> stringResource(R.string.info_today)
        today.minusDays(1) -> stringResource(R.string.info_yesterday)
        else -> spokenDay(date)
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape).padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

private val URL = Regex("https?://[^\\s]+")

/** Der Text mit antippbaren Links (nur `http` und `https`); alles andere bleibt Text, nichts wird als Markup gelesen. */
@Composable
private fun linkedText(text: String): AnnotatedString {
    val style = TextLinkStyles(SpanStyle(color = MaterialTheme.fire.accent, textDecoration = TextDecoration.Underline))
    return buildAnnotatedString {
        var last = 0
        URL.findAll(text).forEach { match ->
            append(text.substring(last, match.range.first))
            withLink(LinkAnnotation.Url(match.value, style)) { append(match.value) }
            last = match.range.last + 1
        }
        append(text.substring(last))
    }
}

@Composable
private fun MessageRow(message: InfoMessage, onEdit: (InfoMessage) -> Unit, onDelete: (InfoMessage) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val time = clockText(message.createdAt).orEmpty()
    val role = when (message.author?.role) {
        "kommando" -> stringResource(R.string.role_commander)
        "leitung" -> stringResource(R.string.role_leader)
        else -> null
    }
    val deleted = stringResource(R.string.info_deleted)
    val edited = stringResource(R.string.info_edited)
    // Ohne Konto (gelöscht) bleibt der Text, der Absender ist neutral.
    val authorName = message.author?.name ?: stringResource(R.string.info_author_gone)
    val by = listOfNotNull(authorName, role).joinToString(", ")
    val spoken = if (message.deleted) "$deleted, $time" else stringResource(R.string.info_message_by, by, time) + ": " + message.text.orEmpty() +
        (if (message.edited) ", $edited" else "")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s3), verticalAlignment = Alignment.Top) {
        Row(
            Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = spoken },
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            if (message.deleted) {
                Box(Modifier.size(36.dp))
            } else {
                Avatar(authorName, size = 36.dp, imageUrl = message.author?.imageUrl, initialSize = 13.sp)
            }
            Column(Modifier.weight(1f)) {
                if (message.deleted) {
                    Text(deleted, style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic), color = scheme.onSurfaceVariant)
                    Text(time, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(authorName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f, fill = false))
                        role?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant) }
                        Text(time, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                    Text(linkedText(message.text.orEmpty()), style = MaterialTheme.typography.bodyLarge)
                    if (message.edited) Text(edited, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
        }
        if (!message.deleted && (message.canEdit || message.canDelete)) {
            var open by remember { mutableStateOf(false) }
            Box {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).focusRing(CircleShape).clickable(role = Role.Button) { open = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_more), contentDescription = stringResource(R.string.info_more), tint = scheme.onSurfaceVariant)
                }
                DropdownMenu(open, onDismissRequest = { open = false }) {
                    if (message.canEdit) DropdownMenuItem(text = { Text(stringResource(R.string.info_edit)) }, onClick = { open = false; onEdit(message) })
                    if (message.canDelete) DropdownMenuItem(text = { Text(stringResource(R.string.info_delete)) }, onClick = { open = false; onDelete(message) })
                }
            }
        }
    }
}

/** Das Ziehen kennt kein Screenreader: Er bekommt dieselbe Aktion am Feld, in dem die Liste steht. */
@Composable
private fun pullAction(onRefresh: () -> Unit): Modifier {
    val label = stringResource(R.string.refresh)
    return Modifier.semantics { customActions = listOf(CustomAccessibilityAction(label) { onRefresh(); true }) }
}

/** Warum das Neuladen durch Ziehen nicht ging; der alte Stand bleibt stehen. */
@Composable
private fun RefreshError(error: ApiError?) {
    error?.let {
        NoticeBanner(
            NoticeKind.ERROR,
            stringResource(it.text),
            Modifier.padding(start = Spacing.s3, end = Spacing.s3, bottom = Spacing.s2).semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/** Ab so vielen Zeichen zeigt die Eingabe den Zähler (das Limit steht bei [MAX_MESSAGE_CHARS]). */
private const val COUNTER_FROM = 1800

/**
 * Die Eingabe wie in jedem Chat: ein abgerundetes Feld, das bis fünf Zeilen wächst, und rechts der runde Senden-Knopf. Beim Bearbeiten
 * steht eine schmale Zeile darüber, die es abbrechen lässt. Die Eingabe leert sich erst nach Erfolg; der Fehler steht darüber.
 */
@Composable
private fun Composer(
    editing: InfoMessage?,
    action: ChannelAction,
    onSend: (String, () -> Unit) -> Unit,
    onEdit: (String, String, () -> Unit) -> Unit,
    onCancelEdit: () -> Unit,
    onDone: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var text by rememberSaveable(editing?.id) { mutableStateOf(editing?.text.orEmpty()) }
    val label = stringResource(if (editing != null) R.string.info_edit_label else R.string.info_compose_label)
    val canSend = text.isNotBlank() && !action.running
    val submit = {
        val trimmed = text.trim()
        if (editing != null) onEdit(editing.id, trimmed) { text = ""; onDone() } else onSend(trimmed) { text = "" }
    }
    Column(
        Modifier.fillMaxWidth().padding(start = Spacing.s3, end = Spacing.s3, top = Spacing.s2, bottom = Spacing.s2),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        action.error?.let { NoticeBanner(NoticeKind.ERROR, stringResource(it.text)) }
        if (editing != null) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                val cancel = stringResource(R.string.info_edit_cancel)
                Box(
                    Modifier.size(48.dp).clip(CircleShape).focusRing(CircleShape).clickable(role = Role.Button, onClick = onCancelEdit)
                        .semantics { contentDescription = cancel },
                    contentAlignment = Alignment.Center,
                ) { Icon(painterResource(R.drawable.ic_cross), contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            BasicTextField(
                value = text,
                onValueChange = { if (it.length <= MAX_MESSAGE_CHARS) text = it },
                enabled = !action.running,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .background(scheme.surfaceContainerHigh, RoundedCornerShape(24.dp))
                    .semantics { contentDescription = label },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.fire.accent),
                maxLines = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                decorationBox = { field ->
                    Box(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), contentAlignment = Alignment.CenterStart) {
                        if (text.isEmpty()) {
                            Text(stringResource(R.string.info_compose_placeholder), style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
                        }
                        field()
                    }
                },
            )
            val sendLabel = stringResource(if (editing != null) R.string.info_save else R.string.info_send)
            Box(
                Modifier
                    .size(Sizes.touchTarget)
                    .clip(CircleShape)
                    .focusRing(CircleShape)
                    .background(if (canSend || action.running) scheme.primary else scheme.surfaceContainerHigh)
                    .clickable(enabled = canSend, role = Role.Button, onClick = submit)
                    .semantics { contentDescription = sendLabel },
                contentAlignment = Alignment.Center,
            ) {
                if (action.running) {
                    Spinner(size = 20.dp, color = scheme.onPrimary)
                } else {
                    Icon(
                        painterResource(R.drawable.ic_send), contentDescription = null,
                        tint = if (canSend) scheme.onPrimary else scheme.onSurfaceVariant, modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        if (text.length >= COUNTER_FROM) {
            Text(
                "${text.length} / $MAX_MESSAGE_CHARS",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End).padding(end = 12.dp),
            )
        }
    }
}
