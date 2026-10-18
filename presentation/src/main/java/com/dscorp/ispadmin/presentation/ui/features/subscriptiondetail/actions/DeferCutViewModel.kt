package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.datasource.remote.CutDeferralApi
import com.dscorp.ispadmin.data.datasource.remote.CutDeferralBody
import com.dscorp.ispadmin.data.datasource.remote.CutDeferralResponse
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.Payment
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.dscorp.ispadmin.presentation.ui.features.home.weekdayDayMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDate

interface DeferCutGateway {
    suspend fun subscription(subscriptionId: Int): SubscriptionResponse
    suspend fun receipts(subscriptionId: Int): List<Payment>
    suspend fun current(subscriptionId: Int): CutDeferralResponse?
    suspend fun defer(subscriptionId: Int, untilDate: String, reason: String)
}

class ApiDeferCutGateway(private val repository: IRepository, private val api: CutDeferralApi) : DeferCutGateway {
    override suspend fun subscription(subscriptionId: Int) = repository.subscriptionById(subscriptionId)
    override suspend fun receipts(subscriptionId: Int) = repository.getRecentPaymentsHistory(subscriptionId, RECENT_RECEIPTS)
    override suspend fun current(subscriptionId: Int): CutDeferralResponse? =
        api.current(subscriptionId).takeIf { it.isSuccessful && it.code() != NO_CONTENT }?.body()
    override suspend fun defer(subscriptionId: Int, untilDate: String, reason: String) {
        val response = api.defer(subscriptionId, CutDeferralBody(untilDate, reason))
        if (!response.isSuccessful) throw HttpException(response)
    }

    private companion object {
        const val RECENT_RECEIPTS = 12
        const val NO_CONTENT = 204
    }
}

data class DeferCutUiState(
    val subscriptionId: Int = 0,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val model: DeferCut? = null,
    val date: LocalDate? = null,
    val reason: String? = null,
    val detail: String = "",
    val done: String? = null,
) {
    val canSubmit: Boolean get() = date != null && reason != null && !saving
}

sealed interface DeferCutIntent {
    data class Load(val subscriptionId: Int) : DeferCutIntent
    data class DateSelected(val date: LocalDate) : DeferCutIntent
    data class ReasonSelected(val reason: String) : DeferCutIntent
    data class DetailChanged(val detail: String) : DeferCutIntent
    data object Submit : DeferCutIntent
}

class DeferCutViewModel(
    private val gateway: DeferCutGateway,
    private val mainImmediate: CoroutineDispatcher,
    private val today: () -> LocalDate,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DeferCutUiState())
    val uiState: StateFlow<DeferCutUiState> = _uiState

    fun onIntent(intent: DeferCutIntent) {
        when (intent) {
            is DeferCutIntent.Load -> load(intent.subscriptionId)
            is DeferCutIntent.DateSelected -> _uiState.update { it.copy(date = intent.date, error = null) }
            is DeferCutIntent.ReasonSelected -> _uiState.update { it.copy(reason = intent.reason, error = null) }
            is DeferCutIntent.DetailChanged -> _uiState.update { it.copy(detail = intent.detail.take(MAX_DETAIL)) }
            DeferCutIntent.Submit -> submit()
        }
    }

    private fun load(subscriptionId: Int) {
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(subscriptionId = subscriptionId, loading = true, error = null) }
            try {
                val subscription = gateway.subscription(subscriptionId)
                val receipts = runCatching { gateway.receipts(subscriptionId) }.getOrDefault(emptyList())
                val current = runCatching { gateway.current(subscriptionId) }.getOrNull()
                val model = DeferCut.from(subscription, receipts, current, today())
                _uiState.update { it.copy(loading = false, model = model, date = it.date ?: model.defaultDate) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(loading = false, error = LOAD_ERROR) }
            }
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        val date = state.date ?: return
        val reason = listOfNotNull(state.reason, state.detail.trim().takeIf { it.isNotEmpty() }).joinToString(" · ")
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(saving = true, error = null) }
            try {
                gateway.defer(state.subscriptionId, date.toString(), reason)
                _uiState.update { it.copy(saving = false, done = "Corte postergado hasta el ${weekdayDayMonth(date)}") }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(saving = false, error = SAVE_ERROR) }
            }
        }
    }

    private companion object {
        const val MAX_DETAIL = 200
        const val LOAD_ERROR = "No se pudo cargar la suscripción. Revisa tu conexión y vuelve a intentar."
        const val SAVE_ERROR = "No se pudo postergar el corte. Revisa tu conexión y vuelve a intentar."
    }
}
