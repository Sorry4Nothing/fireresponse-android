package io.github.sorry4nothing.cevios.auth

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.sorry4nothing.cevios.R
import io.github.sorry4nothing.cevios.ui.Header
import io.github.sorry4nothing.cevios.ui.HeaderButton
import io.github.sorry4nothing.cevios.ui.LabeledTextField
import io.github.sorry4nothing.cevios.ui.NoticeBanner
import io.github.sorry4nothing.cevios.ui.NoticeKind
import io.github.sorry4nothing.cevios.ui.PrimaryButton
import io.github.sorry4nothing.cevios.ui.SlowHint
import io.github.sorry4nothing.cevios.ui.TextActionButton
import io.github.sorry4nothing.cevios.ui.focusRing
import io.github.sorry4nothing.cevios.push.SwitchRow
import io.github.sorry4nothing.cevios.ui.rememberMotionEnabled
import io.github.sorry4nothing.cevios.ui.theme.Eyebrow
import io.github.sorry4nothing.cevios.ui.theme.Pill
import io.github.sorry4nothing.cevios.ui.theme.Sizes
import io.github.sorry4nothing.cevios.ui.theme.Spacing
import io.github.sorry4nothing.cevios.ui.theme.cevi

/** Das Logo füllt den Platz über dem Text, höchstens so gross wie im Entwurf; unter der Mindestgrösse entfällt es. */
private val LOGO_SIZE = 420.dp
private val LOGO_MIN_SIZE = 200.dp

/** So lange baut sich das Logo beim Start auf, und so weit gleitet es von rechts herein. */
private const val LOGO_REVEAL_MS = 700
private val LOGO_SLIDE = 80.dp

private val CardShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

/** Seite mit dem Logo hinter dem Titel oben und einem Block unten. Scrollt, wenn der Platz bei grosser Schrift nicht reicht. */
@Composable
private fun LogoPage(
    top: @Composable ColumnScope.() -> Unit,
    bottom: @Composable ColumnScope.() -> Unit,
    animateLogo: Boolean = false,
) {
    // Beim Start gleitet das Logo von rechts herein und blendet auf; mit ausgeschalteten Animationen steht es gleich da.
    val motionEnabled = rememberMotionEnabled()
    val reveal = remember { Animatable(if (animateLogo && motionEnabled) 0f else 1f) }
    LaunchedEffect(Unit) { if (reveal.value < 1f) reveal.animateTo(1f, tween(LOGO_REVEAL_MS, easing = FastOutSlowInEasing)) }
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val fullHeight = maxHeight
        var areaHeight by remember { mutableIntStateOf(0) }
        var textHeight by remember { mutableIntStateOf(0) }
        // Das Logo steht nur, wo es dem Text nicht in die Quere kommt: Bei offener Tastatur oder grosser Schrift schrumpft es oder entfällt.
        val density = LocalDensity.current
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().heightIn(min = fullHeight)) {
                Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().onSizeChanged { areaHeight = it.height }) {
                    val logoSize = with(density) { (areaHeight - textHeight).toDp() }.coerceAtMost(LOGO_SIZE)
                    if (logoSize >= LOGO_MIN_SIZE) {
                        Image(
                            painterResource(R.drawable.ic_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = logoSize * (110f / 420f), y = logoSize * (8f / 420f))
                                .size(logoSize)
                                .graphicsLayer {
                                    alpha = reveal.value
                                    translationX = (1f - reveal.value) * LOGO_SLIDE.toPx()
                                },
                        )
                    }
                    Column(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .onSizeChanged { textHeight = it.height }
                            .padding(start = 28.dp, end = 28.dp, bottom = 28.dp, top = Spacing.s8),
                        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
                        content = top,
                    )
                }
                bottom()
            }
        }
    }
}

/** Zeile über dem Titel: Name der Abteilung in Akzentfarbe. */
@Composable
private fun DepartmentEyebrow() {
    Text(stringResource(R.string.department).uppercase(), style = Eyebrow, color = MaterialTheme.cevi.accent)
}

