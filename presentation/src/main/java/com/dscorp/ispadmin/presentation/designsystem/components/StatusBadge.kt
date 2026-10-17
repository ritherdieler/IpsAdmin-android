package com.dscorp.ispadmin.presentation.designsystem.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.presentation.theme.IspShapes
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.theme.IspType

enum class BadgeTone { Success, Warning, Error, Info, Neutral }

/** State label: color plus a word, never color alone. */
@Composable
fun StatusBadge(text: String, tone: BadgeTone, modifier: Modifier = Modifier) {
    val (container, content) = badgeColors(tone)
    Surface(modifier, shape = IspShapes.badge, color = container) {
        Text(text, Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = IspType.kpiDelta, color = content)
    }
}

@Composable
private fun badgeColors(tone: BadgeTone): Pair<Color, Color> {
    val colors = IspTheme.colors
    return when (tone) {
        BadgeTone.Success -> colors.successContainer to colors.success
        BadgeTone.Warning -> colors.warningContainer to colors.warning
        BadgeTone.Error -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
        BadgeTone.Info -> colors.infoContainer to colors.info
        BadgeTone.Neutral -> MaterialTheme.colorScheme.surfaceContainer to MaterialTheme.colorScheme.onSurfaceVariant
    }
}
