package io.github.sorry4nothing.cevios.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.sorry4nothing.cevios.ui.theme.Spacing

/**
 * Dialog im Design: `surfaceContainerHigh`, Ecken 24 dp, Innenabstand 24 dp, Abstand 12 dp, Titel als Frage. Aktionen
 * rechtsbündig: «Abbrechen» als Text-Knopf in `dismissButton`, die Aktion in `confirmButton`. Der Dialog ist nur so hoch wie
 * sein Inhalt und scrollt bei grosser Schrift.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CeviDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.padding(Spacing.s6).widthIn(max = 560.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 6.dp,
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(Spacing.s6),
                verticalArrangement = Arrangement.spacedBy(Spacing.s3),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) { content() }
                FlowRow(
                    Modifier.fillMaxWidth().padding(top = Spacing.s1),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s2, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s2),
                ) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}

/** Fliesstext im Dialog: 15/22 in `onSurfaceVariant`. */
@Composable
fun DialogText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
