package io.github.sorry4nothing.cevios.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import io.github.sorry4nothing.cevios.R
import io.github.sorry4nothing.cevios.chat.PhoneState
import io.github.sorry4nothing.cevios.ui.Header
import io.github.sorry4nothing.cevios.ui.HeaderButton
import io.github.sorry4nothing.cevios.ui.LabeledTextField
import io.github.sorry4nothing.cevios.ui.NoticeBanner
import io.github.sorry4nothing.cevios.ui.NoticeKind
import io.github.sorry4nothing.cevios.ui.OutlinedPillButton
import io.github.sorry4nothing.cevios.ui.PrimaryButton
import io.github.sorry4nothing.cevios.ui.SlowHint
import io.github.sorry4nothing.cevios.ui.TextActionButton
import io.github.sorry4nothing.cevios.ui.theme.Spacing

/**
 * Die eigene Telefonnummer eintragen, ändern oder entfernen. Sie ist freiwillig, und der Hinweis vor dem Speichern sagt, wer
 * sie sieht (die API verlangt das). `canSet` ist falsch, wenn die Person keine Stufenleitung mehr ist: Dann geht nur noch
 * Entfernen.
 */
@Composable
fun PhoneScreen(
    phone: String?,
    canSet: Boolean,
    state: PhoneState,
    onSave: (String) -> Unit,
    onBack: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf(phone.orEmpty()) }
    // Welcher Knopf läuft: «Entfernen» und «Speichern» schicken beide dieselbe Anfrage.
    var removing by remember { mutableStateOf(false) }
    val trimmed = input.trim()
    val invalid = trimmed.isNotEmpty() && !PhoneRules.valid(trimmed)
    val changed = trimmed != phone.orEmpty().trim()
    val back = HeaderButton(R.drawable.ic_back, stringResource(R.string.back_to_chat), onBack)
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())) {
        Header(
            title = stringResource(R.string.phone_title),
            back = back,
            titleStyle = MaterialTheme.typography.headlineMedium,
            focusOnTitle = true,
        )
        Column(Modifier.padding(horizontal = Spacing.s5), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
            NoticeBanner(NoticeKind.INFO, stringResource(R.string.phone_notice))
            if (!canSet) NoticeBanner(NoticeKind.WARNING, stringResource(R.string.phone_no_role))
            LabeledTextField(
                label = stringResource(R.string.phone_label),
                value = input,
                onValueChange = { input = it },
                maxChars = PhoneRules.MAX_LENGTH,
                hint = stringResource(R.string.phone_hint),
                keyboardType = KeyboardType.Phone,
                enabled = canSet && !state.running,
            )
            if (invalid) NoticeBanner(NoticeKind.ERROR, stringResource(R.string.phone_invalid))
            SlowHint(state.running)
            state.error?.let { NoticeBanner(NoticeKind.ERROR, stringResource(it.text)) }
            PrimaryButton(
                stringResource(R.string.save),
                onClick = { removing = false; onSave(trimmed) },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSet && changed && trimmed.isNotEmpty() && !invalid && (!state.running || !removing),
                loading = state.running && !removing,
                loadingText = stringResource(R.string.saving),
            )
            if (!phone.isNullOrBlank()) {
                OutlinedPillButton(
                    stringResource(R.string.phone_remove),
                    onClick = { removing = true; onSave("") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.running || removing,
                    loading = state.running && removing,
                    loadingText = stringResource(R.string.saving),
                )
            }
            TextActionButton(stringResource(R.string.cancel), onBack, Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.s2))
        }
    }
}
