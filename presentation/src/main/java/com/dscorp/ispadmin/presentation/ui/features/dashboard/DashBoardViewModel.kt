package com.dscorp.ispadmin.presentation.ui.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.DashBoardDataResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DashBoardViewModel : ViewModel(), KoinComponent {
    private val repository: IRepository by inject()
    
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        getDashBoardData()
    }

    fun getDashBoardData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            runCatching {
                repository.getDashBoardData()
            }.fold(
                onSuccess = { response ->
                    _state.update { 
                        it.copy(
                            dashboardData = response,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
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
    val event: DashboardEvent? = null
)

sealed class DashboardEvent {
    data class ShowError(val message: String?) : DashboardEvent()
    object ServiceCutSuccess : DashboardEvent()
}