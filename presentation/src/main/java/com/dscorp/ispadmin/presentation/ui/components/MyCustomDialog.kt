package com.dscorp.ispadmin.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties


@Composable
fun MyCustomDialog(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(32.dp),
    cancelable: Boolean = true,
    closeable: Boolean = false,
    onDismissRequest: () -> Unit = {},
    content: @Composable (ColumnScope) -> Unit,
) {
    Dialog(
        onDismissRequest = {
            onDismissRequest()
        },
        properties = DialogProperties(
            dismissOnClickOutside = cancelable,
            usePlatformDefaultWidth = false
        )
    ) {
        Box {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .background(
                        Color.White,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                content(this)
            }
            if (closeable)
                Image(
                    painter = rememberVectorPainter(image = Icons.Default.Close),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clickable {
                            onDismissRequest()
                        },
                )
        }

    }


}



