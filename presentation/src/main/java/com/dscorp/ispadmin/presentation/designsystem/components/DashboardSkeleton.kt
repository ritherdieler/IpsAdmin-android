package com.dscorp.ispadmin.presentation.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.presentation.theme.IspShapes
import com.dscorp.ispadmin.presentation.theme.IspSpacing

/** Loading state shaped like the dashboard (KPI grid + first chart), replaced in one step. */
@Composable
fun DashboardSkeleton(modifier: Modifier = Modifier) {
    SkeletonSweepProvider {
        Column(
            modifier.fillMaxWidth().padding(horizontal = IspSpacing.lg).testTag("dashboard_skeleton")
                .semantics { contentDescription = "Cargando dashboard" },
            verticalArrangement = Arrangement.spacedBy(IspSpacing.md),
        ) {
            repeat(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(IspSpacing.md)) {
                    Block(Modifier.weight(1f), 96.dp, IspShapes.card, card = true)
                    Block(Modifier.weight(1f), 96.dp, IspShapes.card, card = true)
                }
            }
            Block(Modifier.fillMaxWidth(), 24.dp, IspShapes.badge)
            Block(Modifier.fillMaxWidth(), 220.dp, IspShapes.card)
        }
    }
}

@Composable
private fun Block(modifier: Modifier, height: Dp, shape: Shape, card: Boolean = false) {
    Box(modifier.height(height).skeleton(card, shape))
}
