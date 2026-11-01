package io.github.sorry4nothing.cevios.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.github.sorry4nothing.cevios.R
import io.github.sorry4nothing.cevios.ui.theme.Sizes
import io.github.sorry4nothing.cevios.ui.theme.Pill
import io.github.sorry4nothing.cevios.ui.theme.cevi

/**
 * Die eine Hauptaktion pro Screen, in Cevi-Rot: 56 dp hoch, pillenförmig.
 * `loading`: Die Aktion läuft. Statt des Symbols dreht ein Ladekreis, der Knopf behält Farbe und Text, nimmt aber keinen
 * zweiten Tipp an, und der Screenreader hört `loadingText` (z. B. «Wird gespeichert»). `enabled` gilt nur für «darf gedrückt
 * werden»: Wer `loading` setzt, lässt `enabled` bei der Eingabe, sonst wird der Knopf grau.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingText: String? = null,
) {
    Button(
        onClick = if (loading) ({}) else onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = Sizes.button).focusRing(Pill).loadingState(loading, loadingText),
        shape = Pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp),
    ) { ButtonContent(text, icon, loading) }
}

/** Nebenaktion mit Rand, z. B. «Nochmals versuchen». `loading` wie bei [PrimaryButton]. */
@Composable
fun OutlinedPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingText: String? = null,
) {
    OutlinedButton(
        onClick = if (loading) ({}) else onClick,
        modifier = modifier.heightIn(min = Sizes.touchTarget).focusRing(Pill).loadingState(loading, loadingText),
        enabled = enabled,
        shape = Pill,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) { ButtonContent(text, icon, loading) }
}

/** Ruhige Aktion in `accent`, z. B. «Abbrechen» oder «Zu den Programmen». */
@Composable
fun TextActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = Sizes.touchTarget).focusRing(Pill),
        shape = Pill,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.cevi.accent),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/** Gestrichelter Knopf für «etwas Neues anlegen», z. B. einen Einzelchat starten: ohne Fläche, 56 dp hoch. `loading` wie bei [PrimaryButton]. */
@Composable
fun DashedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingText: String? = null,
) {
    Row(
        modifier
            .heightIn(min = Sizes.button)
            .focusRing(Pill)
            .clip(Pill)
            .dashedBorder(MaterialTheme.colorScheme.outline, Pill)
            .loadingState(loading, loadingText)
            .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSecondaryContainer) { ButtonContent(text, icon, loading) }
    }
}

/** Der Screenreader hört, dass die Aktion läuft (`loadingText`, sonst «Einen Moment»), solange `loading` gilt. */
@Composable
internal fun Modifier.loadingState(loading: Boolean, loadingText: String?): Modifier {
    val description = loadingText ?: stringResource(R.string.working)
    return semantics { if (loading) stateDescription = description }
}

@Composable
private fun RowScope.ButtonContent(text: String, @DrawableRes icon: Int?, loading: Boolean = false) {
    if (loading) {
        Spinner()
        Spacer(Modifier.width(8.dp))
    } else {
        icon?.let {
            Icon(painterResource(it), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
    }
    Text(text, style = MaterialTheme.typography.labelLarge)
}
