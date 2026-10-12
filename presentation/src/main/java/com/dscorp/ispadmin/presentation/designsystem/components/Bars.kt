package com.dscorp.ispadmin.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.presentation.theme.IspShapes
import com.dscorp.ispadmin.presentation.theme.IspSpacing
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.theme.IspType

data class BarItem(val label: String, val valueText: String, val value: Double, val color: Color? = null)

/** Ranked horizontal bars scaled to the largest value; text stays in text colors. */
@Composable
fun HorizontalBarList(
    items: List<BarItem>,
    modifier: Modifier = Modifier,
    barColor: Color = IspTheme.colors.chartPrimary,
    labelWidth: androidx.compose.ui.unit.Dp = 112.dp,
) {
    val maximum = items.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1.0
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(IspSpacing.sm)) {
                Text(item.label, Modifier.width(labelWidth), style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                ProgressTrack((item.value / maximum).toFloat(), item.color ?: barColor, Modifier.weight(1f), height = 10.dp)
                Text(item.valueText, Modifier.width(72.dp), style = IspType.number, textAlign = TextAlign.End)
            }
        }
    }
}

data class Segment(val label: String, val value: Double, val color: Color)

/** 100% bar for part-to-whole with 2–5 parts, separated by a 2dp gap. */
@Composable
fun ProportionBar(segments: List<Segment>, modifier: Modifier = Modifier) {
    val total = segments.sumOf { it.value }.takeIf { it > 0 } ?: return
    Row(modifier.fillMaxWidth().height(14.dp).clip(IspShapes.badge), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        segments.filter { it.value > 0 }.forEach { segment ->
            Box(Modifier.weight((segment.value / total).toFloat()).fillMaxHeight().background(segment.color))
        }
    }
}

@Composable
fun ProgressTrack(fraction: Float, color: Color, modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 6.dp) {
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(IspTheme.colors.track)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(50)).background(color))
    }
}
