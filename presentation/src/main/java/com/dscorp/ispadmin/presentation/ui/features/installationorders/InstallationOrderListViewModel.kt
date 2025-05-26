package com.dscorp.ispadmin.presentation.ui.features.installationorders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.dscorp.ispadmin.data.model.InstallationOrderStatus
import com.dscorp.ispadmin.domain.model.InstallationOrder
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.usecase.InstallationOrderUseCase
import com.dscorp.ispadmin.domain.usecase.UserUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
        loadInstallationOrders()
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

        _uiState.update { state ->
            state.copy(
                installationOrders = ordersFlow,
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
     * Actualiza la lista de órdenes de instalación, útil para forzar una recarga
     */
    fun refreshInstallationOrders() {
        _uiState.update { it.copy(isLoading = true) }
        loadInstallationOrders()
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
}

/**
 * Representa el estado de la UI para la lista de órdenes de instalación
 */
data class InstallationOrderListUiState(
    val installationOrders: Flow<PagingData<InstallationOrder>>? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentUser: User? = null
)