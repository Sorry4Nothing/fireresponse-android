package ch.fireresponse.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ch.fireresponse.app.ui.theme.Spacing

/** Liste links (40 %) und Detail rechts (60 %, auf die Textbreite begrenzt), ab 600 dp neben dem Dock. */
@Composable
fun TwoPane(list: @Composable () -> Unit, detail: @Composable () -> Unit) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Spacing.s4)) {
        Box(Modifier.weight(0.4f)) { list() }
        Box(Modifier.weight(0.6f)) { ContentWidth { detail() } }
    }
}
