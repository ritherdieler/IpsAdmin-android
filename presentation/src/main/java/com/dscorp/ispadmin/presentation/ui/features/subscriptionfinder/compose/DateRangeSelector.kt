package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.observability.ObservabilityComposeText

private enum class DateField {
    START,
    END,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeSelector(
    onSearch: (String, String) -> Unit,
) {
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var activePicker by remember { mutableStateOf<DateField?>(null) }

    val canSearch = startDate.isNotEmpty() && endDate.isNotEmpty()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Fecha de instalación",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Elige desde y hasta, o usa Hoy para buscar solo el día actual.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DateFieldBox(
                label = "Desde",
                value = startDate,
                testTag = SubscriptionFinderTestTags.DATE_START,
                onClick = { activePicker = DateField.START },
                modifier = Modifier.weight(1f),
            )
            DateFieldBox(
                label = "Hasta",
                value = endDate,
                testTag = SubscriptionFinderTestTags.DATE_END,
                onClick = { activePicker = DateField.END },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    val today = todayDdMmYyyy()
                    startDate = today
                    endDate = today
                    ObservabilityComposeText.report(
                        tag = SubscriptionFinderTestTags.DATE_TODAY,
                        label = "Hoy",
                        value = today,
                    )
                    onSearch(today, today)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag(SubscriptionFinderTestTags.DATE_TODAY),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Hoy")
            }

            Button(
                onClick = {
                    if (canSearch) {
                        onSearch(startDate, endDate)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag(SubscriptionFinderTestTags.DATE_SUBMIT),
                enabled = canSearch,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = SubscriptionFinderContentDescriptions.DATE_SEARCH_ICON,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Buscar",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }

    val pickerField = activePicker
    if (pickerField != null) {
        val initialMillis = when (pickerField) {
            DateField.START -> parseDdMmYyyyToUtcMillis(startDate)
            DateField.END -> parseDdMmYyyyToUtcMillis(endDate)
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
        )
        val confirmEnabled by remember {
            derivedStateOf { datePickerState.selectedDateMillis != null }
        }

        DatePickerDialog(
            onDismissRequest = { activePicker = null },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag(SubscriptionFinderTestTags.DATE_PICKER_CONFIRM),
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val formatted = formatDatePickerUtcMillis(millis)
                            when (pickerField) {
                                DateField.START -> {
                                    startDate = formatted
                                    ObservabilityComposeText.report(
                                        tag = SubscriptionFinderTestTags.DATE_START,
                                        label = "Desde",
                                        value = startDate,
                                    )
                                }
                                DateField.END -> {
                                    endDate = formatted
                                    ObservabilityComposeText.report(
                                        tag = SubscriptionFinderTestTags.DATE_END,
                                        label = "Hasta",
                                        value = endDate,
                                    )
                                }
                            }
                        }
                        activePicker = null
                    },
                    enabled = confirmEnabled,
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        modifier = Modifier.testTag(SubscriptionFinderTestTags.DATE_PICKER_TODAY),
                        onClick = {
                            val today = todayDdMmYyyy()
                            when (pickerField) {
                                DateField.START -> startDate = today
                                DateField.END -> endDate = today
                            }
                            ObservabilityComposeText.report(
                                tag = SubscriptionFinderTestTags.DATE_PICKER_TODAY,
                                label = "Hoy en picker",
                                value = today,
                            )
                            activePicker = null
                        },
                    ) {
                        Text("Hoy")
                    }
                    TextButton(
                        modifier = Modifier.testTag(SubscriptionFinderTestTags.DATE_PICKER_DISMISS),
                        onClick = { activePicker = null },
                    ) {
                        Text("Cancelar")
                    }
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun DateFieldBox(
    label: String,
    value: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { },
        modifier = modifier
            .testTag(testTag)
            .clickable(onClick = onClick),
        label = { Text(label) },
        placeholder = { Text("Elegir") },
        readOnly = true,
        enabled = false,
        trailingIcon = {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = "Seleccionar $label",
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        shape = RoundedCornerShape(8.dp),
    )
}
