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
import java.time.LocalDateTime

/**
 * ViewModel para la lista de órdenes de instalación.
 * Implementa la paginación utilizando Paging 3.
 */
class InstallationOrderListViewModel(
    private val installationOrderUseCase: InstallationOrderUseCase,
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
        loadCurrentUser()
    }

    /**
     * Carga las órdenes de instalación utilizando la paginación de acuerdo al tipo de usuario.
     * El resultado se almacena en el uiState.
     */
    internal fun loadInstallationOrders() {
        val ordersFlow = when (uiState.value.currentUser?.type) {
            User.UserType.TECHNICIAN -> installationOrderUseCase.getInstallationOrdersByTechnicianPaginated(
                uiState.value.currentUser?.id!!
            )

            User.UserType.SALES -> installationOrderUseCase.getInstallationOrdersBySellerPaginated(
                uiState.value.currentUser?.id!!
            )

            User.UserType.ADMIN, User.UserType.SECRETARY, User.UserType.ACCOUNTANT -> installationOrderUseCase.getAllInstallationOrdersPaginated()
            else -> throw Exception("Tipo de usuario no soportado para cargar órdenes de instalación")
        }.cachedIn(viewModelScope)

        val filteredFlow = if (currentStatusFilter != null) {
            // Aplicamos el filtro por estado si existe
            ordersFlow.map { pagingData ->
                pagingData.filter { order -> order.status == currentStatusFilter }
            }
        } else {
            // Si no hay filtro, usamos el flujo original
            ordersFlow
        }

        _uiState.update { state ->
            state.copy(
                installationOrders = filteredFlow,
                isLoading = false
            )
        }
    }

    /**
     * Carga el usuario actual y actualiza el estado
     */
    internal fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val currentUser = userUseCase.getCurrentUser()
                _uiState.update { state ->
                    state.copy(
                        currentUser = currentUser
                    )
                }
            } catch (e: Exception) {
                // Manejar error si es necesario
            }
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
     * Verifica si el usuario actual puede crear órdenes de instalación
     */
    fun canCreateOrder(): Boolean {
        val userType = _uiState.value.currentUser!!.type
        return userType == User.UserType.ADMIN ||
                userType == User.UserType.SALES ||
                userType == User.UserType.SECRETARY ||
                userType == User.UserType.ACCOUNTANT
    }

    fun onOrderSelected(order: InstallationOrder) {
        if (order.status == InstallationOrderStatus.SOLICITADO && canAsignInstallationOrder()) {
            _uiState.update {
                it.copy(
                    selectedOrder = order,
                    showAssignDialog = true
                )
            }
        } else if (uiState.value.currentUser?.type == User.UserType.TECHNICIAN && order.status == InstallationOrderStatus.EN_CURSO) {
            _uiState.update {
                it.copy(
                    selectedOrder = order,
                    navigateToRegisterSubscription = true
                )
            }
        }
    }

    private fun canAsignInstallationOrder(): Boolean =
        uiState.value.currentUser!!.type == User.UserType.ACCOUNTANT || uiState.value.currentUser!!.type == User.UserType.SECRETARY || uiState.value.currentUser!!.type == User.UserType.ADMIN

    fun onCloseDialog() {
        _uiState.update {
            it.copy(
                showAssignDialog = false,
                selectedOrder = null,
                selectedTechnician = null,
                scheduledDate = null
            )
        }
    }

    fun onTechnicianSelected(technician: User) {
        _uiState.update { it.copy(selectedTechnician = technician) }
    }

    fun onScheduledDateSelected(date: LocalDateTime) {
        _uiState.update { it.copy(scheduledDate = date) }
    }

    internal fun loadTechnicians() {
        viewModelScope.launch {
            try {
                val technicians = userUseCase.getTechnicianUsers()
                _uiState.update { it.copy(technicians = technicians) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Error al cargar técnicos"
                    )
                }
            }
        }
    }

    fun assignTechnician() {
        val order = _uiState.value.selectedOrder
        val technician = _uiState.value.selectedTechnician
        val scheduledDate = _uiState.value.scheduledDate

        if (order == null || technician == null || scheduledDate == null) {
            _uiState.update {
                it.copy(
                    error = "Debe seleccionar un técnico y una fecha"
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val currentUser = userUseCase.getCurrentUser()
                val result = installationOrderUseCase.assignTechnician(
                    orderId = order.id,
                    technicianId = technician.id
                        ?: throw IllegalStateException("El técnico no tiene ID"),
                    assignedById = currentUser.id
                        ?: throw IllegalStateException("El usuario actual no tiene ID"),
                    scheduledDate = scheduledDate
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Técnico asignado correctamente",
                        orderUpdated = result,
                        showAssignDialog = false
                    )
                }

                loadInstallationOrders()

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error al asignar técnico",
                        showAssignDialog = false
                    )
                }
            }
        }
    }

    fun resetSelectedOrder() {
        _uiState.update {
            it.copy(
                selectedOrder = null,
                selectedTechnician = null,
                scheduledDate = null,
                showAssignDialog = false,
                navigateToRegisterSubscription = false
            )
        }
    }

    fun onTransferOrderClicked(order: InstallationOrder) {
        if (order.status == InstallationOrderStatus.EN_CURSO) {
            _uiState.update {
                it.copy(
                    selectedOrder = order,
                    showTransferDialog = true
                )
            }
        }
    }

    fun transferOrder() {
        val order = _uiState.value.selectedOrder
        val technician = _uiState.value.selectedTechnician
        val scheduledDate = _uiState.value.scheduledDate
        val currentUser = _uiState.value.currentUser

        if (order == null || technician == null || scheduledDate == null || currentUser == null) {
            _uiState.update { it.copy(error = "Faltan datos para transferir la orden") }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                installationOrderUseCase.transferInstallationOrder(
                    orderId = order.id,
                    newTechnicianId = technician.id!!,
                    transferredById = currentUser.id!!,
                    scheduledDate = scheduledDate
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showTransferDialog = false,
                        selectedOrder = null,
                        selectedTechnician = null,
                        scheduledDate = null,
                        successMessage = "Orden transferida exitosamente"
                    )
                }
                loadInstallationOrders()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error al transferir la orden"
                    )
                }
            }
        }
    }

    fun onCloseTransferDialog() {
        _uiState.update {
            it.copy(
                showTransferDialog = false,
                selectedOrder = null,
                selectedTechnician = null,
                scheduledDate = null
            )
        }
    }
}

/**
 * Representa el estado de la UI para la lista de órdenes de instalación
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
    val navigateToRegisterSubscription: Boolean = false
)