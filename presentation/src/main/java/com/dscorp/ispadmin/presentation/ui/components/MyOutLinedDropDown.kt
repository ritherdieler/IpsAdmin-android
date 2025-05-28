package com.dscorp.ispadmin.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> MyOutLinedDropDown(
    modifier: Modifier = Modifier,
    items: List<T>,
    selected: Any? = null,
    label: String? = null,
    onItemSelected: (T) -> Unit,
    hasError: Boolean = false,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
    ) {
        OutlinedTextField(
            value = if (selected == null) "" else selected.toString(),
            onValueChange = { },
            label = { label?.let { Text(text = it) } },
            readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown Icon"
                )
            },
            isError = hasError,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .clickable(enabled = enabled) { expanded = true }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            items.forEach { option ->
                // Evitamos el NPE asegurándonos que option sea no nulo
                DropdownMenuItem(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        expanded = false
                        onItemSelected(option)
                    },
                    text = {
                        Text(text = option?.toString() ?: "")
                    }
                )
            }
        }
    }
}

