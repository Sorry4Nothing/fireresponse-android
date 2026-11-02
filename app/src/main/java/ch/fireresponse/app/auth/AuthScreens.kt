package ch.fireresponse.app.auth

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.WindowInsets
import ch.fireresponse.app.ui.InitialsBadge
import androidx.compose.ui.unit.em
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.fireresponse.app.R
import ch.fireresponse.app.ui.Avatar
import ch.fireresponse.app.ui.CantonFooter
import ch.fireresponse.app.ui.Crest
import ch.fireresponse.app.ui.Header
import ch.fireresponse.app.ui.HeaderButton
import ch.fireresponse.app.ui.LabeledTextField
import ch.fireresponse.app.ui.NoticeBanner
import ch.fireresponse.app.ui.NoticeKind
import ch.fireresponse.app.ui.PrimaryButton
import ch.fireresponse.app.ui.SlowHint
import ch.fireresponse.app.ui.StatePage
import ch.fireresponse.app.ui.TextActionButton
import ch.fireresponse.app.ui.focusRing
import ch.fireresponse.app.push.SwitchRow
import ch.fireresponse.app.ui.rememberMotionEnabled
import ch.fireresponse.app.ui.theme.Eyebrow
import ch.fireresponse.app.ui.theme.Pill
import ch.fireresponse.app.ui.theme.Sizes
import ch.fireresponse.app.ui.theme.Spacing
import ch.fireresponse.app.ui.theme.fire

/** Die Flamme steht gross und angeschnitten oben rechts hinter dem Text, wie im Entwurf (420 dp, 110 dp über den Rand). */
private val LOGO_SIZE = 420.dp
private val LOGO_OVERHANG = 110.dp

/** So lange baut sich das Logo beim Start auf, und so weit gleitet es von rechts herein. */
private const val LOGO_REVEAL_MS = 700
private val LOGO_SLIDE = 80.dp

private val CardShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