/** Startbildschirm, solange die App den Server fragt, ob eine Sitzung besteht. */
@Composable
fun StartScreen() {
    LogoPage(
        top = {
            DepartmentEyebrow()
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.start_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.s5).semantics { liveRegion = LiveRegionMode.Polite },
            )
        },
        bottom = { Spacer(Modifier.height(72.dp)) },
        animateLogo = true,
    )
}

/**
 * Anmelden mit Benutzername und Passwort. Das Passwort bleibt nur im Bildschirm (`remember`, nicht `rememberSaveable`):
 * Es landet nie in gespeichertem Zustand.
 */
@Composable
fun LoginScreen(form: FormState, onSignIn: (String, String) -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val ready = username.isNotBlank() && password.isNotEmpty()
    val submit = { if (ready && !form.running) onSignIn(username, password) }
    LogoPage(
        top = {
            DepartmentEyebrow()
            Text(
                stringResource(R.string.login_title),
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.semantics { heading() },
            )
        },
        bottom = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CardShape)
                    .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                LabeledTextField(
                    label = stringResource(R.string.login_username),
                    value = username,
                    onValueChange = { username = it },
                    maxChars = USERNAME_MAX_LENGTH,
                    keyboardType = KeyboardType.Ascii,
                    autofill = ContentType.Username,
                    imeAction = ImeAction.Next,
                    enabled = !form.running,
                )
                LabeledTextField(
                    label = stringResource(R.string.login_password),
                    value = password,
                    onValueChange = { password = it },
                    maxChars = PasswordRules.MAX_LENGTH,
                    keyboardType = KeyboardType.Password,
                    masked = !visible,
                    autofill = ContentType.Password,
                    imeAction = ImeAction.Done,
                    onImeAction = submit,
                    enabled = !form.running,
                    trailing = { ShowPasswordSwitch(visible) { visible = it } },
                )
                FormStatus(form)
                PrimaryButton(
                    stringResource(R.string.login_submit),
                    onClick = submit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = ready,
                    loading = form.running,
                    loadingText = stringResource(R.string.login_running),
                )
                Text(
                    stringResource(R.string.login_forgot),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

private const val USERNAME_MAX_LENGTH = 64

/** Zustand eines Formulars unter den Feldern: Was läuft, zeigt der Knopf; dauert es lange, sagt es ein Hinweis; ein Fehler kommt sofort und mit Icon. */
@Composable
private fun FormStatus(form: FormState) {
    SlowHint(form.running)
    formErrorText(form)?.let { NoticeBanner(NoticeKind.ERROR, it) }
}

/** «Passwort anzeigen» als Schalter: Der Screenreader sagt an, ob das Passwort gerade sichtbar ist. */
@Composable
private fun ShowPasswordSwitch(visible: Boolean, onChange: (Boolean) -> Unit) {
    val label = stringResource(R.string.password_show)
    Box(
        Modifier
            .size(Sizes.touchTarget)
            .focusRing(Pill)
            .clip(Pill)
            .toggleable(value = visible, role = Role.Switch, onValueChange = onChange)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(if (visible) R.drawable.ic_eye_off else R.drawable.ic_eye),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Passwort ändern: der Pflichtwechsel nach dem Startpasswort (`first`, ohne Zurück) und der freiwillige aus dem Profil.
 * Der Server verlangt auch das bisherige Passwort. Es gibt kein Feld zum Wiederholen: «Passwort anzeigen» ersetzt es
 * (Checkliste 9.7).
 */
@Composable
fun PasswordScreen(
    first: Boolean,
    form: FormState,
    onSave: (String?, String) -> Unit,
    onBack: (() -> Unit)? = null,
    /** Die App kennt das Startpasswort vom Login: Dann entfällt das Feld dafür. */
    startPasswordKnown: Boolean = false,
) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var currentVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    val askCurrent = !(first && startPasswordKnown)
    val bisher = if (askCurrent) current else null
    val ready = PasswordRules.canSave(bisher, new)
    val save = { if (ready && !form.running) onSave(bisher, new) }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Header(
                title = stringResource(if (first) R.string.password_title_first else R.string.password_title),
                eyebrow = stringResource(if (first) R.string.password_eyebrow_first else R.string.password_eyebrow),
                back = onBack?.let { HeaderButton(R.drawable.ic_back, stringResource(R.string.back_to_profile), it) },
                titleStyle = MaterialTheme.typography.headlineMedium,
                focusOnTitle = true,
            )
            Column(Modifier.padding(horizontal = Spacing.s5), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                if (first) {
                    Text(
                        stringResource(R.string.password_intro_first),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (askCurrent) {
                    LabeledTextField(
                        label = stringResource(if (first) R.string.password_current_first else R.string.password_current),
                        value = current,
                        onValueChange = { current = it },
                        maxChars = PasswordRules.MAX_LENGTH,
                        keyboardType = KeyboardType.Password,
                        masked = !currentVisible,
                        autofill = ContentType.Password,
                        imeAction = ImeAction.Next,
                        enabled = !form.running,
                        trailing = { ShowPasswordSwitch(currentVisible) { currentVisible = it } },
                    )
                }
                LabeledTextField(
                    label = stringResource(R.string.password_new),
                    value = new,
                    onValueChange = { new = it },
                    maxChars = PasswordRules.MAX_LENGTH,
                    keyboardType = KeyboardType.Password,
                    masked = !newVisible,
                    autofill = ContentType.NewPassword,
                    imeAction = ImeAction.Done,
                    onImeAction = save,
                    enabled = !form.running,
                    trailing = { ShowPasswordSwitch(newVisible) { newVisible = it } },
                )
                PasswordRule(R.string.password_rule_length, PasswordRules.longEnough(new))
                if (askCurrent) PasswordRule(R.string.password_rule_different, PasswordRules.different(current, new))
                FormStatus(form)
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CardShape)
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp),
        ) {
            PrimaryButton(
                stringResource(R.string.password_save), onClick = save, modifier = Modifier.fillMaxWidth(), enabled = ready,
                loading = form.running, loadingText = stringResource(R.string.saving),
            )
        }
    }
}

/** Eine Regel für das Passwort: Haken oder Kreuz mit Text, nie nur Farbe. Der Screenreader hört «erfüllt» oder «noch nicht erfüllt». */
@Composable
private fun PasswordRule(@StringRes text: Int, met: Boolean) {
    val words = stringResource(text)
    val state = stringResource(if (met) R.string.password_rule_met else R.string.password_rule_open)
    Row(
        Modifier.semantics(mergeDescendants = true) { stateDescription = state },
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(if (met) R.drawable.ic_check else R.drawable.ic_cross),
            contentDescription = null,
            tint = if (met) MaterialTheme.cevi.onSuccessContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            words,
            style = MaterialTheme.typography.bodyMedium,
            color = if (met) MaterialTheme.cevi.onSuccessContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Gesperrt: Die Person weist sich mit Fingerabdruck oder Gesicht aus. Die Abfrage startet von selbst; der Knopf fragt noch einmal,
 * und «Mit Passwort anmelden» beendet die Sitzung (der Ausweg, wenn die Biometrie nicht geht).
 */
@Composable
fun LockedScreen(form: FormState, onUnlock: () -> Unit, onUsePassword: () -> Unit) {
    LaunchedEffect(Unit) { onUnlock() }
    LogoPage(
        top = {
            DepartmentEyebrow()
            Text(
                stringResource(R.string.lock_title),
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.lock_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.s3),
            )
        },
        bottom = {
            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                FormStatus(form)
                PrimaryButton(stringResource(R.string.lock_unlock), onClick = onUnlock, modifier = Modifier.fillMaxWidth())
                TextActionButton(stringResource(R.string.lock_use_password), onUsePassword, Modifier.fillMaxWidth())
            }
        },
    )
}

/**
 * Willkommen beim ersten Start: Die Person nimmt die Datenschutzerklärung zur Kenntnis (Kenntnisnahme, keine Einwilligung).
 * Der Text der Erklärung steht auf dem Backend und öffnet im Browser.
 */
@Composable
fun WelcomeScreen(
    name: String,
    passwordSaved: Boolean,
    form: FormState,
    onReadPrivacy: () -> Unit,
    onAccept: () -> Unit,
    /** Das Gerät kann Fingerabdruck oder Gesicht; dann bietet der Schalter das Entsperren an (nur direkt nach dem eigenen Passwort). */
    lockAvailable: Boolean = false,
    lockEnabled: Boolean = false,
    onLockChange: (Boolean) -> Unit = {},
) {
    var acknowledged by rememberSaveable { mutableStateOf(false) }
    val ready = acknowledged
    LogoPage(
        top = {
            if (passwordSaved) {
                Row(
                    Modifier
                        .background(MaterialTheme.cevi.successContainer, Pill)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.ic_check), null, tint = MaterialTheme.cevi.onSuccessContainer, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.password_saved), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.cevi.onSuccessContainer)
                }
            }
            DepartmentEyebrow()
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { focus.requestFocus() }
            Text(
                stringResource(R.string.welcome_title, firstName(name)),
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.focusRequester(focus).focusTarget().semantics { heading() },
            )
            Column(Modifier.padding(top = Spacing.s3), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                WelcomePoint(R.drawable.ic_calendar, R.string.welcome_programs, R.string.welcome_programs_text)
                WelcomePoint(R.drawable.ic_check, R.string.welcome_rsvp, R.string.welcome_rsvp_text)
                WelcomePoint(R.drawable.ic_chat, R.string.welcome_chat, R.string.welcome_chat_text)
            }
        },
        bottom = {
            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                if (passwordSaved && lockAvailable) {
                    SwitchRow(
                        R.drawable.ic_lock, stringResource(R.string.lock_switch), null,
                        lockEnabled, enabled = !form.running, onChange = onLockChange,
                    )
                }
                TextActionButton(stringResource(R.string.welcome_privacy_read), onReadPrivacy)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = Sizes.touchTarget)
                        .focusRing(MaterialTheme.shapes.small)
                        .toggleable(value = acknowledged, enabled = !form.running, role = Role.Checkbox, onValueChange = { acknowledged = it })
                        .padding(horizontal = Spacing.s2),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .background(if (acknowledged) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                            .border(if (acknowledged) 0.dp else 1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (acknowledged) Icon(painterResource(R.drawable.ic_check), null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(16.dp))
                    }
                    Text(stringResource(R.string.welcome_privacy_check), style = MaterialTheme.typography.bodyMedium)
                }
                FormStatus(form)
                PrimaryButton(
                    stringResource(R.string.welcome_start), onClick = onAccept, modifier = Modifier.fillMaxWidth(), enabled = ready,
                    loading = form.running,
                )
            }
        },
    )
}

/** Erstes Wort des Anzeigenamens; leer, wenn der Name fehlt. */
internal fun firstName(name: String) = name.trim().substringBefore(' ')

@Composable
private fun WelcomePoint(@DrawableRes icon: Int, @StringRes title: Int, @StringRes text: Int) {
    Row(Modifier.semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondaryContainer, Pill), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, tint = MaterialTheme.cevi.accent, modifier = Modifier.size(20.dp))
        }
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Text des Fehlers eines Formulars; bei 429 mit der Wartezeit des Servers («Bitte warte 30 Sekunden»). */
@Composable
internal fun formErrorText(form: FormState): String? {
    val error = form.error ?: return null
    val seconds = form.waitSeconds ?: return stringResource(error)
    val time = waitTime(seconds)
    val words = pluralStringResource(if (time.minutes) R.plurals.wait_minutes else R.plurals.wait_seconds, time.amount, time.amount)
    return stringResource(error, words)
}
