package com.dscorp.ispadmin.presentation.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

enum class BubbleSide { ABOVE, BELOW }

/** Where a detail bubble goes: window position, side of its anchor and tail position measured from its left edge. */
data class BubblePlacement(val x: Int, val y: Int, val side: BubbleSide, val tailX: Int)

/** One detail line of a bubble; [color] draws a series dot before the label (charts). */
data class BubbleRow(val label: String, val value: String, val color: Color? = null)

/**
 * Design system «Burbuja de detalle»: above the anchor and centered on it, kept inside the screen; below when it
 * does not fit above. The tail keeps pointing at the anchor center, at least [tailInset] from the bubble corners.
 */
fun bubblePlacement(anchor: IntRect, bubble: IntSize, window: IntSize, margin: Int, gap: Int, tailInset: Int = 24): BubblePlacement {
    val center = (anchor.left + anchor.right) / 2
    val maxX = (window.width - margin - bubble.width).coerceAtLeast(margin)
    val x = (center - bubble.width / 2).coerceIn(margin, maxX)
    val aboveY = anchor.top - gap - bubble.height
    val side = if (aboveY >= margin) BubbleSide.ABOVE else BubbleSide.BELOW
    val y = if (side == BubbleSide.ABOVE) aboveY else anchor.bottom + gap
    val tailX = (center - x).coerceIn(tailInset.coerceAtMost(bubble.width / 2), (bubble.width - tailInset).coerceAtLeast(bubble.width / 2))
    return BubblePlacement(x, y, side, tailX)
}

/** Tappable (i) that opens a [DetailBubble] over the content; it never resizes the layout around it. */
@Composable
fun InfoBubbleButton(
    text: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = !open }, modifier = modifier.size(28.dp)) {
            Icon(Icons.Outlined.Info, contentDescription = contentDescription, modifier = Modifier.size(16.dp), tint = tint)
        }
        if (open) AnchoredBubble(title = title, text = text, onDismiss = { open = false })
    }
}

/** Shows a [DetailBubble] anchored to the composable that hosts it, as a popup over the screen. */
@Composable
fun AnchoredBubble(title: String?, text: String?, rows: List<BubbleRow> = emptyList(), onDismiss: () -> Unit) {
    val density = LocalDensity.current
    var placement by remember { mutableStateOf<BubblePlacement?>(null) }
    val provider = remember(density) {
        object : PopupPositionProvider {
            override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
                val result = with(density) {
                    bubblePlacement(anchorBounds, popupContentSize, windowSize, BUBBLE_MARGIN.roundToPx(), BUBBLE_GAP.roundToPx(), BUBBLE_TAIL_INSET.roundToPx())
                }
                if (result != placement) placement = result
                return IntOffset(result.x, result.y)
            }
        }
    }
    Popup(popupPositionProvider = provider, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        val current = placement
        DetailBubble(
            title = title,
            text = text,
            rows = rows,
            side = current?.side ?: BubbleSide.ABOVE,
            tailX = current?.let { with(density) { it.tailX.toDp() } },
        )
    }
}

/** The bubble itself: inverse surface, 12 dp corners, 8 dp tail towards its origin, title and detail lines. */
@Composable
fun DetailBubble(
    title: String?,
    text: String?,
    rows: List<BubbleRow> = emptyList(),
    side: BubbleSide = BubbleSide.ABOVE,
    tailX: Dp? = null,
    modifier: Modifier = Modifier,
) {
    val background = MaterialTheme.colorScheme.inverseSurface
    val content = MaterialTheme.colorScheme.inverseOnSurface
    val tail = BUBBLE_TAIL
    Column(
        modifier = modifier
            .testTag(INFO_BUBBLE_TAG)
            .widthIn(max = BUBBLE_MAX_WIDTH)
            .drawBehind {
                val tailPx = tail.toPx()
                val top = if (side == BubbleSide.BELOW) tailPx else 0f
                val bottom = if (side == BubbleSide.ABOVE) size.height - tailPx else size.height
                val radius = BUBBLE_RADIUS.toPx()
                val tip = (tailX?.toPx() ?: (size.width / 2)).coerceIn(radius + tailPx, size.width - radius - tailPx)
                val path = Path().apply {
                    addRoundRect(RoundRect(0f, top, size.width, bottom, CornerRadius(radius)))
                    if (side == BubbleSide.ABOVE) {
                        moveTo(tip - tailPx, bottom); lineTo(tip, size.height); lineTo(tip + tailPx, bottom); close()
                    } else {
                        moveTo(tip - tailPx, top); lineTo(tip, 0f); lineTo(tip + tailPx, top); close()
                    }
                }
                drawPath(path, background)
            }
            .padding(
                start = 12.dp, end = 12.dp,
                top = 10.dp + if (side == BubbleSide.BELOW) tail else 0.dp,
                bottom = 10.dp + if (side == BubbleSide.ABOVE) tail else 0.dp,
            )
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        title?.let { Text(it, color = MaterialTheme.colorScheme.inversePrimary, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold) }
        text?.let { Text(it, color = content, fontSize = 13.sp, lineHeight = 18.sp) }
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                row.color?.let { dot -> Box(Modifier.size(8.dp).drawBehind { drawCircle(dot, radius = size.minDimension / 2, center = Offset(size.width / 2, size.height / 2)) }) }
                Text(row.label, color = content, fontSize = 13.sp, lineHeight = 18.sp)
                Text(row.value, color = content, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

const val INFO_BUBBLE_TAG = "info_bubble"
private val BUBBLE_RADIUS = 12.dp
private val BUBBLE_TAIL = 8.dp
private val BUBBLE_TAIL_INSET = 20.dp
private val BUBBLE_MARGIN = 16.dp
private val BUBBLE_GAP = 4.dp
private val BUBBLE_MAX_WIDTH = 280.dp
