package com.dscorp.ispadmin.presentation.ui.features.installationorders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.dscorp.ispadmin.data.model.InstallationOrderStatus
import com.dscorp.ispadmin.data.repository.InstallationOrderRepository
import com.dscorp.ispadmin.domain.model.InstallationOrder
import com.dscorp.ispadmin.domain.usecase.UserUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking

/**
 * ViewModel para la lista de órdenes de instalación.
 * Implementa la paginación utilizando Paging 3.
 */
class InstallationOrderListViewModel(
    private val repository: InstallationOrderRepository,
    private val userUseCase: UserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InstallationOrderListUiState())
    val uiState: StateFlow<InstallationOrderListUiState> = _uiState.asStateFlow()

    /**
     * Estado actual del filtro de estado
     */
    private var currentStatusFilter: InstallationOrderStatus? = null

    /**
     * Inicializa la carga de datos
     */
    init {
        loadInstallationOrders()
    }

    /**
     * Carga las órdenes de instalación utilizando la paginación.
     * El resultado se almacena en el uiState.
     */
    private fun loadInstallationOrders() {
        val ordersFlow = repository.getPaginatedInstallationOrders(
            userId = runBlocking {  userUseCase.getCurrentUser().id!! },
            status = currentStatusFilter
        ).cachedIn(viewModelScope)

        _uiState.update { state ->
            state.copy(
                installationOrders = ordersFlow,
                isLoading = false
            )
        }
    }

    /**
     * Aplica un filtro por estado a las órdenes de instalación
     */
    fun filterByStatus(status: InstallationOrderStatus?) {
        if (currentStatusFilter != status) {
            currentStatusFilter = status
            loadInstallationOrders()
        }
    }

    /**
     * Actualiza la lista de órdenes de instalación, útil para forzar una recarga
     */
    fun refreshInstallationOrders() {
        _uiState.update { it.copy(isLoading = true) }
        loadInstallationOrders()
    }
}

/**
 * Representa el estado de la UI para la lista de órdenes de instalación
 */
data class InstallationOrderListUiState(
    val installationOrders: Flow<PagingData<InstallationOrder>>? = null,
    val isLoading: Boolean = true,
    val error: String? = null
) 