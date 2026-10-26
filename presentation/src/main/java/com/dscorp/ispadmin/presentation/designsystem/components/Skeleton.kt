package com.dscorp.ispadmin.presentation.designsystem.components

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.dscorp.ispadmin.presentation.theme.IspTheme
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// Same sweep as the web back office's `.skeleton` (src/index.css): 1.7 s, eased, no pause between passes.
private const val SWEEP_MILLIS = 1700
private val SweepEasing = CubicBezierEasing(0.45f, 0.05f, 0.55f, 0.95f)
private const val SWEEP_ANGLE_RAD = 100.0 * Math.PI / 180.0

/** One clock for every bone on screen, so a single light crosses them all at once. */
@Stable
class SkeletonSweep internal constructor(private val progress: State<Float>?) {
    /** False when the system turned animations off; bones are then drawn flat. */
    val animated: Boolean get() = progress != null

    internal val value: Float get() = progress?.value ?: 0f
}

private val LocalSkeletonSweep = staticCompositionLocalOf<SkeletonSweep?> { null }

@Composable
fun rememberSkeletonSweep(): SkeletonSweep {
    val context = LocalContext.current
    val reduceMotion = remember(context) { animationsDisabled(context) }
    return if (reduceMotion) {
        remember { SkeletonSweep(null) }
    } else {
        val progress = rememberInfiniteTransition(label = "skeleton")
            .animateFloat(0f, 1f, infiniteRepeatable(tween(SWEEP_MILLIS, easing = SweepEasing)), label = "sweep")
        remember(progress) { SkeletonSweep(progress) }
    }
}

/** Wraps a loading screen so all its [skeleton] bones share one sweep; nested wrappers reuse the outer one. */
@Composable
fun SkeletonSweepProvider(content: @Composable () -> Unit) {
    val sweep = LocalSkeletonSweep.current ?: rememberSkeletonSweep()
    CompositionLocalProvider(LocalSkeletonSweep provides sweep, content = content)
}

/** Animator duration scale 0 is how Android exposes «Quitar animaciones» / reduce motion. */
internal fun animationsDisabled(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

private val BoneShape = RoundedCornerShape(8.dp)

/**
 * Loading placeholder with the brand sweep. The gradient is laid out over the whole window and each bone shows its slice,
 * like the web's `background-attachment: fixed`. [card] is for bones that stand for a whole card (KPI tiles).
 */
fun Modifier.skeleton(card: Boolean = false, shape: Shape = BoneShape): Modifier = composed {
    val colors = IspTheme.colors
    val base = if (card) colors.card else colors.skeletonBase
    val highlight = if (card) colors.skeletonCardHighlight else colors.skeletonHighlight
    val edge = if (card) Modifier.border(1.dp, colors.cardBorder, shape) else Modifier
    val sweep = LocalSkeletonSweep.current ?: rememberSkeletonSweep()
    if (!sweep.animated) return@composed edge.background(base, shape)
    val frame = remember { BoneFrame() }
    edge.onGloballyPositioned { frame.update(it) }.drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        onDrawBehind {
            val window = frame.window
            if (window.width <= 0f) drawOutline(outline, base)
            else drawOutline(outline, sweepBrush(sweep.value, frame.offset, window, base, highlight))
        }
    }
}

private class BoneFrame {
    var offset by mutableStateOf(Offset.Zero)
    var window by mutableStateOf(Size.Zero)

    fun update(coordinates: LayoutCoordinates) {
        offset = coordinates.positionInRoot()
        window = coordinates.findRootCoordinates().size.toSize()
    }
}

/**
 * The web gradient in this bone's coordinates: a 100° linear gradient (base 40% → highlight 50% → base 60%) on a tile
 * twice the window's width whose left edge moves from -130% to 30% of the window width.
 */
internal fun sweepBrush(progress: Float, boneOffset: Offset, window: Size, base: Color, highlight: Color): Brush {
    val dx = sin(SWEEP_ANGLE_RAD).toFloat()
    val dy = -cos(SWEEP_ANGLE_RAD).toFloat()
    val tileWidth = window.width * 2
    val length = abs(tileWidth * dx) + abs(window.height * dy)
    val tileLeft = window.width * (-1.3f + 1.6f * progress)
    val center = Offset(tileLeft + window.width, window.height / 2) - boneOffset
    val half = Offset(dx, dy) * (length / 2)
    return Brush.linearGradient(
        0.4f to base, 0.5f to highlight, 0.6f to base,
        start = center - half,
        end = center + half,
    )
}
