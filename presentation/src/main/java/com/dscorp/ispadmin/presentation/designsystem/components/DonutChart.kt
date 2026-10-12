package com.dscorp.ispadmin.presentation.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.presentation.theme.IspType

private const val FULL_TURN = 360f
private const val START_ANGLE = -90f
private const val GAP_DEGREES = 2f

/** Part-to-whole ring for 2–5 parts with the total in the middle; parts are separated by a small gap. */
@Composable
fun DonutChart(segments: List<Segment>, centerLabel: String, centerValue: String, modifier: Modifier = Modifier, diameter: Dp = 128.dp) {
    val total = segments.sumOf { it.value }
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val stroke = 16.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            var start = START_ANGLE
            segments.filter { it.value > 0 && total > 0 }.forEach { segment ->
                val sweep = (segment.value / total * FULL_TURN).toFloat()
                drawArc(segment.color, start, (sweep - GAP_DEGREES).coerceAtLeast(0.5f), useCenter = false,
                    topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke))
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(centerValue, style = IspType.number)
        }
    }
}
