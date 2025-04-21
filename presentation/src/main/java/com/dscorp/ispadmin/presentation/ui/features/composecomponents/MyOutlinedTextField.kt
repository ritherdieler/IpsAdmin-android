package com.dscorp.ispadmin.presentation.ui.features.composecomponents

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun MyOutlinedTextField(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onValueChange: (String) -> Unit,
    hasError: Boolean = false,
    singleLine: Boolean = false,
    maxLines: Int? = null,
    maxLength: Int? = null,
    regex: Regex? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
) {
    var isError by remember { mutableStateOf(hasError) }
    var errorText by remember { mutableStateOf(errorMessage) }

    OutlinedTextField(
        modifier = modifier,
        value = value,
        label = { Text(label) },
        onValueChange = { newValue -> 
            var validInput = true
            var error: String? = null
            
            // Validar longitud máxima
            if (maxLength != null && newValue.length > maxLength) {
                validInput = false
                error = "Máximo $maxLength caracteres"
            }
            
            // Validar patrón de texto usando regex
            if (validInput && regex != null && newValue.isNotEmpty() && !regex.matches(newValue)) {
                validInput = false
                error = "Formato inválido"
            }
            
            if (validInput) {
                onValueChange(newValue)
                isError = false
                errorText = null
            } else {
                isError = true
                errorText = error
            }
        },
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        isError = isError,
        supportingText = if (isError && errorText != null) {
            { Text(errorText!!) }
        } else {
            supportingText
        },
        trailingIcon = {
            if (isError) {
                Icon(Icons.Filled.Error, "Error", tint = androidx.compose.material3.MaterialTheme.colorScheme.error)
            }
        },
        singleLine = singleLine,
        maxLines = maxLines ?: 1,
        enabled = enabled,
        readOnly = readOnly
    )
}