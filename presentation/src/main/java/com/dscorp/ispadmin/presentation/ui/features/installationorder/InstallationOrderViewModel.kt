package com.dscorp.ispadmin.presentation.ui.features.installationorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.domain.model.InstallationOrder
import com.dscorp.ispadmin.domain.model.InstallationOrderStatus
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.usecase.InstallationOrderUseCase
import com.dscorp.ispadmin.domain.usecase.PlaceUseCase
import com.dscorp.ispadmin.domain.usecase.UserUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class InstallationOrderForm(
    val firstName: String = "",
    val lastName: String = "",
    val address: String = "",
    val phone: String = "",
    val place: Place? = null
)

data class InstallationOrderUiState(
    val form: InstallationOrderForm = InstallationOrderForm(),
    val isFormValid: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val technicians: List<User> = emptyList(),
    val places: List<Place> = emptyList(),
    val orderCreated: InstallationOrder? = null,
    val orderUpdated: InstallationOrder? = null
)

class InstallationOrderViewModel : ViewModel(), KoinComponent {
    private val installationOrderUseCase: InstallationOrderUseCase by inject()
    private val userUseCase: UserUseCase by inject()
    private val placeUseCase: PlaceUseCase by inject()

    private val _uiState = MutableStateFlow(InstallationOrderUiState())
    val uiState: StateFlow<InstallationOrderUiState> = _uiState.asStateFlow()

    val currentUser = runBlocking { userUseCase.getCurrentUser() }

    init {
        loadPlaces()
    }

    fun onFirstNameChange(newName: String) {
        _uiState.update { currentState ->
            currentState.copy(form = currentState.form.copy(firstName = newName))
        }
        validateForm()
    }

    fun onLastNameChange(newLastName: String) {
        _uiState.update { currentState ->
            currentState.copy(form = currentState.form.copy(lastName = newLastName))
        }
        validateForm()
    }

    fun onAddressChange(newAddress: String) {
        _uiState.update { currentState ->
            currentState.copy(form = currentState.form.copy(address = newAddress))
        }
        validateForm()
    }

    fun onPhoneChange(newPhone: String) {
        _uiState.update { currentState ->
            currentState.copy(form = currentState.form.copy(phone = newPhone))
        }
        validateForm()
    }

    fun onPlaceChange(place: Place) {
        _uiState.update { currentState ->
            currentState.copy(form = currentState.form.copy(place = place))
        }
        validateForm()
    }

    private fun validateForm() {
        _uiState.update { currentState ->
            val form = currentState.form
            val isValid = form.firstName.isNotBlank() &&
                    form.lastName.isNotBlank() &&
                    form.address.isNotBlank() &&
                    form.phone.isNotBlank() &&
                    form.place != null
            currentState.copy(isFormValid = isValid)
        }
    }

    private fun loadPlaces() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val places = placeUseCase.getPlaces()
                _uiState.update { it.copy(places = places, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error al cargar lugares"
                    )
                }
            }
        }
    }

    fun createOrder() {
        if (!_uiState.value.isFormValid) {
            _uiState.update { it.copy(error = "Por favor, complete todos los campos.") }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val formState = _uiState.value.form

                // Eliminar espacios en blanco al final de los campos
                val trimmedFirstName = formState.firstName.trim()
                val trimmedLastName = formState.lastName.trim()
                val trimmedAddress = formState.address.trim()

                val newOrder = InstallationOrder(
                    customerFirstName = trimmedFirstName,
                    customerLastName = trimmedLastName,
                    customerAddress = trimmedAddress,
                    customerPhone = formState.phone,
                    status = InstallationOrderStatus.SOLICITADO,
                    seller = User(id = currentUser.id),
                    assignedBy = null,
                    technician = null,
                    scheduledDate = null,
                    cancellationReason = null,
                    subscription = null,
                    place = formState.place // Ahora incluimos el lugar seleccionado
                )
                val result = installationOrderUseCase.createInstallationOrder(newOrder)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Orden de instalación creada correctamente",
                        orderCreated = result
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error al crear la orden de instalación"
                    )
                }
            }
        }
    }


    fun closeInstallationOrder(orderId: Int) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val result =
                    installationOrderUseCase.closeInstallationOrder(orderId = orderId) // Use named argument
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Orden de instalación cerrada correctamente",
                        orderUpdated = result
                    )
                }
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

    fun cancelInstallationOrder(orderId: Int, cancellationReason: String? = null) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val result = installationOrderUseCase.cancelInstallationOrder(
                    orderId = orderId,
                    cancellationReason = cancellationReason
                ) // Use named arguments
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Orden de instalación cancelada correctamente",
                        orderUpdated = result
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error al cancelar la orden de instalación"
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun dismissSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

}