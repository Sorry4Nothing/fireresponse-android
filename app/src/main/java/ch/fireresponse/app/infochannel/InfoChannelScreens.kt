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
import ch.fireresponse.app.ui.LabeledTextField
import ch.fireresponse.app.ui.LoadingSkeleton
import ch.fireresponse.app.ui.NoticeBanner
import ch.fireresponse.app.ui.NoticeKind
import ch.fireresponse.app.ui.PrimaryButton
import ch.fireresponse.app.ui.SquadMark
import ch.fireresponse.app.ui.TextActionButton
import ch.fireresponse.app.ui.UnreadBadge
import ch.fireresponse.app.ui.focusRing
import ch.fireresponse.app.ui.theme.Spacing
import ch.fireresponse.app.ui.theme.fire
import java.time.LocalDate

/** Die Liste der Infokanäle, wenn die Person mehrere hat: eine Zeile je Einheit mit Zähler und Zeit der letzten Mitteilung. */
@Composable
fun InfoChannelListScreen(state: ApiResult<List<InfoChannel>>?, onOpen: (String) -> Unit, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Header(title = stringResource(R.string.tab_info))
        when (state) {
            null -> LoadingSkeleton(stringResource(R.string.info_loading))
            is ApiResult.Failure -> ErrorState(state.error, onRetry)
            is ApiResult.Ok -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.s4, vertical = Spacing.s2),
                verticalArrangement = Arrangement.spacedBy(Spacing.s2),
            ) {
                items(state.value, key = { it.unit.id }) { ChannelRow(it, onOpen) }
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
            history.messages.isEmpty() -> EmptyState(
                R.drawable.ic_chat,
                stringResource(R.string.info_empty_title),
                stringResource(if (history.canWrite) R.string.info_empty_writer else R.string.info_empty_reader),
                onRetry,
                Modifier.weight(1f),
            )
            else -> Column(Modifier.weight(1f)) {
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
                        item(key = "aelter") {
                            // Die nächste Seite lädt, sobald das Ende der geladenen erreicht ist; ging das schief, bleibt der Knopf.
                            LaunchedEffect(history.messages.size) { onLoadOlder() }
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                if (!history.loadingMore) TextActionButton(stringResource(R.string.info_load_older), onLoadOlder)
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

/** Die Eingabe: neue Mitteilung an die ganze Einheit oder, nach «Bearbeiten», der Text der eigenen. Die Eingabe leert sich erst nach Erfolg. */
@Composable
private fun Composer(
    editing: InfoMessage?,
    action: ChannelAction,
    onSend: (String, () -> Unit) -> Unit,
    onEdit: (String, String, () -> Unit) -> Unit,
    onCancelEdit: () -> Unit,
    onDone: () -> Unit,
) {
    var text by rememberSaveable(editing?.id) { mutableStateOf(editing?.text.orEmpty()) }
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(Spacing.s4),
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        LabeledTextField(
            label = stringResource(if (editing != null) R.string.info_edit_label else R.string.info_compose_label),
            value = text,
            onValueChange = { text = it },
            maxChars = MAX_MESSAGE_CHARS,
            minLines = 2,
            hint = stringResource(R.string.info_compose_hint),
            enabled = !action.running,
        )
        action.error?.let { NoticeBanner(NoticeKind.ERROR, stringResource(it.text)) }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalAlignment = Alignment.CenterVertically) {
            if (editing != null) TextActionButton(stringResource(R.string.cancel), onCancelEdit)
            Box(Modifier.weight(1f))
            PrimaryButton(
                stringResource(if (editing != null) R.string.info_save else R.string.info_send),
                onClick = {
                    val trimmed = text.trim()
                    if (editing != null) onEdit(editing.id, trimmed) { text = ""; onDone() } else onSend(trimmed) { text = "" }
                },
                icon = R.drawable.ic_send,
                enabled = text.isNotBlank() && !action.running,
                loading = action.running,
                loadingText = stringResource(R.string.working),
            )
        }
    }
}
