package com.dscorp.ispadmin.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.presentation.theme.IspShapes
import com.dscorp.ispadmin.presentation.theme.IspSpacing
import com.dscorp.ispadmin.presentation.theme.IspTheme

data class LegendItem(val label: String, val color: Color)

/** Optional trailing text action, e.g. "Ver detalle". */
data class CardAction(val label: String, val onClick: () -> Unit)

data class ChartCardHeader(
    val title: String,
    val subtitle: String? = null,
    val legend: List<LegendItem> = emptyList(),
    val action: CardAction? = null,
)

/** Frames one chart or breakdown: title, period/unit subtitle, legend for 2+ series, then the content. */
@Composable
fun ChartCard(header: ChartCardHeader, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    IspCard(modifier) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(header.title, style = MaterialTheme.typography.titleMedium)
                header.subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            header.action?.let { ActionText(it) }
        }
        if (header.legend.size > 1) Legend(header.legend)
        content()
    }
}

/** Plain design-system card surface with the standard padding and spacing. */
@Composable
fun IspCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = IspShapes.card, color = IspTheme.colors.card) {
        Column(Modifier.padding(IspSpacing.lg), verticalArrangement = Arrangement.spacedBy(IspSpacing.md), content = content)
    }
}

@Composable
fun Legend(items: List<LegendItem>, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(IspSpacing.lg), verticalArrangement = Arrangement.spacedBy(IspSpacing.xs)) {
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Swatch(item.color)
                Text(item.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun Swatch(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
}

@Composable
fun ActionText(action: CardAction, modifier: Modifier = Modifier) {
    TextButton(onClick = action.onClick, modifier = modifier) {
        Text(action.label, style = MaterialTheme.typography.labelMedium, color = IspTheme.colors.accentInk)
    }
}

/** One-line empty state shown inside a card instead of a blank plot. */
@Composable
fun EmptyChart(text: String) {
    Text(text, Modifier.padding(vertical = IspSpacing.sm), style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, caption: String? = null, action: CardAction? = null) {
    Row(modifier.fillMaxWidth().padding(top = IspSpacing.md), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(title, style = com.dscorp.ispadmin.presentation.theme.IspType.sectionTitle)
            caption?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        action?.let { ActionText(it) }
    }
}
