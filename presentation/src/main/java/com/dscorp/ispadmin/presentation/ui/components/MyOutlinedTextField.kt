package com.dscorp.ispadmin.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.domain.model.extensions.isValidDni

@Composable
fun MyOutlinedTextField(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    maxLines: Int? = null,
    maxLength: Int? = null,
    regex: Regex? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    var isError by remember { mutableStateOf(errorMessage != null) }
    var errorText by remember { mutableStateOf(errorMessage) }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            isError = true
            errorText = errorMessage
        } else {
            isError = false
            errorText = null
        }
    }

    OutlinedTextField(
        modifier = modifier,
        value = value,
        label = { Text(label) },
        onValueChange = { newValue ->
            var error: String? = null

            // Validar longitud máxima
            if (maxLength != null && newValue.length > maxLength) {
                error = "Máximo $maxLength caracteres"
            }

            // Validar patrón de texto usando regex
            if (regex != null && newValue.isNotEmpty() && !regex.matches(newValue)) {
                error = "Formato inválido"
            }

            onValueChange(newValue)
        },
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        isError = isError,
        supportingText = if (isError && errorText != null) {
            { 
                Text(
                    text = errorText!!,
                    style = MaterialTheme.typography.bodySmall
                ) 
            }
        } else {
            supportingText
        },
        trailingIcon = if (trailingIcon != null) {
            {
                when {
                    isError -> Icon(
                        Icons.Filled.Error,
                        "Error",
                        tint = MaterialTheme.colorScheme.error
                    )

                    else -> trailingIcon()
                }
            }
        } else if (isError) {
            null // Remover el ícono de error para que no haya tanto ruido visual
        } else {
            null
        },
        singleLine = singleLine,
        maxLines = maxLines ?: 1,
        enabled = enabled,
        readOnly = readOnly
    )
}

@Composable
@Preview(showBackground = true)
fun MyOutlinedTextFieldPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            var valueIsValid by remember { mutableStateOf(false) }
            var text by remember { mutableStateOf("") }
            // Campo con error
            MyOutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    valueIsValid = text.isValidDni()
                },
                label = "Campo con error",
                errorMessage = if (!valueIsValid) "Este campo contiene un error" else null,
                modifier = Modifier.fillMaxWidth()
            )

        }
    }
}
