package io.github.sorry4nothing.cevios.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sorry4nothing.cevios.chat.initials
import io.github.sorry4nothing.cevios.images.LocalProfileImages
import io.github.sorry4nothing.cevios.ui.theme.Pill

/** Anfangsbuchstabe des ersten Wortes, gross; leer, wenn der Name leer ist. */
fun avatarInitial(name: String): String = initials(name).take(1)

/**
 * Runder Avatar: das Profilbild, solange es da ist und lädt, sonst die Initiale auf `secondary`. Er ist für den Screenreader
 * dekorativ: Der Name steht immer daneben, das Bild trägt nie allein eine Information. `imageUrl` ist die `bildUrl` der API;
 * geladen wird über [LocalProfileImages] (nur Arbeitsspeicher, mit der Sitzung).
 */
@Composable
fun Avatar(name: String, modifier: Modifier = Modifier, size: Dp = 32.dp, imageUrl: String? = null, initialSize: TextUnit = 12.sp) {
    val scheme = MaterialTheme.colorScheme
    val images = LocalProfileImages.current
    val bitmap by produceState<ImageBitmap?>(null, imageUrl, images) {
        value = if (imageUrl != null && images != null) images.get(imageUrl)?.asImageBitmap() else null
    }
    Box(
        modifier.size(scaledWithFont(size)).background(scheme.secondary, Pill).clip(Pill).clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        val shown = bitmap
        if (shown != null) {
            Image(shown, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            InitialText(avatarInitial(name), initialSize, scheme.onSecondary)
        }
    }
}

/** Grösse eines Kreises mit Initiale: wächst mit der Schrift, damit die Initiale bei grösster Schrift hineinpasst. */
@Composable
fun scaledWithFont(size: Dp): Dp = size * LocalDensity.current.fontScale.coerceAtLeast(1f)

/** Initiale im Kreis. Der Kreis wächst mit der Schrift (siehe [scaledWithFont]), die Schrift selbst hat kein Limit. */
@Composable
internal fun InitialText(text: String, fontSize: TextUnit, color: Color) {
    Text(text, style = MaterialTheme.typography.labelMedium.copy(fontSize = fontSize, fontWeight = FontWeight.ExtraBold), color = color)
}
