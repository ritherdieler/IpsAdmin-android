package com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.ZoomableImage

@Composable
fun TicketImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
    ) {
        ZoomableImage(
            imageUrl = imageUrl,
            contentDescription = "Imagen del ticket",
            backgroundColor = Color.Transparent
        )
    }
} 