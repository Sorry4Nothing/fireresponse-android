package io.github.sorry4nothing.cevios.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Gestrichelter Rand (Design: gestrichelter Knopf). Das Design legt die Strichlänge nicht fest:
 * 6 dp Strich, 4 dp Lücke.
 */
fun Modifier.dashedBorder(color: Color, shape: Shape, width: Dp = 1.dp): Modifier = drawBehind {
    val stroke = width.toPx()
    // Der Strich liegt mittig auf der Kante; nach innen rücken, damit er nicht abgeschnitten wird.
    inset(stroke / 2) {
        drawOutline(
            shape.createOutline(size, layoutDirection, this),
            color,
            style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
        )
    }
}
