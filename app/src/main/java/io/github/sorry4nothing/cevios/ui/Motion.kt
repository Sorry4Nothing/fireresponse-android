package io.github.sorry4nothing.cevios.ui

import android.provider.Settings
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.sorry4nothing.cevios.auth.AuthState

/**
 * Ob Bewegung erlaubt ist. Sind die Animationen im System ausgeschaltet («Bewegung entfernen» oder Entwickleroptionen),
 * bleibt alles an seinem Platz und blendet höchstens über (Checkliste 5.9).
 */
@Composable
fun rememberMotionEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
}

/** Dauer eines Übergangs: kurz genug, dass nichts aufhält. */
private const val TRANSITION_MS = 260

/** Wie weit ein Bildschirm beim Schieben läuft: ein Sechstel der Breite, dazu Überblenden (kein ganzes Wegschieben). */
private const val SLIDE_FRACTION = 6

private val NO_TRANSITION = ContentTransform(EnterTransition.None, ExitTransition.None)

/** Überblenden. Ohne Bewegung im System: kein Übergang. */
fun fadeTransition(motion: Boolean): ContentTransform =
    if (!motion) {
        NO_TRANSITION
    } else {
        (fadeIn(tween(TRANSITION_MS, easing = FastOutSlowInEasing)) togetherWith fadeOut(tween(TRANSITION_MS, easing = FastOutSlowInEasing)))
    }

/** Wechsel zwischen Bildschirmen: tiefer (Detail) schiebt von rechts herein, höher (zurück) von links, gleiche Tiefe überblendet. */
fun screenTransition(fromDepth: Int, toDepth: Int, motion: Boolean): ContentTransform {
    if (!motion) return NO_TRANSITION
    if (fromDepth == toDepth) return fadeTransition(true)
    val forward = toDepth > fromDepth
    val sign = if (forward) 1 else -1
    val fade = tween<Float>(TRANSITION_MS, easing = FastOutSlowInEasing)
    val slide = tween<androidx.compose.ui.unit.IntOffset>(TRANSITION_MS, easing = FastOutSlowInEasing)
    return (
        (slideInHorizontally(slide) { it * sign / SLIDE_FRACTION } + fadeIn(fade)) togetherWith
            (slideOutHorizontally(slide) { -it * sign / SLIDE_FRACTION } + fadeOut(fade))
        )
}

/**
 * Wechsel der Anmeldestufen (Anmelden, Passwort, Willkommen, App): überblenden. Wer die App verlässt, wechselt ohne Übergang:
 * Mit der Sitzung ist auch ihr Speicher weg, und der alte Bildschirm soll nicht noch einmal laden.
 */
fun loginTransition(from: AuthState, motion: Boolean): ContentTransform =
    if (from is AuthState.SignedIn) NO_TRANSITION else fadeTransition(motion)
