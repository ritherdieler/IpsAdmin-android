package com.dscorp.ispadmin.presentation.ui.features.outlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.datasource.remote.OutlayApi
import com.dscorp.ispadmin.data.model.OutlayDTO
import com.dscorp.ispadmin.domain.model.OutlayType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

interface OutlaysGateway {
    suspend fun currentMonth(): List<OutlayDTO>
}

sealed interface OutlaysIntent {
    data object Load : OutlaysIntent
    data object Refresh : OutlaysIntent
    data class Filter(val category: String?) : OutlaysIntent
    data class FilterType(val type: OutlayType?) : OutlaysIntent
}

class OutlaysViewModel(
    private val gateway: OutlaysGateway,
    private val summaries: OutlaySummaryGateway,
    canRegisterFixed: Boolean,
    private val mainImmediate: CoroutineDispatcher,
    private val today: () -> LocalDate,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OutlaysUiState(canRegisterFixed = canRegisterFixed))
    val uiState: StateFlow<OutlaysUiState> = _uiState
    private var outlays: List<OutlayDTO> = emptyList()

    fun onIntent(intent: OutlaysIntent) {
        when (intent) {
            OutlaysIntent.Load -> if (_uiState.value.month == null && !_uiState.value.loading) load()
            OutlaysIntent.Refresh -> load()
            is OutlaysIntent.Filter -> _uiState.update { it.copy(filter = intent.category).withMonth() }
            is OutlaysIntent.FilterType -> _uiState.update { it.copy(typeFilter = intent.type).withMonth() }
        }
    }

    /** The detail of one listed outlay, or null if it is no longer in the loaded month. */
    fun detailOf(id: Int): OutlayDetail? = outlays.firstOrNull { it.id == id }?.let(OutlayDetail::from)

    private fun OutlaysUiState.withMonth() = copy(month = OutlayMonth.from(outlays, filter, today(), typeFilter))

    private fun load() {
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(loading = true) }
            try {
                outlays = gateway.currentMonth()
                _uiState.update { it.copy(loading = false, error = null).withMonth() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(loading = false, error = LOAD_ERROR) }
                return@launch
            }
            loadSummary()
        }
    }

    // The month list is the screen's purpose; the summary only enriches it, so its failure is silent.
    private suspend fun loadSummary() {
        val summary = try {
            summaries.summary()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        _uiState.update { it.copy(summary = summary) }
    }

    private companion object {
        const val LOAD_ERROR = "No se pudieron cargar los egresos. Revisa tu conexión y vuelve a intentar."
    }
}

class ApiOutlaysGateway(private val api: OutlayApi) : OutlaysGateway {
    // One page covers a month of outlays (a few dozen at most).
    override suspend fun currentMonth(): List<OutlayDTO> = api.page(page = 0, size = 300, currentMonthOnly = true).content
}
