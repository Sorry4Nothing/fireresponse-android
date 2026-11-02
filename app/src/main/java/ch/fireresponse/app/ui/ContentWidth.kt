package ch.fireresponse.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ch.fireresponse.app.ui.theme.Sizes

/** Hält den Inhalt auf die maximale Textbreite (640 dp) und zentriert ihn, damit Zeilen im Querformat lesbar bleiben. */
@Composable
fun ContentWidth(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = Sizes.contentMaxWidth).fillMaxSize()) { content() }
    }
}
