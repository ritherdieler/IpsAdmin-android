package com.dscorp.ispadmin.presentation.ui.features.payment.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.domain.model.Payment
import com.dscorp.ispadmin.domain.usecase.payment.GetPaymentByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PaymentDetailViewModel(
    private val getPaymentByIdUseCase: GetPaymentByIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentDetailUiState())
    val uiState: StateFlow<PaymentDetailUiState> = _uiState.asStateFlow()

    fun fetchPaymentDetails(paymentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            getPaymentByIdUseCase(paymentId).fold(
                onSuccess = { payment ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            payment = payment,
                            error = null
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Error al cargar los detalles del pago"
                        )
                    }
                }
            )
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class PaymentDetailUiState(
    val isLoading: Boolean = false,
    val payment: Payment? = null,
    val error: String? = null
) 