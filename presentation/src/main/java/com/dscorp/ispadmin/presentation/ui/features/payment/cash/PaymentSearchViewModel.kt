package com.dscorp.ispadmin.presentation.ui.features.payment.cash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.datasource.remote.CashApi
import com.dscorp.ispadmin.data.datasource.remote.CashPaymentResponse
import com.dscorp.ispadmin.data.datasource.remote.CashSearchResponse
import com.dscorp.ispadmin.data.repository.IRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

interface PaymentsGateway {
    suspend fun search(query: String, method: String?, days: Int): CashSearchResponse
    suspend fun payment(receiptId: Int): CashPaymentResponse
    suspend fun methods(): List<String>
}

class ApiPaymentsGateway(private val api: CashApi, private val repository: IRepository) : PaymentsGateway {
    override suspend fun search(query: String, method: String?, days: Int) = api.payments(query.ifBlank { null }, method, days)
    override suspend fun payment(receiptId: Int) = api.payment(receiptId)
    override suspend fun methods(): List<String> = repository.getPaymentMethods().map { it.name }
}

data class PaymentSearchUiState(
    val query: String = "",
    val method: String? = null,
    val period: PaymentPeriod = PaymentPeriod.LAST_30,
    val methods: List<String> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val result: PaymentSearchResult? = null,
) {
    val words: List<String> get() = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
}

sealed interface PaymentSearchIntent {
    data object Load : PaymentSearchIntent
    data class QueryChanged(val query: String) : PaymentSearchIntent
    data class MethodSelected(val method: String?) : PaymentSearchIntent
    data class PeriodSelected(val period: PaymentPeriod) : PaymentSearchIntent
    data object Retry : PaymentSearchIntent
}

/** a-caj-06 «Buscar pagos»: by holder or payer name, method and period. */
class PaymentSearchViewModel(
    private val gateway: PaymentsGateway,
    private val mainImmediate: CoroutineDispatcher,
    private val today: () -> LocalDate,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PaymentSearchUiState())
    val uiState: StateFlow<PaymentSearchUiState> = _uiState
    private var searchJob: Job? = null

    fun onIntent(intent: PaymentSearchIntent) {
        when (intent) {
            PaymentSearchIntent.Load -> if (_uiState.value.result == null && searchJob == null) {
                loadMethods()
                search(debounce = false)
            }
            is PaymentSearchIntent.QueryChanged -> {
                _uiState.update { it.copy(query = intent.query) }
                search(debounce = true)
            }
            is PaymentSearchIntent.MethodSelected -> {
                _uiState.update { it.copy(method = intent.method) }
                search(debounce = false)
            }
            is PaymentSearchIntent.PeriodSelected -> {
                _uiState.update { it.copy(period = intent.period) }
                search(debounce = false)
            }
            PaymentSearchIntent.Retry -> search(debounce = false)
        }
    }

    private fun loadMethods() {
        viewModelScope.launch(mainImmediate) {
            val methods = try {
                gateway.methods()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emptyList()
            }
            _uiState.update { it.copy(methods = methods) }
        }
    }

    private fun search(debounce: Boolean) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(mainImmediate) {
            if (debounce) delay(TYPING_PAUSE_MS)
            val state = _uiState.value
            _uiState.update { it.copy(loading = true) }
            try {
                val response = gateway.search(state.query.trim(), state.method, state.period.days)
                _uiState.update { it.copy(loading = false, error = null, result = PaymentSearchResult.from(response, state.period, today())) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(loading = false, error = SEARCH_ERROR) }
            }
        }
    }

    private companion object {
        const val TYPING_PAUSE_MS = 350L
        const val SEARCH_ERROR = "No se pudieron buscar los pagos. Revisa tu conexión y vuelve a intentar."
    }
}

data class PaymentReceiptUiState(val loading: Boolean = false, val error: String? = null, val detail: PaymentDetail? = null)

/** a-caj-05 «Detalle de pago». */
class PaymentReceiptViewModel(private val gateway: PaymentsGateway, private val mainImmediate: CoroutineDispatcher) : ViewModel() {
    private val _uiState = MutableStateFlow(PaymentReceiptUiState())
    val uiState: StateFlow<PaymentReceiptUiState> = _uiState

    fun load(receiptId: Int) {
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val detail = PaymentDetail.from(gateway.payment(receiptId))
                _uiState.update { it.copy(loading = false, detail = detail) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(loading = false, error = LOAD_ERROR) }
            }
        }
    }

    private companion object {
        const val LOAD_ERROR = "No se pudo cargar el pago. Revisa tu conexión y vuelve a intentar."
    }
}
