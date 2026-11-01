package io.github.sorry4nothing.cevios.programs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sorry4nothing.cevios.R
import io.github.sorry4nothing.cevios.contacts.ContactCard
import io.github.sorry4nothing.cevios.contacts.ContactList
import io.github.sorry4nothing.cevios.net.ApiResult
import io.github.sorry4nothing.cevios.ui.ChipKind
import io.github.sorry4nothing.cevios.ui.ErrorState
import io.github.sorry4nothing.cevios.ui.Header
import io.github.sorry4nothing.cevios.ui.HeaderButton
import io.github.sorry4nothing.cevios.ui.LoadingSkeleton
import io.github.sorry4nothing.cevios.ui.EmptyState
import io.github.sorry4nothing.cevios.ui.NoticeBanner
import io.github.sorry4nothing.cevios.ui.NoticeKind
import io.github.sorry4nothing.cevios.ui.OutlinedPillButton
import io.github.sorry4nothing.cevios.ui.StatusChip
import io.github.sorry4nothing.cevios.ui.focusRing
import io.github.sorry4nothing.cevios.ui.StageLogo
import io.github.sorry4nothing.cevios.ui.theme.Spacing
import io.github.sorry4nothing.cevios.ui.theme.cevi
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
private val CARD = RoundedCornerShape(16.dp)

@Composable
fun ProgramListScreen(
    state: ApiResult<ProgramList>?,
    onOpen: (String) -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Header(
            title = stringResource(R.string.programs_title),
            action = HeaderButton(R.drawable.ic_refresh, stringResource(R.string.refresh), onRetry),
            focusOnTitle = true,
        )
        when (state) {
            null -> LoadingSkeleton(stringResource(R.string.programs_loading))
            is ApiResult.Failure -> ErrorState(state.error, onRetry)
            is ApiResult.Ok ->
                if (state.value.programs.isEmpty()) {
                    EmptyState(
                        R.drawable.ic_calendar,
                        stringResource(R.string.programs_empty_title),
                        stringResource(R.string.programs_empty_text),
                        onRetry,
                    )
                } else {
                    Timeline(groupByDay(state.value.programs), onOpen)
                }
        }
    }
}

/** Die Liste als Zeitleiste: links das Datum an einer Linie, rechts die Programme dieses Tages als Karten. */
@Composable
private fun Timeline(days: List<Day>, onOpen: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(start = Spacing.s3, end = Spacing.s5, bottom = Spacing.s4)) {
        itemsIndexed(days, key = { _, day -> day.programs.first().id }) { index, day ->
            val isLast = index == days.lastIndex
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                DateColumn(day, withLine = !isLast)
                Column(
                    Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 18.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s2),
                ) {
                    day.programs.forEach { ProgramCard(it, onOpen) }
                }
            }
        }
    }
}

@Composable
private fun DateColumn(day: Day, withLine: Boolean) {
    Column(Modifier.width(52.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
        val date = day.date
        if (date != null) {
            Text(weekdayShort(date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.cevi.accent)
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp, lineHeight = 30.sp, fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (withLine) {
            Box(
                Modifier.padding(vertical = 6.dp).width(2.dp).weight(1f)
                    .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(1.dp)),
            )
        }
    }
}

@Composable
private fun ProgramCard(program: Program, onOpen: (String) -> Unit) {
    val ageGroups = program.ageGroups.joinToString(", ") { it.name }
    val headerLine = listOf(listTime(program), ageGroups).filter { it.isNotBlank() }.joinToString(" · ")
    Column(
        Modifier
            .fillMaxWidth()
            .focusRing(CARD)
            .clip(CARD)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClickLabel = stringResource(R.string.program_open)) { onOpen(program.id) }
            .padding(horizontal = Spacing.s4, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalAlignment = Alignment.CenterVertically) {
            program.ageGroups.forEach { StageLogo(it.id, size = 28.dp) }
            Text(
                headerLine,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                color = MaterialTheme.cevi.accent,
            )
        }
        Text(program.title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 24.sp))
        program.location?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        // Je eigenes Kind der Stand (Vorname und «kommt», «kommt nicht» oder «noch offen»); bei einem abgesagten Programm steht schon «Abgesagt» da.
        if (!program.cancelled) program.children.forEach { ChildStatus(it) }
        ProgramStatus(program, Modifier.padding(top = 6.dp))
    }
}

