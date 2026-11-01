package io.github.sorry4nothing.cevios.chat

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sorry4nothing.cevios.R
import kotlinx.coroutines.delay
import io.github.sorry4nothing.cevios.net.ApiResult
import io.github.sorry4nothing.cevios.programs.SWISS_ZONE
import io.github.sorry4nothing.cevios.programs.dayAndMonth
import io.github.sorry4nothing.cevios.ui.Avatar
import io.github.sorry4nothing.cevios.ui.CeviDialog
import io.github.sorry4nothing.cevios.ui.avatarInitial
import io.github.sorry4nothing.cevios.ui.DashedButton
import io.github.sorry4nothing.cevios.ui.InitialText
import io.github.sorry4nothing.cevios.ui.scaledWithFont
import io.github.sorry4nothing.cevios.ui.EmptyState
import io.github.sorry4nothing.cevios.ui.ErrorState
import io.github.sorry4nothing.cevios.ui.Header
import io.github.sorry4nothing.cevios.ui.HeaderButton
import io.github.sorry4nothing.cevios.ui.Loading
import io.github.sorry4nothing.cevios.ui.NoticeBanner
import io.github.sorry4nothing.cevios.ui.NoticeKind
import io.github.sorry4nothing.cevios.ui.OutlinedPillButton
import io.github.sorry4nothing.cevios.ui.Spinner
import io.github.sorry4nothing.cevios.ui.TextActionButton
import io.github.sorry4nothing.cevios.ui.focusRing
import io.github.sorry4nothing.cevios.ui.theme.Pill
import io.github.sorry4nothing.cevios.ui.StageLogo
import io.github.sorry4nothing.cevios.ui.theme.Sizes
import io.github.sorry4nothing.cevios.ui.theme.Spacing
import io.github.sorry4nothing.cevios.ui.theme.cevi
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val MAX_CHARS = 2000
private const val MAX_INPUT_LINES = 5

@Composable
fun chatTitle(chat: Chat?): String = when {
    chat == null -> stringResource(R.string.chat_title)
    chat.isGroup -> stringResource(R.string.chat_group, chat.ageGroup.name)
    else -> stringResource(R.string.chat_direct, chat.parent?.displayName.orEmpty()).trim()
}

@Composable
fun ChatListScreen(
    state: ApiResult<ChatList>?,
    profile: Profile?,
    action: ActionState,
    onOpen: (String) -> Unit,
    onDirectChat: (ageGroupId: String) -> Unit,
    phoneVisible: Boolean,
    onPhone: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Header(title = stringResource(R.string.chat_title), focusOnTitle = true)
        if (phoneVisible) TextActionButton(stringResource(R.string.phone_button), onPhone, Modifier.padding(horizontal = Spacing.s3))
        ActionNotice(action, Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s2))
        when (state) {
            null -> Loading()
            is ApiResult.Failure -> ErrorState(state.error, onRetry)
            is ApiResult.Ok -> {
                val startable = ChatRules.ageGroupsForDirectChat(profile, state.value.chats)
                if (state.value.chats.isEmpty() && startable.isEmpty()) {
                    EmptyState(
                        R.drawable.ic_chat,
                        stringResource(R.string.chat_empty),
                        stringResource(R.string.chat_empty_text),
                        onRetry,
                    )
                } else {
                    ChatSections(state.value.chats, startable, action.running, onOpen, onDirectChat)
                }
            }
        }
    }
}

