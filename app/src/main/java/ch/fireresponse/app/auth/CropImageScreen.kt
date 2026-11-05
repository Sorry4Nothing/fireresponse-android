package ch.fireresponse.app.auth

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import ch.fireresponse.app.R
import ch.fireresponse.app.images.ProfileImageEncoder
import ch.fireresponse.app.images.ProfileImageEncoder.Crop
import ch.fireresponse.app.ui.Header
import ch.fireresponse.app.ui.HeaderButton
import ch.fireresponse.app.ui.NoticeBanner
import ch.fireresponse.app.ui.NoticeKind
import ch.fireresponse.app.ui.OutlinedPillButton
import ch.fireresponse.app.ui.PrimaryButton
import ch.fireresponse.app.ui.Spinner
import ch.fireresponse.app.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/** Ein Schritt der Aktionen für den Screenreader: Anteil des sichtbaren Ausschnitts. */
private const val MOVE_STEP = 0.1f

private sealed interface Preview {
    data object Loading : Preview
    data object Failed : Preview
    class Ready(val bitmap: Bitmap) : Preview
}

/**
 * Der Ausschnitt des gewählten Fotos: ein Quadrat, mit einem Finger verschieben, mit dem Regler zoomen. Das Profilbild wird
 * rund gezeigt, deshalb liegt ein Kreis als Hilfslinie darüber. Der Regler ersetzt die Zwei-Finger-Geste (sie geht auch, wenn man
 * es kann); für den Screenreader hat das Foto Aktionen zum Verschieben, denn eine Geste kennt er nicht.
 */
@Composable
fun CropImageScreen(uri: Uri, onCancel: () -> Unit, onConfirm: (Crop) -> Unit) {
    val resolver = LocalContext.current.contentResolver
    val preview by produceState<Preview>(Preview.Loading, uri) {
        value = withContext(Dispatchers.Default) { ProfileImageEncoder.preview(resolver, uri) }?.let { Preview.Ready(it) } ?: Preview.Failed
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())) {
        Header(
            title = stringResource(R.string.crop_title),
            back = HeaderButton(R.drawable.ic_back, stringResource(R.string.back_to_profile), onCancel),
            focusOnTitle = true,
        )
        when (val state = preview) {
            Preview.Loading -> {
                val loading = stringResource(R.string.crop_loading)
                Box(Modifier.fillMaxWidth().padding(Spacing.s6).semantics { contentDescription = loading }, contentAlignment = Alignment.Center) { Spinner(size = 28.dp) }
            }
            Preview.Failed -> Column(Modifier.padding(horizontal = Spacing.s4), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
                NoticeBanner(NoticeKind.ERROR, stringResource(R.string.crop_failed))
                OutlinedPillButton(stringResource(R.string.back_to_profile), onCancel, Modifier.fillMaxWidth())
            }
            is Preview.Ready -> CropEditor(state.bitmap, onConfirm)
        }
    }
}

@Composable
private fun CropEditor(bitmap: Bitmap, onConfirm: (Crop) -> Unit) {
    val width = bitmap.width
    val height = bitmap.height
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    var crop by remember(bitmap) { mutableStateOf(Crop()) }
    var viewport by remember { mutableFloatStateOf(0f) }
    val square = ProfileImageEncoder.squareCrop(width, height, crop)
    val step = square.side * MOVE_STEP
    val area = stringResource(R.string.crop_area)
    val zoomLabel = stringResource(R.string.crop_zoom)
    val zoomState = stringResource(R.string.crop_zoom_state, String.format(Locale.GERMAN, "%.1f", crop.zoom))
    val moves = listOf(
        stringResource(R.string.crop_left) to (-step to 0f),
        stringResource(R.string.crop_right) to (step to 0f),
        stringResource(R.string.crop_up) to (0f to -step),
        stringResource(R.string.crop_down) to (0f to step),
    )
    Column(Modifier.padding(horizontal = Spacing.s4), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .onSizeChanged { viewport = it.width.toFloat() }
                .semantics {
                    contentDescription = area
                    customActions = moves.map { (label, move) ->
                        CustomAccessibilityAction(label) { crop = crop.moved(move.first, move.second, width, height); true }
                    }
                }
                .pointerInput(bitmap) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        val zoomed = crop.zoomed(zoomChange, width, height)
                        // Ein Pixel des Bildschirms sind so viele Pixel des Fotos, wie das Quadrat breit ist, geteilt durch die Fläche.
                        val perPixel = if (viewport > 0f) ProfileImageEncoder.squareCrop(width, height, zoomed).side / viewport else 0f
                        crop = zoomed.moved(-pan.x * perPixel, -pan.y * perPixel, width, height)
                    }
                },
        ) {
            drawImage(
                image,
                srcOffset = IntOffset(square.x, square.y),
                srcSize = IntSize(square.side, square.side),
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.Medium,
            )
            val all = Path().apply { addRect(Rect(Offset.Zero, size)) }
            val circle = Path().apply { addOval(Rect(Offset.Zero, size)) }
            drawPath(Path.combine(PathOperation.Difference, all, circle), Color.Black.copy(alpha = 0.55f))
            drawCircle(Color.White, radius = size.minDimension / 2 - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
        }
        Text(
            stringResource(R.string.crop_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s1)) {
            Text(zoomLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Slider(
                value = crop.zoom,
                onValueChange = { crop = crop.copy(zoom = it).normalized(width, height) },
                valueRange = 1f..ProfileImageEncoder.MAX_ZOOM,
                modifier = Modifier.semantics {
                    contentDescription = zoomLabel
                    stateDescription = zoomState
                },
            )
        }
        PrimaryButton(stringResource(R.string.crop_confirm), { onConfirm(crop) }, Modifier.fillMaxWidth())
    }
}
