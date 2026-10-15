package com.dscorp.ispadmin.presentation.ui.features.dashboard.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.domain.model.dashboard.ClientsDashboard
import com.dscorp.ispadmin.domain.repository.ClientsDashboardRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClientsUiState(
    val dashboard: ClientsDashboard? = null,
    val isLoading: Boolean = false,
    val failed: Boolean = false,
    val lastUpdatedAtMillis: Long? = null,
)

sealed interface ClientsIntent {
    data object Load : ClientsIntent
}

class ClientsViewModel(
    private val repository: ClientsDashboardRepository,
    private val mainImmediate: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {
    private val state = MutableStateFlow(ClientsUiState())
    val uiState = state.asStateFlow()
    private var loadJob: Job? = null

    fun onIntent(intent: ClientsIntent) {
        when (intent) {
            ClientsIntent.Load -> load()
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch(mainImmediate) {
            state.update { it.copy(isLoading = true, failed = false) }
            repository.load().fold(
                onSuccess = { dashboard ->
                    state.update { it.copy(dashboard = dashboard, isLoading = false, lastUpdatedAtMillis = System.currentTimeMillis()) }
                },
                // Keep the last loaded sections visible when a refresh fails.
                onFailure = { state.update { it.copy(isLoading = false, failed = true) } },
            )
        }
    }
}
