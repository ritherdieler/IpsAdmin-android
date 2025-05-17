package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone

@Composable
fun NameAndLastNameFilterForm(
    modifier: Modifier = Modifier,
    onSearch: (SubscriptionFilter) -> Unit
) {
    var nameText by remember { mutableStateOf("") }
    var lastNameText by remember { mutableStateOf("") }

    val keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
    val nameKeyboardActions =
        KeyboardActions(onSearch = {
            onSearch(SubscriptionFilter.BY_NAME(name = nameText, lastName = lastNameText))
        })

    Row(modifier = modifier) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = nameText,
            onValueChange = {
                nameText = it
                onSearch(SubscriptionFilter.BY_NAME(name = nameText, lastName = lastNameText))
            },
            label = { Text(text = "Nombre") },
            keyboardOptions = keyboardOptions,
            keyboardActions = nameKeyboardActions
        )

        Spacer(modifier = Modifier.size(8.dp))

        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = lastNameText,
            onValueChange = {
                lastNameText = it
                onSearch(SubscriptionFilter.BY_NAME(name = nameText, lastName = lastNameText))
            },
            label = { Text(text = "Apellido") },
            keyboardOptions = keyboardOptions,
            keyboardActions = nameKeyboardActions
        )
    }
}

@Composable
fun DocumentFilterForm(modifier: Modifier = Modifier, onSearch: (SubscriptionFilter) -> Unit) {
    var documentNumberText by remember { mutableStateOf("") }

    val keyboardOptions =
        KeyboardOptions(imeAction = ImeAction.Search, keyboardType = KeyboardType.Number)
    val keyboardActions =
        KeyboardActions(onSearch = { onSearch(SubscriptionFilter.BY_DOCUMENT(documentNumberText)) })

    Row(modifier = modifier) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = documentNumberText,
            onValueChange = {
                documentNumberText = it
                onSearch(SubscriptionFilter.BY_DOCUMENT(documentNumberText))
            },
            label = { Text(text = "DNI") },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions
        )
    }
}

fun Long.localToUTC(): Long {
    val offsetFromUtc = TimeZone.getDefault().getOffset(this)
    return this - offsetFromUtc
}

fun Calendar.getddMMyyyStringDate(): String {
    val format = SimpleDateFormat("dd/MM/yyyy")
    return format.format(this.time)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFilterForm(modifier: Modifier = Modifier, onSearch: (SubscriptionFilter) -> Unit) {

    val nowTime = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
    val endTime = Calendar.getInstance()
        .apply { set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH)) }
    var startDate by remember { mutableStateOf(nowTime.getddMMyyyStringDate()) }
    var endDate by remember { mutableStateOf(endTime.getddMMyyyStringDate()) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onSearch(SubscriptionFilter.BY_DATE(startDate, endDate))
    }

    val keyboardOption = KeyboardOptions(imeAction = ImeAction.Search)
    val keyboardAction =
        KeyboardActions(onSearch = { onSearch(SubscriptionFilter.BY_DATE(startDate, endDate)) })
    Row(modifier = modifier) {
        OutlinedTextField(
            modifier = Modifier
                .weight(1f)
                .clickable { showStartDatePicker = true },
            value = startDate,
            enabled = false,
            onValueChange = { startDate = it },
            label = { Text(text = "Inicio") },
            keyboardOptions = keyboardOption,
            keyboardActions = keyboardAction,
        )

        Spacer(modifier = Modifier.size(8.dp))

        OutlinedTextField(
            modifier = Modifier
                .weight(1f)
                .clickable { showEndDatePicker = true },
            value = endDate,
            enabled = false,
            onValueChange = { endDate = it },
            label = { Text(text = "Fin") },
            keyboardOptions = keyboardOption,
            keyboardActions = keyboardAction,
            readOnly = true,
        )
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState()
        val confirmEnabled = remember {
            derivedStateOf { datePickerState.selectedDateMillis != null }
        }
        DatePickerDialog(
            onDismissRequest = {
                showStartDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStartDatePicker = false
                        startDate = datePickerState.selectedDateMillis?.let {
                            Calendar.getInstance().apply {
                                timeInMillis = it
                                timeInMillis = timeInMillis.localToUTC()
                            }.getddMMyyyStringDate()
                        } ?: ""
                        onSearch(SubscriptionFilter.BY_DATE(startDate, endDate))
                    },
                    enabled = confirmEnabled.value
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showStartDatePicker = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState()
        val confirmEnabled = remember {
            derivedStateOf { datePickerState.selectedDateMillis != null }
        }
        DatePickerDialog(
            onDismissRequest = {
                showEndDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        endDate = datePickerState.selectedDateMillis?.let {
                            Calendar.getInstance().apply {
                                timeInMillis = it
                                timeInMillis = timeInMillis.localToUTC()
                            }.getddMMyyyStringDate()
                        } ?: ""
                        showEndDatePicker = false
                        onSearch(SubscriptionFilter.BY_DATE(startDate, endDate))
                    },
                    enabled = confirmEnabled.value
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEndDatePicker = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }


    }
}

sealed class SubscriptionFilter(val valueName: String) {
    data class BY_NAME(var name: String = "", var lastName: String = "") :
        SubscriptionFilter("Nombre")

    data class BY_DOCUMENT(val documentNumber: String = "") : SubscriptionFilter("Documento")
    data class BY_DATE(val startDate: String = "", val endDate: String = "") :
        SubscriptionFilter("Fecha")
}


