package io.github.sorry4nothing.cevios.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.github.sorry4nothing.cevios.ui.theme.Spacing
import io.github.sorry4nothing.cevios.ui.theme.Eyebrow
import io.github.sorry4nothing.cevios.ui.theme.Sizes
import io.github.sorry4nothing.cevios.ui.theme.Pill
import io.github.sorry4nothing.cevios.ui.theme.cevi

/** Runder Knopf im Kopf. Die Beschreibung ist Pflicht: Der Knopf hat kein Wort. */
class HeaderButton(@DrawableRes val icon: Int, val description: String, val onClick: () -> Unit)

/**
 * Derselbe Kopf auf jedem Screen: oben eine Zeile (48 dp) mit Zurück links und höchstens einer Aktion rechts, darunter
 * Überzeile und Titel.
 * Der Titel ist eine Überschrift; nach dem Öffnen springt der Fokus auf ihn (`focusOnTitle`).
 */
@Composable
fun Header(
    title: String?,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    /** Stufen, deren Logo vor der Überzeile steht (dekorativ, der Name steht in der Überzeile). */
    eyebrowStages: List<String> = emptyList(),
    back: HeaderButton? = null,
    action: HeaderButton? = null,
    titleStyle: TextStyle = MaterialTheme.typography.headlineLarge,
    focusOnTitle: Boolean = false,
) {
    Column(
        modifier.fillMaxWidth().padding(start = Spacing.s3, top = Spacing.s3, end = Spacing.s3, bottom = Spacing.s4),
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        // Die Zeile bleibt auch ohne Knöpfe, damit der Titel zwischen den Screens nicht springt; ohne Titel und Knopf fällt sie weg.
        if (title != null || back != null || action != null) {
            Row(Modifier.fillMaxWidth().height(Sizes.touchTarget), horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                back?.let { RoundButton(it) }
                Spacer(Modifier.weight(1f))
                action?.let { RoundButton(it) }
            }
        }
        if (title != null) {
            Column(Modifier.padding(horizontal = Spacing.s2), verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
                if (eyebrow != null || eyebrowStages.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s1), verticalAlignment = Alignment.CenterVertically) {
                        eyebrowStages.forEach { StageLogo(it, size = 24.dp) }
                        eyebrow?.let { Text(it.uppercase(), style = Eyebrow, color = MaterialTheme.cevi.accent) }
                    }
                }
                val focus = remember { FocusRequester() }
                if (focusOnTitle) LaunchedEffect(title) { focus.requestFocus() }
                Text(
                    title,
                    style = titleStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .then(if (focusOnTitle) Modifier.focusRequester(focus).focusTarget() else Modifier)
                        .semantics { heading() },
                )
            }
        }
    }
}

@Composable
fun RoundButton(button: HeaderButton, modifier: Modifier = Modifier) {
    Surface(
        onClick = button.onClick,
        modifier = modifier.size(Sizes.touchTarget).focusRing(Pill).semantics { contentDescription = button.description },
        shape = Pill,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Icon(
            painterResource(button.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(12.dp),
        )
    }
}
