package com.dscorp.ispadmin.presentation.ui.features.payment.payerFinder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.extensions.PayerFinderResult
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PayerFinderState(
    val searchQuery: String = "",
    val electronicPayers: List<PayerFinderResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class PayerFinderEvent {
    data class SearchQueryChanged(val query: String) : PayerFinderEvent()
}

class PayerFinderViewmodel(
    private val repository: IRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PayerFinderState())
    val state: StateFlow<PayerFinderState> = _state.asStateFlow()

    fun onEvent(event: PayerFinderEvent) {
        when (event) {
            is PayerFinderEvent.SearchQueryChanged -> {
                _state.value = _state.value.copy(searchQuery = event.query)
                observeElectronicPayerSearch(event.query)
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeElectronicPayerSearch(query: String) {
        viewModelScope.launch {
            if (query.isEmpty() || query.length < 3) {
                _state.value = _state.value.copy(electronicPayers = emptyList())
                return@launch
            }
            
            _state.value = _state.value.copy(isLoading = true)
            try {
                val results = repository.findPaymentByElectronicPayerName(query)
                _state.value = _state.value.copy(
                    electronicPayers = results,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error desconocido"
                )
            }
        }
    }
}