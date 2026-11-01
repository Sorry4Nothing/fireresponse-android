package io.github.sorry4nothing.cevios.auth

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sorry4nothing.cevios.R
import io.github.sorry4nothing.cevios.chat.Profile
import io.github.sorry4nothing.cevios.push.SwitchRow
import io.github.sorry4nothing.cevios.ui.Avatar
import io.github.sorry4nothing.cevios.ui.Header
import io.github.sorry4nothing.cevios.ui.NoticeBanner
import io.github.sorry4nothing.cevios.ui.NoticeKind
import io.github.sorry4nothing.cevios.ui.SlowHint
import io.github.sorry4nothing.cevios.ui.Spinner
import io.github.sorry4nothing.cevios.ui.focusRing
import io.github.sorry4nothing.cevios.ui.theme.Eyebrow
import io.github.sorry4nothing.cevios.ui.theme.Pill
import io.github.sorry4nothing.cevios.ui.theme.Sizes
import io.github.sorry4nothing.cevios.ui.theme.Spacing
import io.github.sorry4nothing.cevios.ui.theme.cevi

/**
 * Wer bin ich, und was kann ich an meinem Konto tun. Es gibt kein «Konto löschen»: Das macht auf Anfrage die Leitung.
 * `onLeave` läuft, wenn der Bildschirm verschwindet (der Hinweis «Passwort gespeichert» gilt nur für diesen Besuch).
 */
