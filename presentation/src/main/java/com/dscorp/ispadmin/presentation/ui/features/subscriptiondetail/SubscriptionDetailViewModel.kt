package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SubscriptionDetailViewModel(
    val repository: IRepository,
) : ViewModel() {

    val uiState = MutableStateFlow(SubscriptionDetailUiState())

    fun getSubscription(subscriptionId: Int) = viewModelScope.launch {
        try {
            uiState.value = uiState.value.copy(isLoading = true)
            val subscriptionResponse = repository.subscriptionById(subscriptionId)
            uiState.value = uiState.value.copy(
                subscription = subscriptionResponse,
                isLoading = false
            )
        } catch (e: Exception) {
            uiState.value =
                uiState.value.copy(error = e.message, isLoading = false, subscription = null)
        }
    }

    fun clearError() {
        uiState.value = uiState.value.copy(error = null)
    }

    data class SubscriptionDetailUiState(
        val subscription: SubscriptionResponse? = null,
        val isLoading: Boolean = false,
        val error: String? = null
    )

}
