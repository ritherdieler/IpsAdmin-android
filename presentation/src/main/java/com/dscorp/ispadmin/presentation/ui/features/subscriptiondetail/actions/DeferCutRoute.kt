package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Wires «Postergar corte» and its date picker, limited to the days the Core accepts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeferCutRoute(subscriptionId: Int, onDone: (String) -> Unit, onBack: () -> Unit) {
    val viewModel: DeferCutViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    var picking by remember { mutableStateOf(false) }
    LaunchedEffect(subscriptionId) { viewModel.onIntent(DeferCutIntent.Load(subscriptionId)) }
    LaunchedEffect(state.done) { state.done?.let(onDone) }

    DeferCutContent(
        state, onIntent = viewModel::onIntent, onBack = onBack, onPickDate = { picking = true },
        onRetry = { viewModel.onIntent(DeferCutIntent.Load(subscriptionId)) },
    )

    val range = state.model?.pickRange
    if (picking && range != null) {
        val (first, last) = range
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = (state.date ?: first).toUtcMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val day = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    return !day.isBefore(first) && !day.isAfter(last)
                }
                override fun isSelectableYear(year: Int) = year == first.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { millis ->
                        viewModel.onIntent(DeferCutIntent.DateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()))
                    }
                    picking = false
                }) { Text("Elegir") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancelar") } },
        ) { DatePicker(state = picker) }
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