@Composable
fun ProfileScreen(
    profile: Profile,
    passwordSaved: Boolean,
    form: FormState,
    imageForm: FormState,
    onPickImage: (Uri) -> Unit,
    onRemoveImage: () -> Unit,
    onChangePassword: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenDeleteAccount: () -> Unit,
    onSignOut: () -> Unit,
    onLeave: () -> Unit,
    lockAvailable: Boolean = false,
    lockEnabled: Boolean = false,
    onLockChange: (Boolean) -> Unit = {},
) {
    DisposableEffect(Unit) { onDispose(onLeave) }
    // Die Systemauswahl braucht keine Berechtigung: Die Person wählt, die App bekommt nur dieses eine Bild.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(onPickImage) }
    var removing by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header(title = stringResource(R.string.profile_title), focusOnTitle = true)
        Column(Modifier.padding(horizontal = Spacing.s4), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            if (passwordSaved) {
                NoticeBanner(NoticeKind.INFO, stringResource(R.string.password_saved), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            PersonRow(
                profile,
                uploading = imageForm.running && !removing,
                onChangeImage = { removing = false; picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            )
            SlowHint(imageForm.running)
            formErrorText(imageForm)?.let { NoticeBanner(NoticeKind.ERROR, it) }
            SectionTitle(R.string.profile_section_app)
            SettingsCard {
                SettingsRow(R.drawable.ic_bell, stringResource(R.string.profile_notifications), onClick = onOpenNotifications, chevron = true)
                SettingsDivider()
                SettingsRow(
                    R.drawable.ic_shield, stringResource(R.string.profile_privacy),
                    onClick = onOpenPrivacy, clickLabel = stringResource(R.string.profile_privacy_open), chevron = true,
                )
                SettingsDivider()
                SettingsRow(
                    R.drawable.ic_question, stringResource(R.string.profile_support),
                    onClick = onOpenSupport, clickLabel = stringResource(R.string.profile_privacy_open), chevron = true,
                )
            }
            SectionTitle(R.string.profile_section_account)
            SettingsCard {
                if (lockAvailable) {
                    SwitchRow(
                        R.drawable.ic_lock, stringResource(R.string.lock_switch), null,
                        lockEnabled, enabled = !form.running, onChange = onLockChange,
                    )
                    SettingsDivider()
                }
                SettingsRow(R.drawable.ic_lock, stringResource(R.string.profile_change_password), onClick = onChangePassword, chevron = true)
                SettingsDivider()
                if (profile.imageUrl != null) {
                    SettingsRow(
                        R.drawable.ic_cross, stringResource(R.string.profile_image_remove),
                        onClick = { removing = true; onRemoveImage() }, loading = imageForm.running && removing,
                    )
                    SettingsDivider()
                }
                SettingsRow(
                    R.drawable.ic_info, stringResource(R.string.profile_delete_account),
                    onClick = onOpenDeleteAccount, clickLabel = stringResource(R.string.profile_privacy_open), chevron = true,
                )
                SettingsDivider()
                SettingsRow(R.drawable.ic_logout, stringResource(R.string.profile_sign_out), onClick = onSignOut, loading = form.running)
            }
            SlowHint(form.running)
            formErrorText(form)?.let { NoticeBanner(NoticeKind.ERROR, it) }
        }
    }
}

@Composable
private fun PersonRow(profile: Profile, uploading: Boolean, onChangeImage: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val change = stringResource(R.string.profile_image_change)
    val saving = stringResource(R.string.profile_image_saving)
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, top = Spacing.s2, end = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Das Bild ist Zierde; der Knopf daneben hat einen Namen und ist 48 dp gross (halb über dem Rand des Bildes).
        Box(Modifier.padding(end = 10.dp, bottom = 10.dp)) {
            Avatar(profile.name, size = 84.dp, imageUrl = profile.imageUrl, initialSize = 28.sp)
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(10.dp, 10.dp)
                    .size(Sizes.touchTarget)
                    .focusRing(Pill)
                    .background(scheme.primary, Pill)
                    .border(4.dp, scheme.surface, Pill)
                    .clip(Pill)
                    .clickable(enabled = !uploading, role = Role.Button, onClick = onChangeImage)
                    .semantics {
                        contentDescription = change
                        if (uploading) stateDescription = saving
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (uploading) Spinner(size = 20.dp, color = scheme.onPrimary)
                else Icon(painterResource(R.drawable.ic_camera), contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(20.dp))
            }
        }
        Column(Modifier.semantics(mergeDescendants = true) {}) {
            Text(
                profile.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            profile.ceviName?.trim()?.takeIf { it.isNotEmpty() }?.let {
                Text(stringResource(R.string.profile_cevi_name, it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            roleLine(profile).takeIf { it.isNotEmpty() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            profile.username?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Rollen und, bei Eltern, die Vornamen der Kinder, getrennt mit « · ». */
@Composable
private fun roleLine(profile: Profile): String {
    val parts = buildList {
        if (profile.roles.any { it.type == "stufenleitung" }) add(stringResource(R.string.role_group_leader))
        else if (profile.roles.any { it.type == "leitung" }) add(stringResource(R.string.role_leader))
        if (profile.children.isNotEmpty()) {
            add(stringResource(R.string.role_parent))
            add(profile.children.mapNotNull { it.firstName.takeIf(String::isNotBlank) }.joinToString(", "))
        }
    }
    return parts.filter { it.isNotEmpty() }.joinToString(" · ")
}

@Composable
private fun SectionTitle(@StringRes text: Int) {
    Text(
        stringResource(text).uppercase(),
        style = Eyebrow,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, top = Spacing.s3, bottom = 2.dp).semantics { heading() },
    )
}

@Composable
internal fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
    ) { content() }
}

@Composable
internal fun SettingsDivider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

/** Eine Zeile mit Icon, Text und rechts ein Wert oder ein Pfeil. Ohne `onClick` ist sie nur Anzeige. */
@Composable
private fun SettingsRow(
    @DrawableRes icon: Int,
    title: String,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    clickLabel: String? = null,
    chevron: Boolean = false,
    loading: Boolean = false,
) {
    val working = stringResource(R.string.working)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.touchTarget)
            .then(
                if (onClick != null) Modifier.focusRing(MaterialTheme.shapes.medium).clickable(enabled = !loading, onClickLabel = clickLabel, role = Role.Button, onClick = onClick)
                    .semantics { if (loading) stateDescription = working }
                else Modifier.semantics(mergeDescendants = true) {},
            )
            .padding(horizontal = 14.dp, vertical = Spacing.s2),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) Spinner(size = 24.dp, color = MaterialTheme.cevi.accent)
        else Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.cevi.accent)
        Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f))
        value?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (chevron) Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
