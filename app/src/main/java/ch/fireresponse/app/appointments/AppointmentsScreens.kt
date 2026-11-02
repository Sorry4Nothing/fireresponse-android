package ch.fireresponse.app.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.LocalContext
import android.provider.CalendarContract
import android.content.Intent
import android.content.ActivityNotFoundException
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.Color
import androidx.annotation.DrawableRes
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
import ch.fireresponse.app.R
import ch.fireresponse.app.contacts.ContactCard
import ch.fireresponse.app.contacts.ContactList
import ch.fireresponse.app.net.ApiResult
import ch.fireresponse.app.ui.ChipKind
import ch.fireresponse.app.ui.ErrorState
import ch.fireresponse.app.ui.Header
import ch.fireresponse.app.ui.HeaderButton
import ch.fireresponse.app.ui.LoadingSkeleton
import ch.fireresponse.app.ui.EmptyState
import ch.fireresponse.app.ui.NoticeBanner
import ch.fireresponse.app.ui.NoticeKind
import ch.fireresponse.app.ui.OutlinedPillButton
import ch.fireresponse.app.ui.StatusChip
import ch.fireresponse.app.ui.focusRing
import ch.fireresponse.app.ui.theme.Spacing
import ch.fireresponse.app.ui.theme.fire
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
private val CARD = RoundedCornerShape(16.dp)

@Composable
fun AppointmentListScreen(
    state: ApiResult<AppointmentList>?,
    onOpen: (String) -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Header(
            title = stringResource(R.string.appointments_title),
            action = HeaderButton(R.drawable.ic_refresh, stringResource(R.string.refresh), onRetry),
            focusOnTitle = true,
        )
        when (state) {
            null -> LoadingSkeleton(stringResource(R.string.appointments_loading))
            is ApiResult.Failure -> ErrorState(state.error, onRetry)
            is ApiResult.Ok ->
                if (state.value.appointments.isEmpty()) {
                    EmptyState(
                        R.drawable.ic_calendar,
                        stringResource(R.string.appointments_empty_title),
                        stringResource(R.string.appointments_empty_text),
                        onRetry,
                    )
                } else {
                    Timeline(groupByDay(state.value.appointments), onOpen)
                }
        }
    }
}

/** Die Liste als Zeitleiste: links das Datum an einer Linie, rechts die Programme dieses Tages als Karten. */
@Composable
private fun Timeline(days: List<Day>, onOpen: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(start = Spacing.s3, end = Spacing.s5, bottom = Spacing.s4)) {
        itemsIndexed(days, key = { _, day -> day.appointments.first().id }) { index, day ->
            val isLast = index == days.lastIndex
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                DateColumn(day, withLine = !isLast)
                Column(
                    Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 18.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s2),
                ) {
                    day.appointments.forEach { AppointmentCard(it, onOpen) }
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
            Text(weekdayShort(date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.fire.accent)
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"),
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

/** Eine Zeile Stand auf der Karte: Symbol und Satz, nie nur Farbe. */
private data class StatusLine(@DrawableRes val icon: Int, val text: String, val color: Color)

/**
 * Der Stand je eigenem Teilnehmenden («Noah kommt», «Lena kommt nicht», «Zusage für Lena offen») und, für Leitende, der eigene
 * («Du bist dabei», «Du bist nicht dabei», «Anmeldung offen»). Bei einer abgesagten Übung steht schon «Abgesagt» da.
 */
@Composable
private fun statusLines(appointment: Appointment): List<StatusLine> {
    if (appointment.cancelled) return emptyList()
    val scheme = MaterialTheme.colorScheme
    val colors = MaterialTheme.fire
    val participants = appointment.participants.map {
        when (it.status) {
            "ja" -> StatusLine(R.drawable.ic_check, stringResource(R.string.participant_status_yes, it.firstName), colors.onSuccessContainer)
            "nein" -> StatusLine(R.drawable.ic_cross, stringResource(R.string.participant_status_no, it.firstName), scheme.onErrorContainer)
            else -> StatusLine(R.drawable.ic_question, stringResource(R.string.participant_status_open, it.firstName), scheme.onSurfaceVariant)
        }
    }
    val own = appointment.ownRegistration?.let {
        when (it) {
            "ja" -> StatusLine(R.drawable.ic_check, stringResource(R.string.own_status_yes), colors.onSuccessContainer)
            "nein" -> StatusLine(R.drawable.ic_cross, stringResource(R.string.own_status_no), scheme.onErrorContainer)
            else -> StatusLine(R.drawable.ic_question, stringResource(R.string.own_status_open), scheme.onSurfaceVariant)
        }
    }
    return participants + listOfNotNull(own)
}

/**
 * Die Karte einer Übung: Zeit in `accent`, Titel, Ort, darunter der Stand. Die ganze Karte ist ein Klickziel; TalkBack liest einen
 * vollständigen Satz mit Datum («Samstag, 14. November, 14:00–17:00, Schläuche kuppeln, Feuerwehrmagazin, Zusage für Lena offen»).
 */
@Composable
private fun AppointmentCard(appointment: Appointment, onOpen: (String) -> Unit) {
    val headerLine = listTime(appointment)
    val lines = statusLines(appointment)
    val chips = listOfNotNull(
        stringResource(R.string.status_cancelled).takeIf { appointment.cancelled },
        stringResource(R.string.status_review).takeIf { appointment.inReview },
        stringResource(R.string.status_unassigned).takeIf { appointment.unassigned },
    )
    val spoken = (
        listOfNotNull(appointment.startDay()?.let(::spokenDay), headerLine, appointment.title, appointment.location) +
            chips + lines.map { it.text }
        ).filter { it.isNotBlank() }.joinToString(", ")
    Box(
        Modifier
            .fillMaxWidth()
            .focusRing(CARD)
            .clip(CARD)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClickLabel = stringResource(R.string.appointment_open)) { onOpen(appointment.id) }
            .semantics { contentDescription = spoken },
    ) {
        Column(
            Modifier.clearAndSetSemantics {}.padding(horizontal = Spacing.s4, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                headerLine,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                color = MaterialTheme.fire.accent,
            )
            Text(appointment.title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 24.sp))
            appointment.location?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            lines.forEachIndexed { index, line ->
                Row(
                    Modifier.padding(top = if (index == 0) 6.dp else 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(line.icon), contentDescription = null, tint = line.color, modifier = Modifier.size(16.dp))
                    Text(line.text, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold), color = line.color)
                }
            }
            AppointmentStatus(appointment, Modifier.padding(top = 6.dp))
        }
    }
}

