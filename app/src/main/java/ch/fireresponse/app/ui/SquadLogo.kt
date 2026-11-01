package ch.fireresponse.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import ch.fireresponse.app.R
import ch.fireresponse.app.images.LocalProfileImages
import ch.fireresponse.app.images.LocalSquadLogos

/**
 * Das Logo einer Stufe, eingepasst in ein Quadrat, ohne Hintergrund und Rahmen; ohne eigenes Logo oder solange es lädt (und
 * wenn es nicht lädt) das Logo (`ic_stage_fallback`, im Nachtmodus mit hellerem Blau). Für den Screenreader dekorativ:
 * Der Stufenname steht immer daneben.
 */
@Composable
fun SquadLogo(squadId: String, modifier: Modifier = Modifier, size: Dp) {
    val path = LocalSquadLogos.current[squadId]
    val images = LocalProfileImages.current
    val bitmap by produceState<ImageBitmap?>(null, path, images) {
        value = if (path != null && images != null) images.get(path)?.asImageBitmap() else null
    }
    Box(modifier.size(scaledWithFont(size)).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        val shown = bitmap
        if (shown != null) {
            Image(shown, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        } else {
            Image(painterResource(R.drawable.ic_stage_fallback), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        }
    }
}
