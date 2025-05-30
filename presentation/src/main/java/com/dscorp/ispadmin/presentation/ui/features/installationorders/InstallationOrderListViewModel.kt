package com.dscorp.ispadmin.presentation.ui.features.installationorders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.dscorp.ispadmin.data.model.InstallationOrderStatus
import com.dscorp.ispadmin.domain.model.InstallationOrder
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.usecase.InstallationOrderUseCase
import com.dscorp.ispadmin.domain.usecase.UserUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

/**
 * Eventos que pueden ocurrir en la UI
 */
sealed class InstallationOrderListEvent {
    object LoadInstallationOrders : InstallationOrderListEvent()
    data class FilterByStatus(val status: InstallationOrderStatus?) : InstallationOrderListEvent()
    data class OrderSelected(val order: InstallationOrder) : InstallationOrderListEvent()
    object CloseAssignDialog : InstallationOrderListEvent()
    data class TechnicianSelected(val technician: User) : InstallationOrderListEvent()
    data class ScheduledDateSelected(val date: LocalDateTime) : InstallationOrderListEvent()
    object LoadTechnicians : InstallationOrderListEvent()
    object AssignTechnician : InstallationOrderListEvent()
    object ResetSelectedOrder : InstallationOrderListEvent()
    data class TransferOrderClicked(val order: InstallationOrder) : InstallationOrderListEvent()
    object TransferOrder : InstallationOrderListEvent()
    object CloseTransferDialog : InstallationOrderListEvent()
}

/**
 * Estado de la UI para la lista de órdenes de instalación
 */
data class InstallationOrderListUiState(
    val installationOrders: Flow<PagingData<InstallationOrder>>? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentUser: User? = null,
    val showAssignDialog: Boolean = false,
    val showTransferDialog: Boolean = false,
    val orderUpdated: InstallationOrder? = null,
    val selectedOrder: InstallationOrder? = null,
    val technicians: List<User> = emptyList(),
    val selectedTechnician: User? = null,
    val scheduledDate: LocalDateTime? = null,
    val successMessage: String? = null,
    val navigateToRegisterSubscription: Boolean = false,
) {
    fun canCreateInstallationOrder(): Boolean = currentUser?.type in listOf(
        User.UserType.ACCOUNTANT,
        User.UserType.SECRETARY,
        User.UserType.ADMIN,
        User.UserType.SALES
    )
}

/**
 * ViewModel para la lista de órdenes de instalación.
 * Implementa UDF (Unidirectional Data Flow) y paginación utilizando Paging 3.
 */