/**
 * Abgesagt, «wird noch geprüft» oder «nicht zugeordnet» (nur Leitungen sehen solche Programme): als Etikett mit Icon und
 * Wort. Ein aktives, zugeordnetes Programm braucht keins.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppointmentStatus(appointment: Appointment, modifier: Modifier = Modifier) {
    if (!appointment.cancelled && !appointment.inReview && !appointment.unassigned) return
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.s2), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        if (appointment.cancelled) StatusChip(ChipKind.NO, stringResource(R.string.status_cancelled))
        if (appointment.inReview) StatusChip(ChipKind.REVIEW, stringResource(R.string.status_review))
        if (appointment.unassigned) StatusChip(ChipKind.UNASSIGNED, stringResource(R.string.status_unassigned))
    }
}

/** «14:00–17:00», bei mehreren Tagen der Beginn, bei ganztägigen «Ganztägig». */
@Composable
private fun listTime(appointment: Appointment): String = when (val time = appointment.time()) {
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
fun AppointmentDetailScreen(
    state: ApiResult<Appointment>?,
    rsvp: RsvpState,
    canAssign: Boolean,
    canSeeRegistrations: Boolean,
    contacts: ApiResult<ContactList>?,
    onParticipant: (participantId: String, attending: Boolean) -> Unit,
    onLeader: (attending: Boolean) -> Unit,
    onAssign: () -> Unit,
    onRegistrations: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val back = HeaderButton(R.drawable.ic_back, stringResource(R.string.back_to_appointments), onBack)
    Column(Modifier.fillMaxSize()) {
        when (state) {
            null -> {
                Header(title = null, back = back)
                LoadingSkeleton(stringResource(R.string.appointment_loading))
            }
            is ApiResult.Failure -> {
                Header(title = null, back = back)
                ErrorState(state.error, onRetry, onToAppointments = onBack)
            }
            is ApiResult.Ok -> AppointmentContent(state.value, rsvp, canAssign, canSeeRegistrations, contacts, back, onParticipant, onLeader, onAssign, onRegistrations)
        }
    }
}

@Composable
private fun AppointmentContent(
    appointment: Appointment,
    rsvp: RsvpState,
    canAssign: Boolean,
    canSeeRegistrations: Boolean,
    contacts: ApiResult<ContactList>?,
    back: HeaderButton,
    onParticipant: (participantId: String, attending: Boolean) -> Unit,
    onLeader: (attending: Boolean) -> Unit,
    onAssign: () -> Unit,
    onRegistrations: () -> Unit,
) {
    val date = appointment.startDay()?.let { "${weekdayShort(it)} ${dayAndMonth(it)}" }
    val context = LocalContext.current
    val calendar = calendarIntent(appointment)
    val toCalendar = stringResource(R.string.add_to_calendar)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header(
            title = appointment.title,
            eyebrow = date,
            back = back,
            action = calendar?.let { intent ->
                HeaderButton(R.drawable.ic_calendar_plus, toCalendar) {
                    try {
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        // Keine Kalender-App auf dem Gerät: Der Knopf tut dann nichts.
                    }
                }
            },
            focusOnTitle = true,
        )
        Column(Modifier.padding(horizontal = Spacing.s5), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            AppointmentStatus(appointment)
            Tiles(appointment)
            Facts(appointment)
            ContactSection(appointment, contacts)
            RegistrationSection(appointment, rsvp, onParticipant, onLeader)
            LeaderSection(appointment, canAssign, canSeeRegistrations, onAssign, onRegistrations)
            Box(Modifier.height(Spacing.s4))
        }
    }
}

