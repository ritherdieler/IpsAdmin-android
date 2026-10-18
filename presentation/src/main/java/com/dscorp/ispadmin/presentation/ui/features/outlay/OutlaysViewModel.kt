package com.dscorp.ispadmin.presentation.ui.features.outlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.datasource.remote.OutlayApi
import com.dscorp.ispadmin.data.model.OutlayDTO
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
}

class OutlaysViewModel(
    private val gateway: OutlaysGateway,
    private val mainImmediate: CoroutineDispatcher,
    private val today: () -> LocalDate,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OutlaysUiState())
    val uiState: StateFlow<OutlaysUiState> = _uiState
    private var outlays: List<OutlayDTO> = emptyList()

    fun onIntent(intent: OutlaysIntent) {
        when (intent) {
            OutlaysIntent.Load -> if (_uiState.value.month == null && !_uiState.value.loading) load()
            OutlaysIntent.Refresh -> load()
            is OutlaysIntent.Filter -> _uiState.update {
                it.copy(filter = intent.category, month = OutlayMonth.from(outlays, intent.category, today()))
            }
        }
    }

    private fun load() {
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(loading = true) }
            try {
                outlays = gateway.currentMonth()
                _uiState.update { it.copy(loading = false, error = null, month = OutlayMonth.from(outlays, it.filter, today())) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(loading = false, error = LOAD_ERROR) }
            }
        }
    }

    private companion object {
        const val LOAD_ERROR = "No se pudieron cargar los egresos. Revisa tu conexión y vuelve a intentar."
    }
}

class ApiOutlaysGateway(private val api: OutlayApi) : OutlaysGateway {
    // One page covers a month of outlays (a few dozen at most).
    override suspend fun currentMonth(): List<OutlayDTO> = api.page(page = 0, size = 300, currentMonthOnly = true).content
}