/** Gruppenchats und Einzelchats in eigenen Abschnitten; ein Abschnitt ohne Einträge fällt weg. */
@Composable
private fun ChatSections(
    chats: List<Chat>,
    startable: List<ProfileAgeGroup>,
    actionRunning: Boolean,
    onOpen: (String) -> Unit,
    onDirectChat: (ageGroupId: String) -> Unit,
) {
    // Welche Stufe gerade einen Einzelchat startet: nur dieser Knopf zeigt den Ladekreis, die anderen sind gesperrt.
    var starting by remember { mutableStateOf<String?>(null) }
    val groups = chats.filter { it.isGroup }
    val direct = chats.filterNot { it.isGroup }
    LazyColumn(
        contentPadding = PaddingValues(start = Spacing.s4, end = Spacing.s4, bottom = Spacing.s4),
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        if (groups.isNotEmpty()) {
            item(key = "title-groups") { SectionTitle(R.string.chat_section_groups) }
            items(groups, key = { it.id }) { ChatCard(it, onOpen) }
        }
        if (direct.isNotEmpty() || startable.isNotEmpty()) {
            item(key = "title-direct") { SectionTitle(R.string.chat_section_group_leaders) }
            items(direct, key = { it.id }) { ChatCard(it, onOpen) }
            items(startable, key = { "start-" + it.id }) { ageGroup ->
                DashedButton(
                    stringResource(R.string.direct_chat_start, ageGroup.name),
                    onClick = { starting = ageGroup.id; onDirectChat(ageGroup.id) },
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.s2),
                    icon = R.drawable.ic_plus,
                    enabled = !actionRunning || starting == ageGroup.id,
                    loading = actionRunning && starting == ageGroup.id,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(@StringRes text: Int) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = Spacing.s1, end = Spacing.s1, top = Spacing.s3, bottom = 2.dp).semantics { heading() },
    )
}

/** Karte einer Chatzeile: Titel, Vorschau der letzten Nachricht, Zeit und Zähler «ungelesen». */
@Composable
private fun ChatCard(chat: Chat, onOpen: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val unreadText = if (chat.unread > 0) pluralStringResource(R.plurals.unread_messages, chat.unread, chat.unread) else null
    Row(
        Modifier
            .fillMaxWidth()
            .focusRing(shape)
            .clip(shape)
            .background(scheme.surfaceContainerLow)
            .border(1.dp, scheme.outlineVariant, shape)
            .clickable(onClickLabel = stringResource(R.string.chat_open)) { onOpen(chat.id) }
            // Der Name der Zeile beginnt mit dem sichtbaren Titel, die Zahl folgt als Zustand.
            .semantics { unreadText?.let { stateDescription = it } }
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (chat.isGroup) StageLogo(chat.ageGroup.id, size = 56.dp) else ChatAvatar()
        Column(Modifier.weight(1f)) {
            Text(chatTitle(chat), style = MaterialTheme.typography.titleMedium)
            if (!chat.isGroup) {
                Text(
                    stringResource(R.string.chat_age_group, chat.ageGroup.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
            chat.lastMessage?.let { last ->
                val preview = if (last.own) {
                    stringResource(R.string.chat_preview_own, last.text)
                } else {
                    stringResource(R.string.chat_preview_other, last.author.displayName, last.text)
                }
                Text(
                    preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = PREVIEW_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        val badge = unreadBadgeText(chat.unread)
        val time = chat.lastMessage?.let { previewTimeText(it.createdAt) }.orEmpty()
        if (badge != null || time.isNotEmpty()) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                if (time.isNotEmpty()) {
                    Text(time, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
                badge?.let { UnreadBadge(it) }
            }
        }
    }
}

/** Uhrzeit, wenn die Nachricht von heute ist, sonst «Gestern» oder das Datum (Schweizer Zeit). */
@Composable
private fun previewTimeText(createdAt: String): String {
    val day = dayOf(createdAt) ?: return ""
    return when (val label = dayLabel(day, LocalDate.now(SWISS_ZONE))) {
        DayLabel.Today -> timeOf(createdAt)
        DayLabel.Yesterday -> stringResource(R.string.day_yesterday)
        is DayLabel.On -> PREVIEW_DATE.format(label.date)
    }
}

private const val PREVIEW_LINES = 2
private val PREVIEW_DATE = DateTimeFormatter.ofPattern("d.M.")

/** Zahl in einer roten Pille (nie nur Farbe). Wächst mit der Schrift. Für den Screenreader steht die Zahl am Zustand der Zeile. */
@Composable
private fun UnreadBadge(text: String) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .heightIn(min = 24.dp)
            .widthIn(min = 24.dp)
            .background(scheme.primary, Pill)
            .padding(horizontal = 8.dp)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"), color = scheme.onPrimary)
    }
}

/** Einzelchat: «SL» (Stufenleitung) auf `secondaryContainer`. Dekorativ. Der Gruppenchat zeigt das Stufenlogo ([StageLogo]). */
@Composable
private fun ChatAvatar() {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.size(48.dp).background(scheme.secondaryContainer, Pill), contentAlignment = Alignment.Center) {
        Text(
            stringResource(R.string.avatar_group_leaders),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = scheme.onSecondaryContainer,
        )
    }
}

@Composable
fun ChatConversationScreen(
    title: String,
    chat: Chat?,
    profile: Profile?,
    state: ApiResult<Conversation>?,
    loadingOlder: Boolean,
    send: SendState,
    action: ActionState,
    onSend: (text: String, onSent: () -> Unit) -> Unit,
    onOlder: () -> Unit,
    onDirectChat: (ageGroupId: String, parentUserId: String) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    // Safe-Drawing-Padding enthält die Tastatur: Die Eingabe bleibt sichtbar.
    // In einem niedrigen Fenster (Querformat) mit offener Tastatur bliebe für den grossen Kopf alles, für die Eingabe nichts:
    // Dort entfällt der Kopf, bis die Tastatur zu ist.
    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val windowHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
    val compact = keyboardOpen && windowHeight < COMPACT_HEIGHT
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        // Alles ausser Eingabe: Die Eingabe wird zuerst gemessen und behält immer ihren Platz.
        Column(Modifier.weight(1f)) {
            if (!compact) {
                Header(
                    title = title,
                    eyebrow = chat?.ageGroup?.name,
                    eyebrowStages = listOfNotNull(chat?.ageGroup?.id),
                    back = HeaderButton(R.drawable.ic_back, stringResource(R.string.back_to_chat), onBack),
                    titleStyle = MaterialTheme.typography.headlineMedium,
                    focusOnTitle = true,
                )
                // Im Einzelchat lesen alle Stufenleitungen der Stufe mit; das steht im Kopf und scrollt nicht weg.
                if (chat != null && !chat.isGroup && chat.groupLeaders.isNotEmpty()) {
                    ReadAlongNote(chat.groupLeaders, Modifier.padding(start = Spacing.s4, end = Spacing.s4, bottom = Spacing.s4))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            Column(Modifier.weight(1f)) {
                when (state) {
                    null -> Loading()
                    is ApiResult.Failure -> ErrorState(state.error, onRetry, notFoundText = R.string.state_chat_not_found_text)
                    is ApiResult.Ok ->
                        Box(Modifier.fillMaxSize()) {
                            Messages(state.value, chat, profile, loadingOlder, onOlder, onDirectChat)
                            NewMessagesAnnouncer(state.value)
                        }
                }
            }
        }
        ActionNotice(action, Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s2))
        MessageInput(send, onSend)
    }
}

/**
 * Sagt neue Nachrichten anderer gesammelt an («2 neue Nachrichten»), ohne eine laufende Ansage abzubrechen. Es gibt dafür
 * eine winzige, unsichtbare Fläche mit Live-Region; der Text steht kurz da und wird dann geleert, damit dieselbe Ansage
 * beim nächsten Mal wieder gesprochen wird.
 */
@Composable
private fun NewMessagesAnnouncer(conversation: Conversation) {
    var known by remember { mutableStateOf<String?>(null) }
    var count by remember { mutableIntStateOf(0) }
    val newest = conversation.messages.firstOrNull()?.id
    LaunchedEffect(newest) {
        val added = conversation.newFromOthers(known)
        known = newest
        if (added > 0) {
            count = added
            delay(ANNOUNCEMENT_MS)
            count = 0
        }
    }
    val text = if (count > 0) pluralStringResource(R.plurals.new_messages, count, count) else ""
    Box(Modifier.size(1.dp).semantics { liveRegion = LiveRegionMode.Polite; contentDescription = text })
}

private const val ANNOUNCEMENT_MS = 3_000L

/** Fenster, die niedriger sind (Querformat eines Telefons), zeigen mit offener Tastatur den Kopf nicht. */
private val COMPACT_HEIGHT = 500.dp

/** Hinweis im Einzelchat: Die Nachricht geht an die ganze Stufenleitung. Die Avatare sind dekorativ. */
@Composable
private fun ReadAlongNote(leaders: List<ChatPerson>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val names = leaders.joinToString(", ") { it.displayName }
    val full = stringResource(R.string.chat_read_along, names)
    val text = buildAnnotatedString {
        append(full)
        val start = full.lastIndexOf(names)
        if (start >= 0) addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, start + names.length)
    }
    Row(
        modifier.fillMaxWidth().background(scheme.secondaryContainer, MaterialTheme.shapes.medium).padding(horizontal = Spacing.s3, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
            leaders.take(3).forEach { leader ->
                Box(
                    Modifier.size(scaledWithFont(28.dp)).background(scheme.secondary, Pill).border(2.dp, scheme.secondaryContainer, Pill),
                    contentAlignment = Alignment.Center,
                ) {
                    // Eine Initiale: zwei Buchstaben würde der nächste Kreis überdecken.
                    InitialText(avatarInitial(leader.displayName), 11.sp, scheme.onSecondary)
                }
            }
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = scheme.onSecondaryContainer)
    }
}

@Composable
private fun Messages(
    conversation: Conversation,
    chat: Chat?,
    profile: Profile?,
    loadingOlder: Boolean,
    onOlder: () -> Unit,
    onDirectChat: (ageGroupId: String, parentUserId: String) -> Unit,
) {
    if (conversation.messages.isEmpty()) {
        Text(stringResource(R.string.messages_empty), Modifier.padding(Spacing.s4), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    var selected by remember { mutableStateOf<ChatMessage?>(null) }
    val items = remember(conversation.messages) { withDayDividers(conversation.messages) }
    val today = LocalDate.now(SWISS_ZONE)

    fun perform(message: ChatMessage, action: MessageAction) {
        when (action) {
            MessageAction.OPEN_DIRECT_CHAT -> chat?.let { onDirectChat(it.ageGroup.id, message.author.id) }
        }
    }

    // Eine eben gesendete Nachricht soll sichtbar sein, auch wenn man weiter oben gelesen hat.
    val listState = rememberLazyListState()
    val newestOwnId = conversation.messages.firstOrNull()?.takeIf { it.own }?.id
    LaunchedEffect(newestOwnId) { if (newestOwnId != null) listState.scrollToItem(0) }

    // Neueste zuerst und von unten nach oben: so wächst der Verlauf wie in jedem Chat nach oben.
    LazyColumn(
        state = listState,
        reverseLayout = true,
        contentPadding = PaddingValues(Spacing.s4),
        verticalArrangement = Arrangement.spacedBy(Spacing.s3, Alignment.Bottom),
    ) {
        items(
            items,
            key = {
                when (it) {
                    is ConversationItem.Message -> it.message.id
                    is ConversationItem.DayDivider -> "day-" + it.date
                }
            },
        ) { item ->
            when (item) {
                is ConversationItem.Message -> MessageBubble(
                    item.message,
                    ChatRules.actions(chat, item.message, profile),
                    onLongPress = { selected = item.message },
                    onAction = { perform(item.message, it) },
                )
                is ConversationItem.DayDivider -> DayDividerText(dayLabel(item.date, today))
            }
        }
        if (conversation.next != null) {
            item(key = "older") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    OutlinedPillButton(
                        stringResource(R.string.messages_older),
                        onClick = onOlder,
                        loading = loadingOlder,
                        loadingText = stringResource(R.string.loading),
                    )
                }
            }
        }
    }

    selected?.let { message ->
        CeviDialog(
            title = stringResource(R.string.actions_title, message.author.displayName),
            onDismiss = { selected = null },
            confirmButton = { TextActionButton(stringResource(R.string.cancel), { selected = null }) },
        ) {
            Column {
                ChatRules.actions(chat, message, profile).forEach { action ->
                    // Nach links um den Innenabstand des Knopfes: Der Text steht bündig mit dem Titel.
                    TextActionButton(actionLabel(action, message), {
                        selected = null
                        perform(message, action)
                    }, Modifier.offset(x = -Spacing.s3))
                }
            }
        }
    }
}

@Composable
private fun actionLabel(action: MessageAction, message: ChatMessage): String = when (action) {
    MessageAction.OPEN_DIRECT_CHAT -> stringResource(R.string.action_direct_chat, message.author.displayName)
}

@Composable
private fun DayDividerText(label: DayLabel) {
    Text(
        when (label) {
            DayLabel.Today -> stringResource(R.string.day_today)
            DayLabel.Yesterday -> stringResource(R.string.day_yesterday)
            is DayLabel.On -> dayAndMonth(label.date)
        },
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/**
 * Eine Nachricht als Blase, höchstens 80 % breit; fremde Nachrichten haben links einen Avatar. Die Aktion (Einzelchat öffnen) liegt auf langem
 * Drücken und als Screenreader-Aktion; ohne erlaubte Aktion gibt es keine.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(message: ChatMessage, actions: List<MessageAction>, onLongPress: () -> Unit, onAction: (MessageAction) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val own = message.own
    val labels = actions.map { it to actionLabel(it, message) }
    // Die untere Ecke zur Absenderseite ist spitz (6 dp).
    val pointedOwn = own
    val shape = RoundedCornerShape(
        topStart = 20.dp, topEnd = 20.dp,
        bottomStart = if (pointedOwn) 20.dp else 6.dp,
        bottomEnd = if (pointedOwn) 6.dp else 20.dp,
    )
    val ringShape = MaterialTheme.shapes.medium
    // Die Blase samt Name und Zeit; 80 % der Breite, bei fremden Nachrichten der Spalte neben dem Avatar.
    val column: @Composable () -> Unit = {
        Column(
            Modifier
                .fillMaxWidth(0.8f)
                .focusRing(ringShape)
                .semantics(mergeDescendants = true) {
                    customActions = labels.map { (action, label) -> CustomAccessibilityAction(label) { onAction(action); true } }
                }
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = actions.isNotEmpty(),
                    onLongClick = onLongPress,
                    onClick = {},
                ),
            horizontalAlignment = if (own) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(Spacing.s1),
        ) {
            if (!own) {
                val authorLine = if (message.author.groupLeader) {
                    stringResource(R.string.message_author_leader, message.author.displayName)
                } else {
                    message.author.displayName
                }
                Text(authorLine, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.cevi.accent)
            }
            val content = if (own) scheme.onSecondary else scheme.onSurface
            Column(
                Modifier
                    .background(if (own) scheme.secondary else scheme.surfaceContainerLow, shape)
                    .then(if (own) Modifier else Modifier.border(1.dp, scheme.outlineVariant, shape))
                    .padding(horizontal = Spacing.s4, vertical = Spacing.s3),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(message.text.orEmpty(), style = MaterialTheme.typography.bodyLarge, color = content)
                message.program?.let {
                    Text(stringResource(R.string.message_program, it.title), style = MaterialTheme.typography.bodyMedium, color = content)
                }
            }
            val time = message.timeText()
            Text(
                if (own) stringResource(R.string.message_own_time, time) else time,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
    if (own) {
        Box(Modifier.fillMaxWidth(), contentAlignment = if (own) Alignment.CenterEnd else Alignment.CenterStart) { column() }
    } else {
        // Avatar unten bündig mit der Blase: Er rückt um die Zeile der Zeit nach oben.
        val timeHeight = with(LocalDensity.current) { MaterialTheme.typography.bodySmall.lineHeight.toDp() } + Spacing.s1
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalAlignment = Alignment.Bottom) {
            Avatar(message.author.displayName, Modifier.padding(bottom = timeHeight), imageUrl = message.author.imageUrl)
            Box(Modifier.weight(1f)) { column() }
        }
    }
}

/** Läuft oder Fehler einer Aktion als Banner; angesagt als Live-Region. Ohne Anlass bleibt nichts stehen. */
@Composable
private fun ActionNotice(action: ActionState, modifier: Modifier = Modifier) {
    val polite = modifier.semantics { liveRegion = LiveRegionMode.Polite }
    when {
        action.error != null -> NoticeBanner(NoticeKind.ERROR, stringResource(action.error.text), modifier)
        action.running -> NoticeBanner(NoticeKind.INFO, stringResource(R.string.action_running), polite)
    }
}

@Composable
private fun MessageInput(send: SendState, onSend: (String, () -> Unit) -> Unit) {
    var draft by rememberSaveable { mutableStateOf("") }
    var emptyHint by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val label = stringResource(R.string.message_input)
    val sendLabel = stringResource(R.string.message_send)
    val sendingLabel = stringResource(R.string.message_sending)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val fieldShape = RoundedCornerShape(26.dp)
    Column(
        Modifier.padding(start = Spacing.s3, end = Spacing.s3, top = 10.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (send.running) {
            NoticeBanner(NoticeKind.INFO, stringResource(R.string.message_sending), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        send.error?.let { NoticeBanner(NoticeKind.ERROR, stringResource(it.text)) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = draft,
                onValueChange = {
                    if (it.length <= MAX_CHARS) draft = it
                    emptyHint = false
                },
                modifier = Modifier.weight(1f).semantics { contentDescription = label },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.onSurface),
                maxLines = MAX_INPUT_LINES,
                interactionSource = interaction,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = Sizes.textField)
                            .background(scheme.surfaceContainerLow, fieldShape)
                            .border(if (focused) 2.dp else 1.dp, if (focused) MaterialTheme.cevi.focus else scheme.outline, fieldShape)
                            .padding(horizontal = 20.dp, vertical = 15.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (draft.isEmpty()) {
                            Text(stringResource(R.string.message_placeholder), style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
                        }
                        inner()
                    }
                },
            )
            Surface(
                onClick = {
                    if (draft.isBlank()) emptyHint = true else onSend(draft) { draft = "" }
                },
                enabled = !send.running,
                modifier = Modifier.size(52.dp).focusRing(Pill).semantics {
                    contentDescription = sendLabel
                    if (send.running) stateDescription = sendingLabel
                },
                shape = Pill,
                color = scheme.primary,
                contentColor = scheme.onPrimary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (send.running) Spinner(size = Sizes.icon)
                    else Icon(painterResource(R.drawable.ic_send), contentDescription = null, modifier = Modifier.size(Sizes.icon))
                }
            }
        }
        if (emptyHint) {
            Text(
                stringResource(R.string.message_empty_hint),
                modifier = Modifier.padding(horizontal = 20.dp).semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}
