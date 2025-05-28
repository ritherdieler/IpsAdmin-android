package com.dscorp.ispadmin.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dscorp.ispadmin.presentation.theme.OnSurface
import com.dscorp.ispadmin.presentation.theme.Outline

@Composable
fun MyClickableOutlineTextField(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
    label: String
) {
    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        colors = TextFieldDefaults.colors(
            disabledIndicatorColor = Outline,
            disabledContainerColor = Color.Transparent,
            disabledPlaceholderColor = OnSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
        ),
        readOnly = true,
        enabled = false,
        value = text,
        onValueChange = { },
        label = { Text(label, color = MaterialTheme.colorScheme.onSurface) },
    )
}