package io.github.sorry4nothing.cevios.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.sorry4nothing.cevios.ui.theme.cevi

private val RING_THICKNESS = 3.dp
private val RING_GAP = 2.dp

/**
 * Fokusring aus dem Design: 3 dp in `focus`, 2 dp Abstand zum Element (Tastatur, Switch Access, Voice Access).
 * Vor `clickable` oder `selectable` setzen, damit er den Fokus des Elements sieht.
 */
fun Modifier.focusRing(shape: Shape): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val color = MaterialTheme.cevi.focus
    val density = LocalDensity.current
    onFocusChanged { focused = it.isFocused }.drawWithContent {
        drawContent()
        if (focused) {
            val outer = (RING_GAP + RING_THICKNESS / 2).toPx()
            val thickness = RING_THICKNESS.toPx()
            val outlineSize = Size(size.width + 2 * outer, size.height + 2 * outer)
            val outline = shape.createOutline(outlineSize, LayoutDirection.Ltr, density)
            translate(-outer, -outer) { drawOutline(outline, color, style = Stroke(thickness)) }
        }
    }
}