class InstallationOrderListViewModel(
    private val installationOrderUseCase: InstallationOrderUseCase,
    private val userUseCase: UserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        InstallationOrderListUiState(
            currentUser = runBlocking { userUseCase.getCurrentUser() }
        )
    )
    val uiState: StateFlow<InstallationOrderListUiState> = _uiState.asStateFlow()

    private var currentStatusFilter: InstallationOrderStatus? = null

    fun onEvent(event: InstallationOrderListEvent) {
        when (event) {
            is InstallationOrderListEvent.LoadInstallationOrders -> loadInstallationOrders()
            is InstallationOrderListEvent.FilterByStatus -> onFilterByStatus(event.status)
            is InstallationOrderListEvent.OrderSelected -> onOrderSelected(event.order)
            is InstallationOrderListEvent.CloseAssignDialog -> onCloseDialog()
            is InstallationOrderListEvent.TechnicianSelected -> onTechnicianSelected(event.technician)
            is InstallationOrderListEvent.ScheduledDateSelected -> onScheduledDateSelected(event.date)
            is InstallationOrderListEvent.LoadTechnicians -> onLoadTechnicians()
            is InstallationOrderListEvent.AssignTechnician -> onAssignTechnician()
            is InstallationOrderListEvent.ResetSelectedOrder -> onResetSelectedOrder()
            is InstallationOrderListEvent.TransferOrderClicked -> onTransferOrderClicked(event.order)
            is InstallationOrderListEvent.TransferOrder -> onTransferOrder()
            is InstallationOrderListEvent.CloseTransferDialog -> onCloseTransferDialog()
        }
    }

    private fun loadInstallationOrders() {
        viewModelScope.launch {
            try {
                val currentUser = _uiState.value.currentUser
                    ?: throw IllegalStateException("Usuario no cargado")

                val ordersFlow = getOrdersFlowForUser(currentUser)
                    .cachedIn(viewModelScope)

                val filteredFlow = currentStatusFilter?.let { status ->
                    ordersFlow.map { pagingData ->
                        pagingData.filter { order -> order.status == status }
                    }
                } ?: ordersFlow

                _uiState.update { state ->
                    state.copy(
                        installationOrders = filteredFlow,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = e.message ?: "Error desconocido al cargar órdenes"
                    )
                }
            }
        }
    }

    private fun getOrdersFlowForUser(user: User): Flow<PagingData<InstallationOrder>> {
        return when (user.type) {
            User.UserType.TECHNICIAN -> installationOrderUseCase.getInstallationOrdersByTechnicianPaginated(
                user.id ?: throw IllegalStateException("ID de técnico no disponible")
            )
            User.UserType.SALES -> installationOrderUseCase.getInstallationOrdersBySellerPaginated(
                user.id ?: throw IllegalStateException("ID de vendedor no disponible")
            )
            User.UserType.ADMIN, User.UserType.SECRETARY, User.UserType.ACCOUNTANT ->
                installationOrderUseCase.getAllInstallationOrdersPaginated()
            else -> throw IllegalStateException("Tipo de usuario no soportado: ${user.type}")
        }
    }

    private fun onFilterByStatus(status: InstallationOrderStatus?) {
        if (currentStatusFilter != status) {
            currentStatusFilter = status
            loadInstallationOrders()
        }
    }

    private fun onOrderSelected(order: InstallationOrder) {
        when {
            order.status == InstallationOrderStatus.SOLICITADO -> {
                _uiState.update {
                    it.copy(
                        selectedOrder = order,
                        showAssignDialog = true,
                        error = null
                    )
                }
            }
            uiState.value.currentUser?.type == User.UserType.TECHNICIAN &&
                    order.status == InstallationOrderStatus.EN_CURSO -> {
                _uiState.update {
                    it.copy(
                        selectedOrder = order,
                        navigateToRegisterSubscription = true,
                        error = null
                    )
                }
            }
        }
    }

    private fun onCloseDialog() {
        _uiState.update {
            it.copy(
                showAssignDialog = false,
                selectedOrder = null,
                selectedTechnician = null,
                scheduledDate = null,
                error = null
            )
        }
    }

    private fun onTechnicianSelected(technician: User) {
        _uiState.update { it.copy(selectedTechnician = technician, error = null) }
    }

    private fun onScheduledDateSelected(date: LocalDateTime) {
        _uiState.update { it.copy(scheduledDate = date, error = null) }
    }

    private fun onLoadTechnicians() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val technicians = userUseCase.getTechnicianUsers()
                _uiState.update {
                    it.copy(
                        technicians = technicians,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error al cargar técnicos"
                    )
                }
            }
        }
    }

    private fun onAssignTechnician() {
        handleTechnicianOperation(isTransfer = false)
    }

    private fun onTransferOrder() {
        handleTechnicianOperation(isTransfer = true)
    }

    private fun handleTechnicianOperation(isTransfer: Boolean) {
        val order = _uiState.value.selectedOrder
        val technician = _uiState.value.selectedTechnician
        val scheduledDate = _uiState.value.scheduledDate
        val currentUser = _uiState.value.currentUser

        if (order == null || technician == null || scheduledDate == null || currentUser == null) {
            _uiState.update {
                it.copy(
                    error = if (isTransfer) "Faltan datos para transferir la orden" else "Debe seleccionar un técnico y una fecha"
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val result = if (isTransfer) {
                    installationOrderUseCase.transferInstallationOrder(
                        orderId = order.id,
                        newTechnicianId = technician.id!!,
                        transferredById = currentUser.id!!,
                        scheduledDate = scheduledDate
                    )
                } else {
                    installationOrderUseCase.assignTechnician(
                        orderId = order.id,
                        technicianId = technician.id!!,
                        assignedById = currentUser.id!!,
                        scheduledDate = scheduledDate
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = if (isTransfer) "Orden transferida exitosamente" else "Técnico asignado correctamente",
                        orderUpdated = result,
                        selectedTechnician = null,
                        showAssignDialog = false,
                        showTransferDialog = false,
                        error = null
                    )
                }

                loadInstallationOrders()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: if (isTransfer) "Error al transferir la orden" else "Error al asignar técnico",
                        showAssignDialog = false,
                        showTransferDialog = false
                    )
                }
            }
        }
    }

    private fun onResetSelectedOrder() {
        _uiState.update {
            it.copy(
                selectedOrder = null,
                selectedTechnician = null,
                scheduledDate = null,
                showAssignDialog = false,
                navigateToRegisterSubscription = false,
                error = null
            )
        }
    }

    private fun onTransferOrderClicked(order: InstallationOrder) {
        if (order.status == InstallationOrderStatus.EN_CURSO) {
            _uiState.update {
                it.copy(
                    selectedOrder = order,
                    showTransferDialog = true,
                    error = null
                )
            }
        }
    }

    private fun onCloseTransferDialog() {
        _uiState.update {
            it.copy(
                showTransferDialog = false,
                selectedOrder = null,
                selectedTechnician = null,
                scheduledDate = null,
                error = null
            )
        }
    }
}