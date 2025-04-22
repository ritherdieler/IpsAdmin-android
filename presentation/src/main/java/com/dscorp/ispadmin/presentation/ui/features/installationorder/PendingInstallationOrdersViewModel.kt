package com.dscorp.ispadmin.presentation.ui.features.installationorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.domain.model.InstallationOrder
import com.dscorp.ispadmin.domain.model.InstallationOrderStatus
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.usecase.InstallationOrderUseCase
import com.dscorp.ispadmin.domain.usecase.UserUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate

data class PendingOrdersUiState(
    val pendingOrders: List<InstallationOrder> = emptyList(),
    val technicians: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val selectedOrder: InstallationOrder? = null,
    val selectedTechnician: User? = null,
    val scheduledDate: LocalDate? = null,
    val showAssignDialog: Boolean = false,
    val orderUpdated: InstallationOrder? = null
)

class PendingInstallationOrdersViewModel : ViewModel(), KoinComponent {
    private val installationOrderUseCase: InstallationOrderUseCase by inject()
    private val userUseCase: UserUseCase by inject()

    private val _uiState = MutableStateFlow(PendingOrdersUiState())
    val uiState: StateFlow<PendingOrdersUiState> = _uiState.asStateFlow()

    init {
        loadPendingOrders()
        loadTechnicians()
    }

    fun loadPendingOrders() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val pendingOrders = installationOrderUseCase.getInstallationOrdersByStatus(InstallationOrderStatus.SOLICITADO)
                
                _uiState.update { it.copy(
                    pendingOrders = pendingOrders,
                    isLoading = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Error al cargar órdenes pendientes"
                ) }
            }
        }
    }

    private fun loadTechnicians() {
        viewModelScope.launch {
            try {
                val technicians = userUseCase.getTechnicianUsers()
                _uiState.update { it.copy(technicians = technicians) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = e.message ?: "Error al cargar técnicos"
                ) }
            }
        }
    }

    fun onOrderSelected(order: InstallationOrder) {
        _uiState.update { it.copy(
            selectedOrder = order,
            showAssignDialog = true
        ) }
    }

    fun onTechnicianSelected(technician: User) {
        _uiState.update { it.copy(selectedTechnician = technician) }
    }

    fun onScheduledDateSelected(date: LocalDate) {
        _uiState.update { it.copy(scheduledDate = date) }
    }

    fun onCloseDialog() {
        _uiState.update { it.copy(
            showAssignDialog = false,
            selectedOrder = null,
            selectedTechnician = null,
            scheduledDate = null
        ) }
    }

    fun assignTechnician() {
        val order = _uiState.value.selectedOrder
        val technician = _uiState.value.selectedTechnician
        val scheduledDate = _uiState.value.scheduledDate

        if (order == null || technician == null || scheduledDate == null) {
            _uiState.update { it.copy(
                error = "Debe seleccionar un técnico y una fecha"
            ) }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val currentUser = userUseCase.getCurrentUser()
                val result = installationOrderUseCase.assignTechnician(
                    orderId = order.id,
                    technicianId = technician.id ?: throw IllegalStateException("El técnico no tiene ID"),
                    assignedById = currentUser.id ?: throw IllegalStateException("El usuario actual no tiene ID"),
                    scheduledDate = scheduledDate
                )
                
                _uiState.update { it.copy(
                    isLoading = false,
                    successMessage = "Técnico asignado correctamente",
                    orderUpdated = result,
                    showAssignDialog = false
                ) }
                
                // Recargar la lista de órdenes pendientes
                loadPendingOrders()
                
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Error al asignar técnico",
                    showAssignDialog = false
                ) }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun dismissSuccess() {
        _uiState.update { it.copy(
            successMessage = null,
            orderUpdated = null
        ) }
    }
} 