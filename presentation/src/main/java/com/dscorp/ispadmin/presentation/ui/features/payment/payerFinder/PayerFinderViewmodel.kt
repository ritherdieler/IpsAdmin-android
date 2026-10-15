package com.dscorp.ispadmin.presentation.ui.features.payment.payerFinder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.extensions.PayerFinderResult
import com.dscorp.ispadmin.observability.ObsBreadcrumbCategory
import com.dscorp.ispadmin.observability.ObservabilityClient
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
    private val repository: IRepository,
    private val observabilityClient: ObservabilityClient
) : ViewModel() {

    private companion object {
        const val OBS_FEATURE = "payment"
        const val OBS_SCREEN = "payer_finder"
        const val SEARCH_ERROR = "No se pudo buscar. Revisa tu conexión y vuelve a intentar."
    }

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
            if (query.trim().length < MIN_PAYER_QUERY) {
                _state.value = _state.value.copy(electronicPayers = emptyList(), error = null)
                return@launch
            }
            
            _state.value = _state.value.copy(isLoading = true)
            observabilityClient.addBreadcrumb(
                category = ObsBreadcrumbCategory.USER_ACTION,
                message = "$OBS_FEATURE.search_payer",
                data = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "queryLength" to query.length)
            )
            try {
                val results = repository.findPaymentByElectronicPayerName(query)
                _state.value = _state.value.copy(
                    electronicPayers = results,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                observabilityClient.reportError(
                    throwable = e,
                    message = "Fallo al buscar pagador electrónico",
                    tags = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "action" to "search_payer")
                )
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = SEARCH_ERROR
                )
            }
        }
    }
}