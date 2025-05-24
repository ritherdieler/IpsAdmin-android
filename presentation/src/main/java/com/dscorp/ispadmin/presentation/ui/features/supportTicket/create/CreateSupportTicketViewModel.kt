package com.dscorp.ispadmin.presentation.ui.features.supportTicket.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.apirequestmodel.AssistanceTicketRequest
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.SubscriptionFastSearchResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateSupportTicketViewModel(
    private val repository: IRepository,
) : ViewModel() {

    // Estado principal de la pantalla usando UDF
    private val _uiState = MutableStateFlow(CreateSupportTicketUiState())
    val uiState: StateFlow<CreateSupportTicketUiState> = _uiState.asStateFlow()

    // Categorías para los tickets de soporte
    val categories = listOf(
        "Sin Conexión a Internet",
        "Internet Lento",
        "Migración a fibra óptica",
        "Cambio de Domicilio",
        "Cambio de Contraseña",
        "Ruptura de cable última milla",
        "Alineamiento de antena CPE",
        "Instalación de Tv Cable",
        "Añadir Tv Cable a su plan de internet",
        "No tiene señal de Tv Cable",
        "Cambio de Onu",
        "Cambio de Router",
        "Instalación de repetidor",
        "Evaluar Factibilidad de Servicio",
        "Instalación de Internet",
        "Otros",
    )

    // Inicializar los datos al crear el ViewModel
    init {
        getPlaces()
    }

    // Obtener lugares disponibles
    private fun getPlaces() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val response = repository.getPlaces()
                _uiState.update { 
                    it.copy(
                        places = response,
                        isLoading = false
                    ) 
                }
            } catch (e: Exception) {
                // Manejar el error sin afectar el estado principal
                _uiState.update { 
                    it.copy(
                        error = "Error al cargar lugares: ${e.message}",
                        isLoading = false
                    ) 
                }
            }
        }
    }

    // Actualizar el teléfono
    fun updatePhone(phone: String) {
        _uiState.update {
            val phoneError = if (phone.isEmpty()) {
                "El teléfono es obligatorio"
            } else if (phone.length != 9) {
                "El teléfono debe tener 9 dígitos"
            } else {
                null
            }
            
            it.copy(
                phone = phone,
                phoneError = phoneError
            )
        }
    }
    
    // Actualizar la categoría
    fun updateCategory(category: String) {
        _uiState.update {
            val categoryError = if (category.isEmpty()) {
                "La categoría es obligatoria"
            } else {
                null
            }
            
            it.copy(
                category = category,
                categoryError = categoryError
            )
        }
    }
    
    // Actualizar la descripción
    fun updateDescription(description: String) {
        _uiState.update {
            val descriptionError = if (description.isEmpty()) {
                "La descripción es obligatoria"
            } else if (description.length > 300) {
                "La descripción no puede superar los 300 caracteres"
            } else {
                null
            }
            
            it.copy(
                description = description,
                descriptionError = descriptionError
            )
        }
    }
    
    // Actualizar el tipo de cliente
    fun updateIsClient(isClient: Boolean) {
        _uiState.update {
            it.copy(
                isClient = isClient,
                // Resetear selecciones según el tipo de cliente
                selectedPlace = if (isClient) null else it.selectedPlace,
                selectedSubscription = if (!isClient) null else it.selectedSubscription,
                customerNameError = null
            )
        }
    }
    
    // Actualizar el lugar seleccionado
    fun updateSelectedPlace(place: Place?) {
        _uiState.update {
            val placeError = if (!it.isClient && place == null) {
                "Debe seleccionar un lugar"
            } else {
                null
            }
            
            it.copy(
                selectedPlace = place,
                placeError = placeError
            )
        }
    }
    
    // Actualizar la suscripción seleccionada
    fun updateSelectedSubscription(subscription: SubscriptionFastSearchResponse?) {
        _uiState.update {
            val subscriptionError = if (it.isClient && subscription == null) {
                "Debe seleccionar un cliente"
            } else {
                null
            }
            
            it.copy(
                selectedSubscription = subscription,
                subscriptionError = subscriptionError
            )
        }
    }

    // Buscar suscripciones por nombre
    fun findSubscriptionByNames(names: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val response = repository.findSubscriptionByNames(names)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        subscriptions = response
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al buscar suscripciones: ${e.message}"
                    )
                }
            }
        }
    }

    // Crear un ticket de soporte
    fun createTicket() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                // Validar campos obligatorios
                val state = _uiState.value
                val isValid = validateForm(state)
                
                if (isValid) {
                    // Crear objeto de request con los datos del formulario
                    val ticketRequest = AssistanceTicketRequest(
                        phone = state.phone,
                        category = state.category,
                        description = state.description,
                        subscriptionId = state.selectedSubscription?.id,
                        customerName = if (state.isClient) state.selectedSubscription?.fullName ?: "" else state.customerName,
                        placeName = state.selectedPlace?.name
                    )
                    
                    repository.createTicket(ticketRequest)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isTicketCreated = true
                        )
                    }
                } else {
                    // Actualizar errores si el formulario no es válido
                    updateFormErrors()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Por favor complete correctamente todos los campos"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al crear ticket: ${e.message}"
                    )
                }
            }
        }
    }
    
    // Validar el formulario completo
    private fun validateForm(state: CreateSupportTicketUiState): Boolean {
        // Verificar campos básicos
        if (state.phone.isEmpty() || state.phone.length != 9 || 
            state.category.isEmpty() || state.description.isEmpty()) {
            return false
        }
        
        // Verificar campos específicos según tipo de cliente
        return if (state.isClient) {
            state.selectedSubscription != null
        } else {
            state.selectedPlace != null && !state.customerName.isNullOrBlank()
        }
    }
    
    // Actualizar errores del formulario
    private fun updateFormErrors() {
        val state = _uiState.value
        _uiState.update {
            it.copy(
                phoneError = if (state.phone.isEmpty()) "El teléfono es obligatorio" 
                    else if (state.phone.length != 9) "El teléfono debe tener 9 dígitos" else null,
                categoryError = if (state.category.isEmpty()) "La categoría es obligatoria" else null,
                descriptionError = if (state.description.isEmpty()) "La descripción es obligatoria" else null,
                subscriptionError = if (state.isClient && state.selectedSubscription == null) "Debe seleccionar un cliente" else null,
                placeError = if (!state.isClient && state.selectedPlace == null) "Debe seleccionar un lugar" else null,
                customerNameError = if (!state.isClient && state.customerName.isBlank()) "El nombre completo es obligatorio" else null
            )
        }
    }

    // Resetear el error
    fun resetError() {
        _uiState.update { it.copy(error = null) }
    }

    // Resetear el estado de ticketCreated
    fun resetTicketCreated() {
        _uiState.update { it.copy(isTicketCreated = false) }
    }

    // Función para actualizar el nombre del cliente cuando no es cliente
    fun updateCustomerName(name: String) {
        _uiState.update {
            val customerNameError = if (!it.isClient && name.isEmpty()) {
                "El nombre completo es obligatorio"
            } else {
                null
            }
            
            it.copy(
                customerName = name,
                customerNameError = customerNameError
            )
        }
    }
}

// Estado de la UI siguiendo UDF
data class CreateSupportTicketUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isTicketCreated: Boolean = false,
    
    // Campos del formulario
    val phone: String = "",
    val phoneError: String? = null,
    val category: String = "",
    val categoryError: String? = null,
    val description: String = "",
    val descriptionError: String? = null,
    val isClient: Boolean = true,
    val selectedSubscription: SubscriptionFastSearchResponse? = null,
    val subscriptionError: String? = null,
    val selectedPlace: Place? = null,
    val placeError: String? = null,
    val customerName: String = "",
    val customerNameError: String? = null,
    
    // Datos para los dropdowns
    val subscriptions: List<SubscriptionFastSearchResponse> = emptyList(),
    val places: List<Place> = emptyList()
) 