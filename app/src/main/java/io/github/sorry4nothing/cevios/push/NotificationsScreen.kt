package io.github.sorry4nothing.cevios.push

import android.content.Intent
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.sorry4nothing.cevios.R
import io.github.sorry4nothing.cevios.auth.FormState
import io.github.sorry4nothing.cevios.auth.SettingsCard
import io.github.sorry4nothing.cevios.auth.SettingsDivider
import io.github.sorry4nothing.cevios.auth.formErrorText
import io.github.sorry4nothing.cevios.ui.Header
import io.github.sorry4nothing.cevios.ui.HeaderButton
import io.github.sorry4nothing.cevios.ui.NoticeBanner
import io.github.sorry4nothing.cevios.ui.NoticeKind
import io.github.sorry4nothing.cevios.ui.OutlinedPillButton
import io.github.sorry4nothing.cevios.ui.SlowHint
import io.github.sorry4nothing.cevios.ui.focusRing
import io.github.sorry4nothing.cevios.ui.theme.Sizes
import io.github.sorry4nothing.cevios.ui.theme.Spacing
import io.github.sorry4nothing.cevios.ui.theme.cevi

/**
 * Welche Mitteilungen die Person bekommt: neue Chat-Nachrichten, Mitteilungen und Erinnerungen, und für Leitende die An- und
 * Abmeldungen für Programme. Die Schalter gelten beim
 * Server, nicht auf diesem Gerät; ob das Gerät der App Mitteilungen zeigen lässt, entscheidet das System, und der
 * Bildschirm sagt es, wenn es sie sperrt.
 */
@Composable
fun NotificationsScreen(
    switches: PushViewModel.Switches,
    form: FormState,
    onChat: (Boolean) -> Unit,
    onPrograms: (Boolean) -> Unit,
    onRegistrations: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    // Nach den Einstellungen des Systems kommt die Person zurück: dann gilt der neue Stand.
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val deviceAllows = remember(lifecycleState) { Notifications.enabled(context) }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())) {
        Header(
            title = stringResource(R.string.notifications_title),
            back = HeaderButton(R.drawable.ic_back, stringResource(R.string.back_to_profile), onBack),
            titleStyle = MaterialTheme.typography.headlineMedium,
            focusOnTitle = true,
        )
        Column(Modifier.padding(horizontal = Spacing.s4), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
            if (!deviceAllows) {
                NoticeBanner(NoticeKind.WARNING, stringResource(R.string.notifications_device_off))
                OutlinedPillButton(
                    stringResource(R.string.notifications_open_settings),
                    {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                    },
                    Modifier.fillMaxWidth(),
                )
            }
            SettingsCard {
                SwitchRow(
                    R.drawable.ic_chat, stringResource(R.string.notifications_chat), stringResource(R.string.notifications_chat_hint),
                    switches.chat, enabled = !form.running, onChange = onChat,
                )
                SettingsDivider()
                SwitchRow(
                    R.drawable.ic_calendar, stringResource(R.string.notifications_programs), stringResource(R.string.notifications_programs_hint),
                    switches.programs, enabled = !form.running, onChange = onPrograms,
                )
                switches.registrations?.let {
                    SettingsDivider()
                    SwitchRow(
                        R.drawable.ic_group, stringResource(R.string.notifications_registrations),
                        stringResource(R.string.notifications_registrations_hint),
                        it, enabled = !form.running, onChange = onRegistrations,
                    )
                }
            }
            SlowHint(form.running)
            formErrorText(form)?.let { NoticeBanner(NoticeKind.ERROR, it) }
            Text(
                stringResource(R.string.notifications_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
    }
}

/** Eine ganze Zeile ist der Schalter (mindestens 48 dp); der Zustand steht in der Stellung und im Haken, nie nur in der Farbe. */
@Composable
internal fun SwitchRow(
    @DrawableRes icon: Int,
    title: String,
    hint: String?,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.touchTarget)
            .focusRing(MaterialTheme.shapes.medium)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 14.dp, vertical = Spacing.s2),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.cevi.accent)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
            hint?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            thumbContent = if (checked) {
                { Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else {
                null
            },
        )
    }
}
