package io.github.sorry4nothing.cevios.contacts

import android.content.ActivityNotFoundException
import android.content.Intent
import io.github.sorry4nothing.cevios.ui.theme.cevi
import io.github.sorry4nothing.cevios.ui.focusRing
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
import io.github.sorry4nothing.cevios.R
import io.github.sorry4nothing.cevios.ui.Avatar
import io.github.sorry4nothing.cevios.ui.OutlinedPillButton
import io.github.sorry4nothing.cevios.ui.theme.Sizes
import io.github.sorry4nothing.cevios.ui.theme.Spacing

private val CARD = RoundedCornerShape(16.dp)

/**
 * Die Kontaktpersonen in einer Karte, je Zeile Name und Nummer. Ohne Eintrag steht «Stufenleitung»: Dann hat die Stufe
 * keine SL, und der Einzelchat mit der Stufenleitung geht trotzdem.
 */
@Composable
fun ContactCard(contacts: List<Contact>, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, CARD)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CARD)
            .padding(horizontal = 14.dp, vertical = 4.dp),
    ) {
        if (contacts.isEmpty()) {
            Text(
                stringResource(R.string.contact_group_leaders),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.heightIn(min = Sizes.touchTarget).padding(vertical = Spacing.s3),
            )
        } else {
            contacts.forEach { ContactRow(it) }
        }
    }
}

/**
 * Name, darunter die Nummer, rechts «Anrufen». Der Knopf öffnet nur die Telefon-App mit der Nummer (`ACTION_DIAL`): Die
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
        Modifier.fillMaxWidth().heightIn(min = Sizes.touchTarget).padding(vertical = Spacing.s2),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(contact.title, size = 40.dp, imageUrl = contact.imageUrl, initialSize = 16.sp)
        Column(Modifier.weight(1f)) {
            Text(contact.title, style = MaterialTheme.typography.titleMedium)
            contact.fullName?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            contact.phone?.takeIf { it.isNotBlank() }?.let { number ->
                if (canCall) {
                    // Ein Tipp auf die Nummer öffnet die Telefon-App mit der Nummer; der grosse Treffer bleibt der Knopf «Anrufen».
                    Text(
                        number,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.cevi.accent,
                        modifier = Modifier
                            .focusRing(MaterialTheme.shapes.small)
                            .clickable(onClickLabel = stringResource(R.string.contact_call), role = Role.Button, onClick = dial),
                    )
                } else {
                    Text(number, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (canCall && intent != null) {
            val description = stringResource(R.string.contact_call_name, contact.title)
            OutlinedPillButton(
                stringResource(R.string.contact_call),
                onClick = dial,
                modifier = Modifier.semantics { contentDescription = description },
            )
        }
    }
}