/**
 * Seite mit der Flamme im Hintergrund, dem Titel oben und einem Block unten. Die Flamme liegt unter der Statusleiste und wird nicht
 * von den Rändern des Systems beschnitten (`logoTop`: Abstand ihres Rahmens zum oberen Rand, im Entwurf -30 dp, auf Willkommen
 * -130 dp). Scrollt, wenn der Platz bei grosser Schrift nicht reicht.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogoPage(
    top: @Composable ColumnScope.() -> Unit,
    bottom: @Composable ColumnScope.() -> Unit,
    animateLogo: Boolean = false,
    logoTop: Dp = (-30).dp,
) {
    // Beim Start gleitet das Logo von rechts herein und blendet auf; mit ausgeschalteten Animationen steht es gleich da.
    val motionEnabled = rememberMotionEnabled()
    val reveal = remember { Animatable(if (animateLogo && motionEnabled) 0f else 1f) }
    LaunchedEffect(Unit) { if (reveal.value < 1f) reveal.animateTo(1f, tween(LOGO_REVEAL_MS, easing = FastOutSlowInEasing)) }
    // Bei grosser Schrift (ab 130 %) und bei offener Tastatur entfällt die Flamme: Sie ist Zierde, und der Text darüber wäre auf Rot kaum lesbar.
    val showLogo = LocalDensity.current.fontScale < 1.3f && !WindowInsets.isImeVisible
    Box(Modifier.fillMaxSize().clipToBounds()) {
        if (showLogo) Image(
            painterResource(R.drawable.ic_logo),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = LOGO_OVERHANG, y = logoTop)
                .size(LOGO_SIZE)
                .graphicsLayer {
                    alpha = reveal.value
                    translationX = (1f - reveal.value) * LOGO_SLIDE.toPx()
                },
        )
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val fullHeight = maxHeight
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(Modifier.fillMaxWidth().heightIn(min = fullHeight)) {
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 28.dp, end = 28.dp, bottom = 28.dp, top = Spacing.s8),
                        verticalArrangement = Arrangement.spacedBy(Spacing.s2, Alignment.Bottom),
                        content = top,
                    )
                    bottom()
                }
            }
        }
    }
}

/** Zeile über dem Titel in Akzentfarbe, auf Start, Login und Sperre mit dem Wappen darüber; auf Willkommen steht der Name der Einheit. */
@Composable
private fun DepartmentEyebrow(text: String = stringResource(R.string.department), crest: Boolean = true) {
    if (crest) Crest(Modifier.padding(bottom = Spacing.s2))
    Text(text.uppercase(), style = Eyebrow, color = MaterialTheme.fire.accent)
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
            Row(
                Modifier.padding(top = Spacing.s5).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Der Balken ist Zierde (Entwurf: 96 x 4 dp, knapp die Hälfte gefüllt); der Text sagt, was läuft.
                Box(Modifier.width(96.dp).height(4.dp).background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp))) {
                    Box(Modifier.fillMaxWidth(0.45f).height(4.dp).background(MaterialTheme.fire.accent, RoundedCornerShape(2.dp)))
                }
                Text(
                    stringResource(R.string.start_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
                var forgot by remember { mutableStateOf(false) }
                TextActionButton(
                    stringResource(R.string.login_forgot_link), { forgot = !forgot }, Modifier.align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                )
                if (forgot) NoticeBanner(NoticeKind.INFO, stringResource(R.string.login_forgot))
                CantonFooter(Modifier.align(Alignment.CenterHorizontally))
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
 * Eigenes Passwort wählen: der Pflichtwechsel nach dem ersten Login, ohne Zurück und ohne Dock (im Profil gibt es keinen Wechsel).
 * Der Server verlangt auch das bisherige Passwort. Das Feld zum Wiederholen und die Regel «Stimmt überein» folgen dem Entwurf.
 */
@Composable
fun PasswordScreen(
    form: FormState,
    onSave: (String?, String) -> Unit,
    /** Die App kennt das Startpasswort vom Login: Dann entfällt das Feld dafür. */
    startPasswordKnown: Boolean = false,
) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var currentVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    var repeat by remember { mutableStateOf("") }
    val repeatFocus = remember { FocusRequester() }
    val askCurrent = !startPasswordKnown
    val bisher = if (askCurrent) current else null
    val ready = PasswordRules.canSave(bisher, new) && PasswordRules.matches(new, repeat)
    val save = { if (ready && !form.running) onSave(bisher, new) }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Header(
                title = stringResource(R.string.password_title_first),
                eyebrow = stringResource(R.string.password_eyebrow_first),
                focusOnTitle = true,
            )
            Column(Modifier.padding(horizontal = Spacing.s5), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(
                    stringResource(R.string.password_intro_first),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (askCurrent) {
                    LabeledTextField(
                        label = stringResource(R.string.password_current_first),
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
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LabeledTextField(
                    label = stringResource(R.string.password_new),
                        container = MaterialTheme.colorScheme.surfaceContainerLow,
                    value = new,
                    onValueChange = { new = it },
                    maxChars = PasswordRules.MAX_LENGTH,
                    keyboardType = KeyboardType.Password,
                    masked = !newVisible,
                    autofill = ContentType.NewPassword,
                    imeAction = ImeAction.Next,
                        onImeAction = { repeatFocus.requestFocus() },
                    enabled = !form.running,
                    trailing = { ShowPasswordSwitch(newVisible) { newVisible = it } },
                )
                    // Die Regeln stehen unter dem Feld und sagen Änderungen an (Polite); nie nur über die Farbe.
                    Column(
                        Modifier.padding(top = 2.dp).semantics { liveRegion = LiveRegionMode.Polite },
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        PasswordRule(R.string.password_rule_length, PasswordRules.longEnough(new))
                        if (askCurrent) PasswordRule(R.string.password_rule_different, PasswordRules.different(current, new))
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LabeledTextField(
                    label = stringResource(R.string.password_repeat),
                    focusRequester = repeatFocus,
                    container = MaterialTheme.colorScheme.surfaceContainerLow,
                    value = repeat,
                    onValueChange = { repeat = it },
                    maxChars = PasswordRules.MAX_LENGTH,
                    keyboardType = KeyboardType.Password,
                    masked = !newVisible,
                    autofill = ContentType.NewPassword,
                    imeAction = ImeAction.Done,
                    onImeAction = save,
                    enabled = !form.running,
                    trailing = { ShowPasswordSwitch(newVisible) { newVisible = it } },
                )
                    if (repeat.isNotEmpty()) {
                        Box(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                            PasswordRule(R.string.password_rule_match, PasswordRules.matches(new, repeat))
                        }
                    }
                }
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
            tint = if (met) MaterialTheme.fire.onSuccessContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            words,
            style = MaterialTheme.typography.bodyMedium,
            color = if (met) MaterialTheme.fire.onSuccessContainer else MaterialTheme.colorScheme.onSurfaceVariant,
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
 * Willkommen beim ersten Start: kurze Einführung, die Teilnehmenden des Kontos und die Einwilligung zum Datenschutz.
 * Der Text der Erklärung steht auf dem Backend und öffnet im Browser. «Los geht’s» gilt erst mit gesetztem Haken: Sonst sagt ein
 * Hinweis, warum, und der Fokus springt auf die Checkbox.
 */
@Composable
fun WelcomeScreen(
    name: String,
    /** Namen der Einheiten des Kontos, wie der Server sie liefert; stehen als Überzeile. */
    squads: List<String>,
    /** Vornamen der Teilnehmenden des Kontos. Das Rangabzeichen kommt dazu, sobald die API es liefert. */
    participants: List<String>,
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
    var showHint by rememberSaveable { mutableStateOf(false) }
    val checkboxFocus = remember { FocusRequester() }
    val scheme = MaterialTheme.colorScheme
    LogoPage(
        logoTop = (-130).dp,
        top = {
            if (passwordSaved) {
                Row(
                    Modifier
                        .background(MaterialTheme.fire.successContainer, RoundedCornerShape(14.dp))
                        .heightIn(min = 28.dp)
                        .padding(horizontal = 12.dp)
                        .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.ic_check), null, tint = MaterialTheme.fire.onSuccessContainer, modifier = Modifier.size(18.dp))
                    Text(
                        stringResource(R.string.password_saved),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.fire.onSuccessContainer,
                    )
                }
            }
            DepartmentEyebrow(text = squads.filter { it.isNotBlank() }.joinToString(", ").ifEmpty { stringResource(R.string.department) }, crest = false)
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { focus.requestFocus() }
            Text(
                stringResource(R.string.welcome_title, firstName(name)),
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.focusRequester(focus).focusTarget().semantics { heading() },
            )
            if (participants.isNotEmpty()) {
                Text(
                    stringResource(R.string.welcome_participants).uppercase(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.04.em),
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = Spacing.s3).semantics { heading() },
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(scheme.surfaceContainerLow)
                        .border(1.dp, scheme.outlineVariant, MaterialTheme.shapes.medium),
                ) {
                    participants.forEachIndexed { index, firstName ->
                        if (index > 0) HorizontalDivider(Modifier.padding(start = 62.dp), color = scheme.outlineVariant)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .semantics(mergeDescendants = true) {},
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            InitialsBadge(firstName)
                            Text(firstName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            Column(Modifier.padding(top = Spacing.s2), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WelcomePoint(
                    R.drawable.ic_calendar, stringResource(R.string.welcome_appointments),
                    if (participants.isEmpty()) stringResource(R.string.welcome_appointments_text) else stringResource(R.string.welcome_appointments_of, joinNames(participants)),
                )
                WelcomePoint(R.drawable.ic_check, stringResource(R.string.welcome_rsvp), stringResource(R.string.welcome_rsvp_text))
            }
        },
        bottom = {
            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (passwordSaved && lockAvailable) {
                    SwitchRow(
                        R.drawable.ic_lock, stringResource(R.string.lock_switch), null,
                        lockEnabled, enabled = !form.running, onChange = onLockChange,
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = Sizes.touchTarget)
                        .focusRequester(checkboxFocus)
                        .focusRing(MaterialTheme.shapes.small)
                        .toggleable(
                            value = acknowledged,
                            enabled = !form.running,
                            role = Role.Checkbox,
                            onValueChange = { acknowledged = it; if (it) showHint = false },
                        )
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .background(if (acknowledged) MaterialTheme.fire.accent else Color.Transparent, RoundedCornerShape(6.dp))
                            .border(if (acknowledged) 0.dp else 1.5.dp, scheme.outline, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (acknowledged) Icon(painterResource(R.drawable.ic_check_selected), null, tint = scheme.surface, modifier = Modifier.size(16.dp))
                    }
                    Text(stringResource(R.string.welcome_privacy_check), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 20.sp))
                }
                Text(
                    stringResource(R.string.welcome_privacy_read),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.fire.accent,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .heightIn(min = Sizes.touchTarget)
                        .focusRing(MaterialTheme.shapes.small)
                        .clickable(role = Role.Button, onClick = onReadPrivacy)
                        .padding(start = 46.dp, end = 8.dp)
                        .wrapContentHeight(Alignment.CenterVertically),
                )
                if (showHint) {
                    NoticeBanner(NoticeKind.WARNING, stringResource(R.string.welcome_privacy_hint), Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                }
                FormStatus(form)
                PrimaryButton(
                    stringResource(R.string.welcome_start),
                    onClick = { if (acknowledged) onAccept() else { showHint = true; checkboxFocus.requestFocus() } },
                    modifier = Modifier.fillMaxWidth(),
                    loading = form.running,
                )
                CantonFooter(Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp))
            }
        },
    )
}

/** «Lena», «Lena und Noah», «Lena, Noah und Mia». */
private fun joinNames(names: List<String>): String =
    if (names.size <= 1) names.joinToString() else names.dropLast(1).joinToString(", ") + " und " + names.last()

/** Erstes Wort des Anzeigenamens; leer, wenn der Name fehlt. */
internal fun firstName(name: String) = name.trim().substringBefore(' ')

@Composable
private fun WelcomePoint(@DrawableRes icon: Int, title: String, text: String) {
    Row(Modifier.semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, Pill), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, tint = MaterialTheme.fire.accent, modifier = Modifier.size(20.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

/** Das Konto gehört noch zu keiner Einheit: Die Leitung erledigt das. Einzige Aktion ist der Weg zurück zum Login. */
@Composable
fun NoSquadScreen(onBackToLogin: () -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Header(title = null, back = HeaderButton(R.drawable.ic_back, stringResource(R.string.state_no_squad_action), onBackToLogin))
        StatePage(R.drawable.ic_lock, stringResource(R.string.state_no_squad_title), stringResource(R.string.state_no_squad_text), Modifier.weight(1f)) {
            PrimaryButton(stringResource(R.string.state_no_squad_action), onBackToLogin)
        }
    }
}
