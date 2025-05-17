package com.dscorp.ispadmin.presentation.ui.features.payment.history

sealed interface PaymentHistoryUiState {
    class OnPaymentHistoryFilteredResponse() : PaymentHistoryUiState
    class OnError(val message: String? = "Unknown Error") : PaymentHistoryUiState
    class GetRecentPaymentsHistoryResponse() : PaymentHistoryUiState
    class GetRecentPaymentsHistoryError(val message: String? = "Unknown Error") : PaymentHistoryUiState
    object ServiceReactivated:PaymentHistoryUiState

}
