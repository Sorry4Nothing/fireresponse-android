package ch.fireresponse.app.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import ch.fireresponse.app.ui.InitialsBadge
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.fireresponse.app.R
import ch.fireresponse.app.ui.ChipKind
import ch.fireresponse.app.ui.NoticeKind
import ch.fireresponse.app.ui.NoticeBanner
import ch.fireresponse.app.ui.SlowHint
import ch.fireresponse.app.ui.Spinner
import ch.fireresponse.app.ui.StatusChip
import ch.fireresponse.app.ui.focusRing
import ch.fireresponse.app.ui.theme.Spacing
import ch.fireresponse.app.ui.theme.Sizes
import ch.fireresponse.app.ui.theme.Pill
import ch.fireresponse.app.ui.theme.fire

/**
 * Ja/Nein für die eigenen Teilnehmenden und, falls vorhanden, für die eigene Anmeldung als Leitung.
 * Die Pillen erscheinen nur, solange die API `attendanceOpen` meldet; sonst steht nur der Stand da.
 */
@Composable
fun RegistrationSection(
    appointment: Appointment,
    rsvp: RsvpState,
    onParticipant: (participantId: String, attending: Boolean) -> Unit,
    onLeader: (attending: Boolean) -> Unit,
) {
    val leader = appointment.ownRegistration
    if (appointment.participants.isEmpty() && leader == null) return

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
        Text(
            stringResource(if (appointment.participants.isEmpty()) R.string.section_leader_registration else R.string.section_participants),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, lineHeight = 24.sp),
            modifier = Modifier.padding(top = 6.dp).semantics { heading() },
        )
        if (!appointment.attendanceOpen) NoticeBanner(NoticeKind.INFO, stringResource(R.string.registration_closed))

        appointment.participants.forEach { participant ->
            RsvpRow(
                name = participant.firstName,
                answer = participant.status,
                isOpen = appointment.attendanceOpen,
                locked = rsvp.running,
                pending = rsvp.pendingAttending.takeIf { rsvp.pendingKey == participant.id },
                sentAt = rsvp.sentAt[participant.id],
                onRsvp = { onParticipant(participant.id, it) },
            )
        }
        if (leader != null) {
            RsvpRow(
                name = stringResource(R.string.message_you),
                answer = leader.takeIf { it == "ja" || it == "nein" },
                isOpen = appointment.attendanceOpen,
                locked = rsvp.running,
                pending = rsvp.pendingAttending.takeIf { rsvp.pendingKey == LEADER_KEY },
                sentAt = rsvp.sentAt[LEADER_KEY],
                onRsvp = onLeader,
            )
        }

        SlowHint(rsvp.running)
        rsvp.error?.let { NoticeBanner(NoticeKind.ERROR, stringResource(it.text)) }
    }
}

/** Name und Stand links, daneben die zwei Pillen Ja und Nein. */
@Composable
private fun RsvpRow(
    name: String,
    answer: String?,
    isOpen: Boolean,
    locked: Boolean,
    pending: Boolean?,
    sentAt: String?,
    onRsvp: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.fire
    val scheme = MaterialTheme.colorScheme
    val (statusText, statusColor) = when {
        pending != null -> stringResource(R.string.registration_saving) to scheme.onSurfaceVariant
        sentAt != null -> stringResource(R.string.rsvp_sent, sentAt) to colors.onSuccessContainer
        answer == "ja" -> stringResource(R.string.rsvp_yes) to colors.onSuccessContainer
        answer == "nein" -> stringResource(R.string.rsvp_no) to scheme.onErrorContainer
        else -> stringResource(R.string.rsvp_pending) to scheme.onSurfaceVariant
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        InitialsBadge(name)
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            // Ist die Anmeldung geschlossen, sagt schon das Etikett rechts denselben Stand: nicht doppelt zeigen.
            if (isOpen) {
                Text(
                    statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                    modifier = Modifier.semantics { if (sentAt != null || pending != null) liveRegion = LiveRegionMode.Polite },
                )
            }
        }
        if (isOpen) {
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                RsvpPill(attending = true, selected = answer == "ja", active = !locked, loading = pending == true, name = name) { onRsvp(true) }
                RsvpPill(attending = false, selected = answer == "nein", active = !locked, loading = pending == false, name = name) { onRsvp(false) }
            }
        } else {
            when (answer) {
                "ja" -> StatusChip(ChipKind.YES, stringResource(R.string.rsvp_yes))
                "nein" -> StatusChip(ChipKind.NO, stringResource(R.string.rsvp_no))
                else -> StatusChip(ChipKind.PENDING, stringResource(R.string.rsvp_pending))
            }
        }
    }
}

/**
 * Eine Pille: offen mit Rand, gewählt Ja grün mit dickem Häkchen, Nein rot mit Kreuz, beide mit 2-dp-Rand.
 * Der Zustand steht in Form, Icon und Wort, nicht nur in der Farbe. TalkBack: «Vorname, Ja, nicht ausgewählt».
 */
@Composable
private fun RsvpPill(attending: Boolean, selected: Boolean, active: Boolean, loading: Boolean, name: String, onClick: () -> Unit) {
    val colors = MaterialTheme.fire
    val scheme = MaterialTheme.colorScheme
    val contentColor: Color
    val surfaceColor: Color
    val icon: Int
    when {
        selected && attending -> { contentColor = colors.onSuccessContainer; surfaceColor = colors.successContainer; icon = R.drawable.ic_check_selected }
        selected -> { contentColor = scheme.onErrorContainer; surfaceColor = scheme.errorContainer; icon = R.drawable.ic_cross_selected }
        else -> { contentColor = scheme.onSurface; surfaceColor = Color.Transparent; icon = if (attending) R.drawable.ic_check else R.drawable.ic_cross }
    }
    val borderSpec = if (selected) 2.dp to contentColor else 1.dp to scheme.outline
    val word = stringResource(if (attending) R.string.button_yes else R.string.button_no)
    Row(
        Modifier
            .height(Sizes.touchTarget)
            .focusRing(Pill)
            .clip(Pill)
            .background(surfaceColor)
            .border(borderSpec.first, borderSpec.second, Pill)
            .selectable(selected = selected, enabled = active, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 18.dp)
            .clearAndSetSemantics { contentDescription = "$word, $name" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) Spinner(color = contentColor) else Icon(painterResource(icon), contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(word, style = MaterialTheme.typography.labelLarge, color = contentColor, fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold)
    }
}