/** Der Stand eines eigenen Kindes auf der Karte: Symbol und Wort, nie nur Farbe. */
@Composable
private fun ChildStatus(child: ProgramChild) {
    val (icon, text) = when (child.status) {
        "ja" -> R.drawable.ic_check to stringResource(R.string.child_status_yes, child.firstName)
        "nein" -> R.drawable.ic_cross to stringResource(R.string.child_status_no, child.firstName)
        else -> R.drawable.ic_question to stringResource(R.string.child_status_open, child.firstName)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s1), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Abgesagt, «wird noch geprüft» oder «nicht zugeordnet» (nur Leitungen sehen solche Programme): als Etikett mit Icon und
 * Wort. Ein aktives, zugeordnetes Programm braucht keins.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgramStatus(program: Program, modifier: Modifier = Modifier) {
    if (!program.cancelled && !program.inReview && !program.unassigned) return
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        if (program.cancelled) StatusChip(ChipKind.NO, stringResource(R.string.status_cancelled))
        if (program.inReview) StatusChip(ChipKind.REVIEW, stringResource(R.string.status_review))
        if (program.unassigned) StatusChip(ChipKind.UNASSIGNED, stringResource(R.string.status_unassigned))
    }
}

/** «14:00–17:00», bei mehreren Tagen der Beginn, bei ganztägigen «Ganztägig». */
@Composable
private fun listTime(program: Program): String = when (val time = program.time()) {
    null -> stringResource(R.string.time_pending)
    is Timing.AllDay ->
        if (time.from == time.until) {
            stringResource(R.string.time_list_all_day)
        } else {
            stringResource(R.string.time_list_until, DateTimeFormatter.ofPattern("d.M.").format(time.until))
        }
    is Timing.Timed ->
        if (time.start.toLocalDate() == time.end.toLocalDate()) {
            "${TIME_FORMAT.format(time.start)}–${TIME_FORMAT.format(time.end)}"
        } else {
            TIME_FORMAT.format(time.start)
        }
}

@Composable
fun ProgramDetailScreen(
    state: ApiResult<Program>?,
    rsvp: RsvpState,
    canAssign: Boolean,
    canSeeRegistrations: Boolean,
    contacts: ApiResult<ContactList>?,
    onChild: (childId: String, attending: Boolean) -> Unit,
    onLeader: (attending: Boolean) -> Unit,
    onAssign: () -> Unit,
    onRegistrations: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val back = HeaderButton(R.drawable.ic_back, stringResource(R.string.back_to_programs), onBack)
    Column(Modifier.fillMaxSize()) {
        when (state) {
            null -> {
                Header(title = null, back = back)
                LoadingSkeleton(stringResource(R.string.program_loading))
            }
            is ApiResult.Failure -> {
                Header(title = null, back = back)
                ErrorState(state.error, onRetry, onToPrograms = onBack)
            }
            is ApiResult.Ok -> ProgramContent(state.value, rsvp, canAssign, canSeeRegistrations, contacts, back, onChild, onLeader, onAssign, onRegistrations)
        }
    }
}

@Composable
private fun ProgramContent(
    program: Program,
    rsvp: RsvpState,
    canAssign: Boolean,
    canSeeRegistrations: Boolean,
    contacts: ApiResult<ContactList>?,
    back: HeaderButton,
    onChild: (childId: String, attending: Boolean) -> Unit,
    onLeader: (attending: Boolean) -> Unit,
    onAssign: () -> Unit,
    onRegistrations: () -> Unit,
) {
    val ageGroups = program.ageGroups.joinToString(", ") { it.name }
    val date = program.startDay()?.let { "${weekdayShort(it)} ${dayAndMonth(it)}" }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header(
            title = program.title,
            eyebrow = listOfNotNull(ageGroups.ifBlank { null }, date).joinToString(" · "),
            eyebrowStages = program.ageGroups.map { it.id },
            back = back,
            titleStyle = MaterialTheme.typography.headlineMedium,
            focusOnTitle = true,
        )
        Column(Modifier.padding(horizontal = Spacing.s5), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            ProgramStatus(program)
            Tiles(program)
            Facts(program)
            RegistrationSection(program, rsvp, onChild, onLeader)
            ContactSection(program, contacts)
            LeaderSection(program, canAssign, canSeeRegistrations, onAssign, onRegistrations)
            Box(Modifier.height(Spacing.s4))
        }
    }
}

