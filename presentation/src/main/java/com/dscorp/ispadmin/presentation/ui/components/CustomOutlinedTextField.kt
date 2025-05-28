package com.dscorp.ispadmin.presentation.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun CustomOutlinedTextField(
    modifier: Modifier = Modifier,
    value: String = "",
    onValueChange: (String) -> Unit,
    label: String,
    maxLength: Int = 100,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    enabled: Boolean = true,
    isError: Boolean = false,
) {
    var text by remember { mutableStateOf(value) }
    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth(),
        isError = isError,
        singleLine = true,
        maxLines = 1,
        value = text,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        onValueChange = {
            if (it.length > maxLength) return@OutlinedTextField
            text = it
            onValueChange(it)
        },
        label = {
            Text(
                text = label,
            )
        }
    )
}