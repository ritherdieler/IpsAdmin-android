package com.dscorp.ispadmin.presentation.ui.features.payment.cash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.datasource.remote.CashApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.dscorp.ispadmin.data.datasource.remote.CashDayResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

interface CashDayGateway {
    suspend fun day(date: LocalDate?): CashDayResponse
}

data class CashDayUiState(val loading: Boolean = false, val error: String? = null, val day: CashDay? = null, val date: LocalDate? = null)

sealed interface CashIntent {
    data object Load : CashIntent
    data object Refresh : CashIntent
    data class SelectDate(val date: LocalDate) : CashIntent
}

class CashDayViewModel(private val gateway: CashDayGateway, private val mainImmediate: CoroutineDispatcher, private val today: () -> LocalDate) : ViewModel() {
    private val _uiState = MutableStateFlow(CashDayUiState())
    val uiState: StateFlow<CashDayUiState> = _uiState

    fun onIntent(intent: CashIntent) {
        when (intent) {
            CashIntent.Load -> if (_uiState.value.day == null && !_uiState.value.loading) load(_uiState.value.date)
            CashIntent.Refresh -> load(_uiState.value.date)
            is CashIntent.SelectDate -> load(intent.date)
        }
    }

    private fun load(date: LocalDate?) {
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(loading = true, date = date) }
            try {
                val day = CashDay.from(gateway.day(date), today())
                _uiState.update { it.copy(loading = false, error = null, day = day) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(loading = false, error = LOAD_ERROR) }
            }
        }
    }

    private companion object {
        const val LOAD_ERROR = "No se pudo cargar la caja. Revisa tu conexión y vuelve a intentar."
    }
}

class ApiCashDayGateway(private val api: CashApi) : CashDayGateway {
    override suspend fun day(date: LocalDate?): CashDayResponse = api.day(date?.toString())
}