/** «Kontakt» mit den Stufenleitungen des Programms (wer abgesagt hat, fehlt). Ein Fehler beim Laden zeigt keinen Abschnitt. */
@Composable
private fun ContactSection(program: Program, contacts: ApiResult<ContactList>?) {
    if (program.unassigned || contacts !is ApiResult.Ok) return
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
        Text(
            stringResource(R.string.contact_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 6.dp).semantics { heading() },
        )
        ContactCard(contacts.value.contacts)
    }
}

/**
 * Aktionen für Leitende: die Anmeldungen ansehen (Admins sowie Leitende und SL der Stufen) und, nur für Admins und
 * Stufenleitungen, Stufen und Sichtbarkeit setzen.
 */
@Composable
private fun LeaderSection(
    program: Program,
    canAssign: Boolean,
    canSeeRegistrations: Boolean,
    onAssign: () -> Unit,
    onRegistrations: () -> Unit,
) {
    if (!canAssign && !canSeeRegistrations) return
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
        Text(
            stringResource(R.string.section_leaders),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 6.dp).semantics { heading() },
        )
        if (canAssign && program.unassigned) NoticeBanner(NoticeKind.INFO, stringResource(R.string.unassigned_notice))
        if (canSeeRegistrations) {
            OutlinedPillButton(stringResource(R.string.registrations_action), onRegistrations, Modifier.fillMaxWidth(), icon = R.drawable.ic_group)
        }
        if (canAssign) OutlinedPillButton(stringResource(R.string.assignment_action), onAssign, Modifier.fillMaxWidth())
    }
}

/** Zwei Kacheln, die Eltern am häufigsten suchen: wann und wo man sich trifft, wann und wo es endet. */
@Composable
private fun Tiles(program: Program) {
    val closing = program.closingTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
    val meeting = parseMeetingPoint(program.meetingPoint)
    // Nennt die Webseite eine Zeit, gilt sie; sonst der Beginn aus dem Kalender.
    val meetingTime = meeting.time ?: when (val time = program.time()) {
        is Timing.Timed -> TIME_FORMAT.format(time.start)
        is Timing.AllDay -> stringResource(R.string.time_list_all_day)
        null -> null
    }
    val meetingPoint = meeting.place
    val closingLocation = program.closingLocation?.takeIf { it.isNotBlank() }
    Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        if (meetingTime != null || meetingPoint != null) {
            Tile(stringResource(R.string.section_meeting_point), meetingTime, meetingPoint, Modifier.weight(1f).fillMaxHeight())
        }
        if (closing != null || closingLocation != null) {
            Tile(
                stringResource(R.string.section_closing), closing?.let { TIME_FORMAT.format(it) }, closingLocation,
                Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun Tile(label: String, time: String?, location: String?, modifier: Modifier) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow, CARD)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CARD)
            .padding(horizontal = 14.dp, vertical = Spacing.s3),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        time?.let { Text(it, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp, fontFeatureSettings = "tnum")) }
        location?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/** Liste mit Ort und Mitnehmen, je mit Icon. Felder ohne Inhalt fallen weg. */
@Composable
private fun Facts(program: Program) {
    val rows = listOf(
        Triple(R.drawable.ic_location, R.string.fact_location, program.googleLocation?.takeIf { it.isNotBlank() }),
        Triple(R.drawable.ic_bring_along, R.string.section_bring_along, program.bringAlong?.takeIf { it.isNotBlank() }),
    ).filter { it.third != null }
    if (rows.isEmpty()) return
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, CARD)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CARD)
            .padding(horizontal = 14.dp, vertical = 4.dp),
    ) {
        rows.forEachIndexed { index, (icon, label, value) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(Spacing.s3)) {
                Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.cevi.accent, modifier = Modifier.size(24.dp))
                Column {
                    Text(stringResource(label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value.orEmpty(), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp))
                }
            }
            if (index < rows.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        }
    }
}
