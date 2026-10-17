package com.dscorp.ispadmin.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** 4dp spacing grid from the design system. */
object IspSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object IspShapes {
    val badge = RoundedCornerShape(8.dp)
    val card = RoundedCornerShape(16.dp)
    val heroBottom = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
    val pill = RoundedCornerShape(percent = 50)
}
