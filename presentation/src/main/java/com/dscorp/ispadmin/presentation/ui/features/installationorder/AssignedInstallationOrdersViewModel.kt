package com.dscorp.ispadmin.presentation.ui.features.installationorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.model.InstallationOrderStatus
import com.dscorp.ispadmin.domain.model.InstallationOrder
import com.dscorp.ispadmin.domain.usecase.InstallationOrderUseCase
import com.dscorp.ispadmin.domain.usecase.UserUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class AssignedOrdersUiState(
    val assignedOrders: List<InstallationOrder> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedOrder: InstallationOrder? = null
)

class AssignedInstallationOrdersViewModel : ViewModel(), KoinComponent {
    private val installationOrderUseCase: InstallationOrderUseCase by inject()
    private val userUseCase: UserUseCase by inject()

    private val _uiState = MutableStateFlow(AssignedOrdersUiState())
    val uiState: StateFlow<AssignedOrdersUiState> = _uiState.asStateFlow()

    init {
        loadAssignedOrders()
    }

    fun loadAssignedOrders() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val currentUser = userUseCase.getCurrentUser()

                val orders = installationOrderUseCase.getInstallationOrdersByTechnicianAndStatus(
                    currentUser.id!!,
                    InstallationOrderStatus.EN_CURSO
                )

                _uiState.update {
                    it.copy(
                        assignedOrders = orders,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error al cargar órdenes asignadas"
                    )
                }
            }
        }
    }

    fun onOrderSelected(order: InstallationOrder) {
        _uiState.update { it.copy(selectedOrder = order) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun closeInstallationOrder(orderId: Int) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // Llamar al caso de uso para cerrar la orden
                installationOrderUseCase.closeInstallationOrder(orderId)

                // Recargar las órdenes actualizadas
                loadAssignedOrders()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error al cerrar la orden de instalación"
                    )
                }
            }
        }
    }
} 