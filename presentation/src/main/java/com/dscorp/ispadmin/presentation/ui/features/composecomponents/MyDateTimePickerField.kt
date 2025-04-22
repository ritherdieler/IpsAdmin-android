package com.dscorp.ispadmin.presentation.ui.features.composecomponents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDateTimePickerField(
    modifier: Modifier = Modifier,
    label: String,
    dateTime: String,
    onDateTimeSelected: (String) -> Unit
) {
    var showDateTimePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()
    
    val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")

    MyClickableOutlineTextField(
        modifier = modifier,
        text = dateTime,
        label = label,
        onClick = { showDateTimePicker = true }
    )

    if (showDateTimePicker) {
        DatePickerDialog(
            colors = DatePickerDefaults.colors().copy(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            onDismissRequest = { 
                showDateTimePicker = false 
                selectedDate = null
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = it
                            showDateTimePicker = false
                            showTimePicker = true
                        }
                    }, 
                    content = { Text("Siguiente") }
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDateTimePicker = false 
                        selectedDate = null
                    },
                    content = { Text("Cancelar") }
                )
            },
            content = {
                DatePicker(
                    state = datePickerState,
                )
            }
        )
    }
    
    if (showTimePicker && selectedDate != null) {
        TimePickerDialog(
            onCancel = { 
                showTimePicker = false 
                selectedDate = null
            },
            onConfirm = {
                // Combinar fecha y hora
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = selectedDate!!
                
                // Configura la hora desde el TimePicker
                calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                calendar.set(Calendar.MINUTE, timePickerState.minute)
                
                // Formatear el resultado
                val formattedDateTime = SimpleDateFormat(
                    "dd-MM-yyyy HH:mm",
                    Locale.getDefault()
                ).format(calendar.time)
                
                onDateTimeSelected(formattedDateTime)
                showTimePicker = false
                selectedDate = null
            },
            timePickerState = timePickerState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    timePickerState: TimePickerState
) {
    AlertDialog(
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        ),
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel
            ) {
                Text("Cancelar")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Seleccione hora",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                        timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                        timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    )
} 