/** Die Kontaktpersonen der Übung (wer abgesagt hat, fehlt), je eine Karte. Ein Fehler beim Laden zeigt nichts. */
@Composable
private fun ContactSection(appointment: Appointment, contacts: ApiResult<ContactList>?) {
    if (appointment.unassigned || contacts !is ApiResult.Ok) return
    ContactCard(contacts.value.contacts)
}

/** Der Termin als Eintrag für die Kalender-App der Person; `null`, solange Zeit oder Tag fehlen. */
private fun calendarIntent(appointment: Appointment): Intent? {
    val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.Events.TITLE, appointment.title)
        .putExtra(CalendarContract.Events.EVENT_LOCATION, appointment.location)
    return when (val time = appointment.time()) {
        is Timing.Timed -> intent
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, time.start.toInstant().toEpochMilli())
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, time.end.toInstant().toEpochMilli())
        is Timing.AllDay -> intent
            .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, time.from.atStartOfDay(SWISS_ZONE).toInstant().toEpochMilli())
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, time.until.atStartOfDay(SWISS_ZONE).toInstant().toEpochMilli())
        null -> null
    }
}

/**
 * Aktionen für Leitende: die Anmeldungen ansehen (Admins sowie Leitende und SL der Stufen) und, nur für Admins und
 * Stufenleitungen, Stufen und Sichtbarkeit setzen.
 */
@Composable
private fun LeaderSection(
    appointment: Appointment,
    canAssign: Boolean,
    canSeeRegistrations: Boolean,
    onAssign: () -> Unit,
    onRegistrations: () -> Unit,
) {
    if (!canAssign && !canSeeRegistrations) return
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
        Text(
            stringResource(R.string.section_leaders),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, lineHeight = 24.sp),
            modifier = Modifier.padding(top = 6.dp).semantics { heading() },
        )
        if (canAssign && appointment.unassigned) NoticeBanner(NoticeKind.INFO, stringResource(R.string.unassigned_notice))
        if (canSeeRegistrations) {
            OutlinedPillButton(stringResource(R.string.registrations_action), onRegistrations, Modifier.fillMaxWidth(), icon = R.drawable.ic_group)
        }
        if (canAssign) OutlinedPillButton(stringResource(R.string.assignment_action), onAssign, Modifier.fillMaxWidth())
    }
}

/** Zwei Kacheln, die Eltern am häufigsten suchen: wann und wo man sich trifft, wann und wo es endet. */
@Composable
private fun Tiles(appointment: Appointment) {
    val closing = appointment.closingTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
    val meeting = parseMeetingPoint(appointment.meetingPoint)
    // Nennt die Webseite eine Zeit, gilt sie; sonst der Beginn aus dem Kalender.
    val meetingTime = meeting.time ?: when (val time = appointment.time()) {
        is Timing.Timed -> TIME_FORMAT.format(time.start)
        is Timing.AllDay -> stringResource(R.string.time_list_all_day)
        null -> null
    }
    val meetingPoint = meeting.place
    val closingLocation = appointment.closingLocation?.takeIf { it.isNotBlank() }
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
private fun Facts(appointment: Appointment) {
    val rows = listOf(
        Triple(R.drawable.ic_location, R.string.fact_location, appointment.googleLocation?.takeIf { it.isNotBlank() }),
        Triple(R.drawable.ic_bring_along, R.string.section_bring_along, appointment.bringAlong?.takeIf { it.isNotBlank() }),
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
                Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.fire.accent, modifier = Modifier.size(24.dp))
                Column {
                    Text(stringResource(label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value.orEmpty(), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp))
                }
            }
            if (index < rows.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        }
    }
}
