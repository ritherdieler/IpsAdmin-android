package com.dscorp.ispadmin.presentation.ui.features.financial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.domain.model.DownloadDocumentResponse
import com.dscorp.ispadmin.domain.model.financial.FinancialReport
import com.dscorp.ispadmin.domain.repository.FinancialRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId

// Displayed data always belongs to a saved, identifiable cut.
data class FinancialUiState(
    val report: FinancialReport? = null, val isLoading: Boolean = false, val failed: Boolean = false,
    val serviceMonth: String = "", val movementsMonth: String = "", val billingView: Boolean = false,
    val exporting: Boolean = false, val exportFailed: Boolean = false
)
sealed interface FinancialIntent {
    data object Load : FinancialIntent
    data object Export : FinancialIntent
    data class SelectServiceMonth(val month: String) : FinancialIntent
    data class SelectMovementsMonth(val month: String) : FinancialIntent
    data class SelectView(val billing: Boolean) : FinancialIntent
}
sealed interface FinancialEvent {
    data class Document(val document: DownloadDocumentResponse) : FinancialEvent
}
class FinancialViewModel(
    private val repository: FinancialRepository,
    private val mainImmediate: CoroutineDispatcher = Dispatchers.Main.immediate,
    currentMonth: String = YearMonth.now(ZoneId.of("America/Lima")).toString()
) : ViewModel() {
    private val state = MutableStateFlow(FinancialUiState(serviceMonth = YearMonth.parse(currentMonth).minusMonths(1).toString(), movementsMonth = currentMonth))
    val uiState = state.asStateFlow()
    private val event = MutableSharedFlow<FinancialEvent>(extraBufferCapacity = 1)
    val events = event.asSharedFlow()
    private var loadJob: Job? = null
    private var exportJob: Job? = null

    fun onIntent(intent: FinancialIntent) {
        when (intent) {
            FinancialIntent.Load -> load()
            FinancialIntent.Export -> export()
            is FinancialIntent.SelectView -> state.update { it.copy(billingView = intent.billing) }
            is FinancialIntent.SelectServiceMonth -> {
                YearMonth.parse(intent.month)
                state.update { it.copy(serviceMonth = intent.month, report = null) }
                load()
            }
            is FinancialIntent.SelectMovementsMonth -> {
                YearMonth.parse(intent.month)
                state.update { it.copy(movementsMonth = intent.month, report = null) }
                load()
            }
        }
    }
    private fun load() {
        loadJob?.cancel()
        val service = state.value.serviceMonth
        val movements = state.value.movementsMonth
        loadJob = viewModelScope.launch(mainImmediate) {
            state.update { it.copy(isLoading = true, failed = false) }
            val result = repository.load(service, movements)
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            result.fold(
                onSuccess = { report -> state.update { it.copy(report = report, isLoading = false, failed = false) } },
                onFailure = { state.update { it.copy(isLoading = false, failed = true) } }
            )
        }
    }
    private fun export() {
        if (exportJob?.isActive == true) return
        val id = state.value.report?.id ?: return
        exportJob = viewModelScope.launch(mainImmediate) {
            state.update { it.copy(exporting = true, exportFailed = false) }
            val result = repository.export(id)
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            result.fold(
                onSuccess = { event.emit(FinancialEvent.Document(it)) },
                onFailure = { state.update { it.copy(exportFailed = true) } }
            )
            state.update { it.copy(exporting = false) }
        }
    }
}
