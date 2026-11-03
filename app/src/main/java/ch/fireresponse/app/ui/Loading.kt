package ch.fireresponse.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.fireresponse.app.R
import kotlinx.coroutines.delay

/**
 * Ladekreis in der Farbe des Inhalts (im Knopf: Textfarbe). Er ist stumm: Was läuft, sagt der Knopf mit seinem Zustand.
 * Mit ausgeschalteten Animationen im System dreht er sich nicht (Checkliste 5.9).
 */
@Composable
fun Spinner(modifier: Modifier = Modifier, size: Dp = 18.dp, color: Color = LocalContentColor.current) {
    CircularProgressIndicator(modifier.size(size), color = color, strokeWidth = 2.5.dp)
}

/** Ruhiges Pulsieren für Platzhalter-Flächen (Skelett). Ohne Animationen im System steht die Fläche still (Checkliste 5.9). */
@Composable
fun Modifier.pulse(): Modifier {
    val motion = rememberMotionEnabled()
    val alpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900), RepeatMode.Reverse),
        label = "pulse-alpha",
    )
    return if (motion) graphicsLayer { this.alpha = alpha } else this
}

/** So lange läuft eine Aktion, bevor die App sagt, dass es länger dauert (die Anfrage gibt nach 10 Sekunden auf). */
private const val SLOW_AFTER_MS = 4_000L

/**
 * Hinweis bei langer Wartezeit: Wer nach ein paar Sekunden noch wartet, soll nicht im Dunkeln stehen. Erscheint nur, solange
 * `running` gilt, und wird höflich angesagt.
 */
@Composable
fun SlowHint(running: Boolean, modifier: Modifier = Modifier) {
    var slow by remember { mutableStateOf(false) }
    LaunchedEffect(running) {
        slow = false
        if (running) {
            delay(SLOW_AFTER_MS)
            slow = true
        }
    }
    if (running && slow) {
        NoticeBanner(
            NoticeKind.INFO,
            stringResource(R.string.slow_hint),
            modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}
