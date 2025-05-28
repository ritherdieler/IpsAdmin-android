package com.dscorp.ispadmin.presentation.ui.features.installationorders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.model.InstallationOrderStatus
import com.dscorp.ispadmin.domain.model.InstallationOrder
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

// Eventos que pueden ocurrir en la UI
sealed class InstallationOrderEvent {
    data class OnFirstNameChange(val firstName: String) : InstallationOrderEvent()
    data class OnLastNameChange(val lastName: String) : InstallationOrderEvent()
    data class OnAddressChange(val address: String) : InstallationOrderEvent()
    data class OnPhoneChange(val phone: String) : InstallationOrderEvent()
    data class OnDniChange(val dni: String) : InstallationOrderEvent()
    data class OnPlaceChange(val place: Place?) : InstallationOrderEvent()
    object OnCreateOrder : InstallationOrderEvent()
    object OnDismissError : InstallationOrderEvent()
    object OnDismissSuccess : InstallationOrderEvent()
    object OnLoadPlaces : InstallationOrderEvent()
}

// Estado de la UI
data class InstallationOrderForm(
    val firstName: String = "",
    val lastName: String = "",
    val address: String = "",
    val phone: String = "",
    val dni: String = "",
    val place: Place? = null
)

data class InstallationOrderUiState(
    val form: InstallationOrderForm = InstallationOrderForm(),
    val isFormValid: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val places: List<Place> = emptyList(),
    val orderCreated: InstallationOrder? = null
)

class CreateInstallationOrderViewModel(
    private val installationOrderUseCase: InstallationOrderUseCase,
    private val userUseCase: UserUseCase,
    private val placeUseCase: PlaceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InstallationOrderUiState())
    val uiState: StateFlow<InstallationOrderUiState> = _uiState.asStateFlow()

    private val currentUser = runBlocking { userUseCase.getCurrentUser() }

    fun onEvent(event: InstallationOrderEvent) {
        when (event) {
            is InstallationOrderEvent.OnFirstNameChange -> {
                _uiState.update { currentState ->
                    currentState.copy(form = currentState.form.copy(firstName = event.firstName))
                }
                validateForm()
            }
            is InstallationOrderEvent.OnLastNameChange -> {
                _uiState.update { currentState ->
                    currentState.copy(form = currentState.form.copy(lastName = event.lastName))
                }
                validateForm()
            }
            is InstallationOrderEvent.OnAddressChange -> {
                _uiState.update { currentState ->
                    currentState.copy(form = currentState.form.copy(address = event.address))
                }
                validateForm()
            }
            is InstallationOrderEvent.OnPhoneChange -> {
                _uiState.update { currentState ->
                    currentState.copy(form = currentState.form.copy(phone = event.phone))
                }
                validateForm()
            }
            is InstallationOrderEvent.OnDniChange -> {
                _uiState.update { currentState ->
                    currentState.copy(form = currentState.form.copy(dni = event.dni))
                }
                validateForm()
            }
            is InstallationOrderEvent.OnPlaceChange -> {
                _uiState.update { currentState ->
                    currentState.copy(form = currentState.form.copy(place = event.place))
                }
                validateForm()
            }
            InstallationOrderEvent.OnCreateOrder -> createOrder()
            InstallationOrderEvent.OnDismissError -> dismissError()
            InstallationOrderEvent.OnDismissSuccess -> dismissSuccess()
            InstallationOrderEvent.OnLoadPlaces -> loadPlaces()
        }
    }

    private fun validateForm() {
        _uiState.update { currentState ->
            val form = currentState.form
            val isValid = form.firstName.isNotBlank() &&
                    form.lastName.isNotBlank() &&
                    form.address.isNotBlank() &&
                    form.phone.isNotBlank() &&
                    form.dni.isNotBlank() &&
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

    private fun createOrder() {
        if (!_uiState.value.isFormValid) {
            _uiState.update { it.copy(error = "Por favor, complete todos los campos.") }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val formState = _uiState.value.form

                val newOrder = InstallationOrder(
                    customerFirstName = formState.firstName.trim(),
                    customerLastName = formState.lastName.trim(),
                    customerAddress = formState.address.trim(),
                    customerPhone = formState.phone,
                    customerDni = formState.dni,
                    status = InstallationOrderStatus.SOLICITADO,
                    seller = User(id = currentUser.id),
                    assignedBy = null,
                    technician = null,
                    scheduledDate = null,
                    cancellationReason = null,
                    subscription = null,
                    place = formState.place
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
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error al crear la orden de instalación"
                    )
                }
            }
        }
    }

    private fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun dismissSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}