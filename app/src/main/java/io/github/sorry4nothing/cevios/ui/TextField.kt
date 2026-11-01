package io.github.sorry4nothing.cevios.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.sorry4nothing.cevios.ui.theme.Sizes
import io.github.sorry4nothing.cevios.ui.theme.cevi

/**
 * Textfeld mit Label oben, wie im Design: Das Label ist immer sichtbar, das Feld mindestens 52 dp hoch mit Radius 12.
 * Der Fokus zeigt sich als 2-dp-Rand in `focus`. `hint` erklärt das Format unter dem Feld. Mehr als `maxChars` Zeichen
 * nimmt das Feld nicht an. `keyboardType` stellt die passende Tastatur ein (Telefon: Ziffern, keine Grossschreibung).
 */
@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    maxChars: Int,
    modifier: Modifier = Modifier,
    hint: String? = null,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    masked: Boolean = false,
    autofill: ContentType? = null,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = MaterialTheme.shapes.small
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= maxChars) onValueChange(it) },
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = label
                autofill?.let { this.contentType = it }
            },
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
            cursorBrush = SolidColor(scheme.onSurface),
            singleLine = minLines == 1,
            minLines = minLines,
            interactionSource = interaction,
            visualTransformation = if (masked) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                capitalization = if (keyboardType == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
                autoCorrectEnabled = keyboardType == KeyboardType.Text,
                keyboardType = keyboardType,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
            decorationBox = { inner ->
                // Mit Knopf am Ende (48 dp) bleibt im 52-dp-Feld nur ein schmaler Rand.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = Sizes.textField)
                        .background(scheme.surface, shape)
                        .border(if (focused) 2.dp else 1.dp, if (focused) MaterialTheme.cevi.focus else scheme.outline, shape)
                        .padding(
                            start = 16.dp,
                            end = if (trailing == null) 16.dp else 2.dp,
                            top = if (trailing == null) 14.dp else 2.dp,
                            bottom = if (trailing == null) 14.dp else 2.dp,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) { inner() }
                    trailing?.invoke()
                }
            },
        )
        hint?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant) }
    }
}
