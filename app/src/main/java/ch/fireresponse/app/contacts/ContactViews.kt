package ch.fireresponse.app.contacts

import android.content.ActivityNotFoundException
import android.content.Intent
import ch.fireresponse.app.ui.theme.fire
import ch.fireresponse.app.ui.focusRing
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import ch.fireresponse.app.ui.theme.Pill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import ch.fireresponse.app.R
import ch.fireresponse.app.ui.Avatar
import ch.fireresponse.app.ui.OutlinedPillButton
import ch.fireresponse.app.ui.RankMark
import ch.fireresponse.app.ui.theme.Sizes
import ch.fireresponse.app.ui.theme.Spacing

private val CARD = RoundedCornerShape(16.dp)

/**
 * Die Kontaktpersonen, je eine Karte: Initialen, «Kontaktperson», Name und rechts der runde Knopf «Anrufen». Ohne Eintrag steht
 * «Kommando»: Dann hat die Einheit keine Kontaktperson.
 */
@Composable
fun ContactCard(contacts: List<Contact>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        if (contacts.isEmpty()) {
            Text(
                stringResource(R.string.contact_commanders),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.heightIn(min = Sizes.touchTarget).padding(vertical = Spacing.s3),
            )
        } else {
            contacts.forEach { ContactRow(it) }
        }
    }
}

/**
 * Name, darüber «Kontaktperson», rechts «Anrufen». Der Knopf öffnet nur die Telefon-App mit der Nummer (`ACTION_DIAL`): Die
 * Person wählt selbst, die App braucht dafür keine Berechtigung. Ohne Nummer oder ohne Telefon-App fehlt der Knopf.
 */
@Composable
private fun ContactRow(contact: Contact) {
    val context = LocalContext.current
    val intent = remember(contact.phone) {
        contact.phone?.takeIf { it.isNotBlank() }?.let { Intent(Intent.ACTION_DIAL, PhoneRules.dialUri(it).toUri()) }
    }
    val canCall = intent != null && intent.resolveActivity(context.packageManager) != null
    val dial = {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Die Telefon-App ist zwischen Prüfung und Klick verschwunden: Es gibt nichts zu tun.
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, CARD)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CARD)
            .padding(start = 14.dp, top = 10.dp, end = 8.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(contact.name, size = 40.dp, imageUrl = contact.imageUrl, initialSize = 14.sp)
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
            Text(
                stringResource(R.string.contact_person),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(contact.name, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f, fill = false))
                contact.badge?.let { RankMark(it, height = 24.dp) }
            }
        }
        if (canCall && intent != null) {
            val description = stringResource(R.string.contact_call_name, contact.name)
            Box(
                Modifier
                    .size(Sizes.touchTarget)
                    .focusRing(Pill)
                    .clip(Pill)
                    .background(MaterialTheme.colorScheme.surfaceContainer, Pill)
                    .clickable(role = Role.Button, onClick = dial)
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_phone), contentDescription = null, tint = MaterialTheme.fire.accent)
            }
        }
    }
}
