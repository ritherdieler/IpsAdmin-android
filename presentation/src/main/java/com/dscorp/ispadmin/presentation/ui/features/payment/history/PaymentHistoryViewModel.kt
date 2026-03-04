package com.dscorp.ispadmin.presentation.ui.features.payment.history

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.Payment
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.usecase.ReactivateServiceUseCase
import com.dscorp.ispadmin.domain.usecase.RestoreInternetConnectionUseCase
import com.dscorp.ispadmin.presentation.ui.features.base.BaseUiState
import com.dscorp.ispadmin.presentation.ui.features.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PaymentHistoryState(
    val isLoading: Boolean = false,
    val payments: List<Payment> = emptyList(),
    val error: String? = null,
    val isReactivationButtonLoading: Boolean = false,
    val isServiceReactivated: Boolean = false,
    val reactivationNotes: String = "",
    val isRestoreInternetLoading: Boolean = false,
    val isInternetRestored: Boolean = false
)

class PaymentHistoryViewModel(
    val repository: IRepository,
    private val reactivateServiceUseCase: ReactivateServiceUseCase,
    private val restoreInternetConnectionUseCase: RestoreInternetConnectionUseCase
) : BaseViewModel<PaymentHistoryUiState>() {
    companion object {
        const val LAST_PAYMENTS_ROW_LIMIT = 10
    }

    private val _state = MutableStateFlow(PaymentHistoryState())
    val state: StateFlow<PaymentHistoryState> = _state.asStateFlow()

    // Keeping this for backward compatibility with the BaseViewModel
    val reactivationButtonIsLoading = MutableLiveData(false)

    // Store the original unfiltered list of payments
    private var allPayments: List<Payment> = emptyList()

    var subscriptionId: Int? = null
    
    // Añadimos la propiedad serviceStatus
    var serviceStatus: ServiceStatus = ServiceStatus.ACTIVE

    fun getLastPayments(itemsLimit: Int) = viewModelScope.launch {
        try {
            _state.update { it.copy(isLoading = true) }
            val response = repository.getRecentPaymentsHistory(subscriptionId!!, itemsLimit)
            allPayments = response // Store the original list
            _state.update { it.copy(isLoading = false, payments = response) }

            // For backward compatibility
            uiState.value =
                BaseUiState(PaymentHistoryUiState.GetRecentPaymentsHistoryResponse())
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = e.message) }
            uiState.value =
                BaseUiState(PaymentHistoryUiState.GetRecentPaymentsHistoryError(e.message))
        }
    }

    fun showOnlyPendingPayments() = viewModelScope.launch {
        try {
            val pendingPayments = allPayments.filter { !it.paid }
            _state.update { it.copy(payments = pendingPayments) }

            // For backward compatibility
            uiState.value =
                BaseUiState(PaymentHistoryUiState.OnPaymentHistoryFilteredResponse())
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message) }
            uiState.value = BaseUiState(PaymentHistoryUiState.OnError(e.message))
        }
    }

    fun showAllPayments() = viewModelScope.launch {
        try {
            // Use the stored original list
            _state.update { it.copy(payments = allPayments) }

            // For backward compatibility
            uiState.value =
                BaseUiState(PaymentHistoryUiState.OnPaymentHistoryFilteredResponse())
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message) }
            uiState.value = BaseUiState(PaymentHistoryUiState.OnError(e.message))
        }
    }

    fun updateReactivationNotes(notes: String) {
        _state.update { it.copy(reactivationNotes = notes) }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun clearReactivationState() {
        _state.update { it.copy(isServiceReactivated = false) }
    }

    fun reactivateService() = viewModelScope.launch {
        _state.update { it.copy(isReactivationButtonLoading = true) }
        reactivationButtonIsLoading.value = true

        subscriptionId?.let { id ->
            reactivateServiceUseCase(id, _state.value.reactivationNotes).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            isReactivationButtonLoading = false,
                            isServiceReactivated = true,
                            reactivationNotes = "" // Clear notes after successful reactivation
                        )
                    }
                    uiState.value = BaseUiState(PaymentHistoryUiState.ServiceReactivated)
                    reactivationButtonIsLoading.value = false
                },
                onFailure = { error ->
                    _state.update { 
                        it.copy(
                            isReactivationButtonLoading = false, 
                            error = error.message
                        ) 
                    }
                    uiState.value = BaseUiState(PaymentHistoryUiState.OnError(error.message))
                    reactivationButtonIsLoading.value = false
                }
            )
        }
    }

    fun restoreInternetConnection() = viewModelScope.launch {
        _state.update { it.copy(isRestoreInternetLoading = true) }

        subscriptionId?.let { id ->
            restoreInternetConnectionUseCase(id).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            isRestoreInternetLoading = false,
                            isInternetRestored = true
                        )
                    }
                    uiState.value = BaseUiState(PaymentHistoryUiState.InternetRestored)
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isRestoreInternetLoading = false,
                            error = error.message
                        )
                    }
                    uiState.value = BaseUiState(PaymentHistoryUiState.OnError(error.message))
                }
            )
        }
    }

    fun clearInternetRestoredState() {
        _state.update { it.copy(isInternetRestored = false) }
    }
} 