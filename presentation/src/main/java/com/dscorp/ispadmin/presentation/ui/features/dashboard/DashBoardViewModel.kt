package com.dscorp.ispadmin.presentation.ui.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.DashBoardDataResponse
import com.dscorp.ispadmin.observability.ObsBreadcrumbCategory
import com.dscorp.ispadmin.observability.ObservabilityClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DashBoardViewModel : ViewModel(), KoinComponent {
    private val repository: IRepository by inject()
    private val observabilityClient: ObservabilityClient by inject()

    private companion object {
        const val OBS_FEATURE = "dashboard"
        const val OBS_SCREEN = "dashboard"
    }

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        getDashBoardData()
    }

    fun getDashBoardData() {
        viewModelScope.launch {
            observabilityClient.addBreadcrumb(
                category = ObsBreadcrumbCategory.NAVIGATION,
                message = "$OBS_FEATURE.load_data",
                data = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN)
            )
            _state.update { it.copy(isLoading = true, event = null) }
            runCatching {
                repository.getDashBoardData()
            }.fold(
                onSuccess = { response ->
                    _state.update { 
                        it.copy(
                            dashboardData = response,
                            isLoading = false,
                            lastUpdatedAtMillis = System.currentTimeMillis(),
                            event = null
                        )
                    }
                },
                onFailure = { error ->
                    observabilityClient.reportError(
                        throwable = error,
                        message = "Fallo al cargar datos del dashboard",
                        tags = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "action" to "load_data")
                    )
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            event = DashboardEvent.ShowError(error.message)
                        )
                    }
                }
            )
        }
    }
    
    fun onEventHandled() {
        _state.update { it.copy(event = null) }
    }
}

data class DashboardState(
    val isLoading: Boolean = false,
    val dashboardData: DashBoardDataResponse? = null,
    val lastUpdatedAtMillis: Long? = null,
    val event: DashboardEvent? = null
)

sealed class DashboardEvent {
    data class ShowError(val message: String?) : DashboardEvent()
    object ServiceCutSuccess : DashboardEvent()
}
