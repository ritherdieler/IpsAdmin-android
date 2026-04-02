
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Size

@Composable
fun ZoomableImage(
    modifier: Modifier = Modifier,
    imageUrl: String,
    maxScale: Float = 3f,
    minScale: Float = 1f,
    backgroundColor: Color = Color.Black,
    contentDescription: String? = null,
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset(0f, 0f)) }
    var imageSize by remember { mutableStateOf(Offset.Zero) }
    var dragStartX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val swipeThreshold = 50f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale *= zoom
                    scale = scale.coerceIn(minScale, maxScale)

                    offset = if (scale > 1) {
                        Offset(
                            (offset.x + pan.x * scale).coerceIn(
                                -imageSize.x * (scale - 1) / 2,
                                imageSize.x * (scale - 1) / 2
                            ),
                            (offset.y + pan.y * scale).coerceIn(
                                -imageSize.y * (scale - 1) / 2,
                                imageSize.y * (scale - 1) / 2
                            )
                        )
                    } else {
                        Offset.Zero
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale == minScale) maxScale else minScale
                        offset = Offset.Zero
                    }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragStartX = it.x },
                    onDragEnd = {
                        if (isDragging && scale <= 1f) {
                            isDragging = false
                        }
                    },
                    onDragCancel = { isDragging = false },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        isDragging = true

                        // Only handle swipes when not zoomed in
                        if (scale <= 1f) {
                            if (dragAmount > swipeThreshold) {
                                onSwipeRight()
                                isDragging = false
                            } else if (dragAmount < -swipeThreshold) {
                                onSwipeLeft()
                                isDragging = false
                            }
                        }
                    }
                )
            }
    ) {
        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .size(Size.ORIGINAL)
                .build()
        )
        val state by painter.state.collectAsState()

        when (state) {
            is AsyncImagePainter.State.Loading -> {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            }
            is AsyncImagePainter.State.Success -> {
                Image(
                    painter = painter,
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .onGloballyPositioned {
                            imageSize = Offset(it.size.width.toFloat(), it.size.height.toFloat())
                        },
                    contentScale = ContentScale.Inside,
                )
            }
            is AsyncImagePainter.State.Error -> {
                Text(text = "Error al cargar la imagen", Modifier.align(Alignment.Center))
            }
            is AsyncImagePainter.State.Empty -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
        }
    }
}
