package com.dscorp.ispadmin.presentation.ui.features.installationorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.domain.model.InstallationOrder
import com.dscorp.ispadmin.domain.model.InstallationOrderStatus
import com.dscorp.ispadmin.domain.usecase.InstallationOrderUseCase
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.GetUserSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class ClosedOrdersUiState(
    val closedOrders: List<InstallationOrder> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class SellerClosedOrdersViewModel : ViewModel(), KoinComponent {
    private val installationOrderUseCase: InstallationOrderUseCase by inject()
    private val userSessionUseCase: GetUserSessionUseCase by inject()
    
    private val _uiState = MutableStateFlow(ClosedOrdersUiState())
    val uiState: StateFlow<ClosedOrdersUiState> = _uiState.asStateFlow()
    
    fun loadClosedOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            userSessionUseCase().fold(
                onSuccess = { user ->
                    if (user == null) {
                        _uiState.update {
                            it.copy(
                                error = "No se pudo identificar al usuario actual",
                                isLoading = false
                            )
                        }
                        return@launch
                    }
                    
                    val userId = user.id as? Int
                    if (userId == null) {
                        _uiState.update {
                            it.copy(
                                error = "ID de usuario inválido",
                                isLoading = false
                            )
                        }
                        return@launch
                    }
                    
                    loadOrdersForUser(userId)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message ?: "Error al obtener la sesión del usuario",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }
    
    private fun loadOrdersForUser(userId: Int) {
        viewModelScope.launch {
            runCatching {
                installationOrderUseCase.getInstallationOrdersBySellerAndStatus(
                    userId = userId,
                    status = InstallationOrderStatus.CERRADO
                )
            }.fold(
                onSuccess = { orders ->
                    _uiState.update { 
                        it.copy(
                            closedOrders = orders,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            error = error.message ?: "Error al cargar las órdenes",
                            isLoading = false
                        ) 
                    }
                }
            )
        }
    }
    
    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun dismissSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
} 