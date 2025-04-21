package com.dscorp.ispadmin.presentation.ui.features.installationorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.domain.model.InstallationOrder
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

class InstallationOrderViewModel : ViewModel(), KoinComponent {
    private val installationOrderUseCase: InstallationOrderUseCase by inject()
    private val userUseCase: UserUseCase by inject()

    private val _uiState = MutableStateFlow(InstallationOrderUiState())
    val uiState: StateFlow<InstallationOrderUiState> = _uiState.asStateFlow()

    init {
        loadTechnicians()
    }

    private fun loadTechnicians() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val technicians = userUseCase.getTechnicianUsers()
                _uiState.update { it.copy(technicians = technicians, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false, 
                    error = e.message ?: "Error al cargar técnicos"
                ) }
            }
        }
    }

    fun createInstallationOrder(installationOrder: InstallationOrder) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val result = installationOrderUseCase.createInstallationOrder(installationOrder)
                _uiState.update { it.copy(
                    isLoading = false,
                    successMessage = "Orden de instalación creada correctamente",
                    orderCreated = result
                ) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Error al crear la orden de instalación"
                ) }
            }
        }
    }

    fun assignTechnicianToOrder(
        orderId: Int,
        technicianId: Int,
        assignedById: Int,
        scheduledDate: LocalDate
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val result = installationOrderUseCase.assignTechnician(
                    orderId, technicianId, assignedById, scheduledDate
                )
                _uiState.update { it.copy(
                    isLoading = false,
                    successMessage = "Técnico asignado correctamente",
                    orderUpdated = result
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Error al asignar técnico"
                ) }
            }
        }
    }

    fun closeInstallationOrder(orderId: Int) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val result = installationOrderUseCase.closeInstallationOrder(orderId)
                _uiState.update { it.copy(
                    isLoading = false,
                    successMessage = "Orden de instalación cerrada correctamente",
                    orderUpdated = result
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Error al cerrar la orden de instalación"
                ) }
            }
        }
    }

    fun cancelInstallationOrder(orderId: Int, cancellationReason: String? = null) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val result = installationOrderUseCase.cancelInstallationOrder(orderId, cancellationReason)
                _uiState.update { it.copy(
                    isLoading = false,
                    successMessage = "Orden de instalación cancelada correctamente",
                    orderUpdated = result
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Error al cancelar la orden de instalación"
                ) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearOrderCreated() {
        _uiState.update { it.copy(orderCreated = null) }
    }

    fun clearOrderUpdated() {
        _uiState.update { it.copy(orderUpdated = null) }
    }
}

data class InstallationOrderUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val technicians: List<User> = emptyList(),
    val orderCreated: InstallationOrder? = null,
    val orderUpdated: InstallationOrder? = null
) 