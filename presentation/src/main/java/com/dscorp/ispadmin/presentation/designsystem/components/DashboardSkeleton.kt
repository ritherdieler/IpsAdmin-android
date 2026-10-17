package com.dscorp.ispadmin.presentation.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.presentation.theme.IspShapes
import com.dscorp.ispadmin.presentation.theme.IspSpacing

private const val SHIMMER_MILLIS = 1200

/** Loading state shaped like the dashboard (KPI grid + first chart), replaced in one step. */
@Composable
fun DashboardSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(0.55f, 1f,
        infiniteRepeatable(tween(SHIMMER_MILLIS, easing = LinearEasing), RepeatMode.Reverse), label = "alpha")
    Column(
        modifier.fillMaxWidth().padding(horizontal = IspSpacing.lg).testTag("dashboard_skeleton")
            .semantics { contentDescription = "Cargando dashboard" }.graphicsLayer { this.alpha = alpha },
        verticalArrangement = Arrangement.spacedBy(IspSpacing.md),
    ) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(IspSpacing.md)) {
                Block(Modifier.weight(1f), 96.dp, IspShapes.card)
                Block(Modifier.weight(1f), 96.dp, IspShapes.card)
            }
        }
        Block(Modifier.fillMaxWidth(), 24.dp, IspShapes.badge)
        Block(Modifier.fillMaxWidth(), 220.dp, IspShapes.card)
    }
}

@Composable
private fun Block(modifier: Modifier, height: Dp, shape: Shape) {
    Box(modifier.height(height).background(MaterialTheme.colorScheme.surfaceContainerHigh, shape))
}
