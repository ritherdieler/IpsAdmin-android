package com.dscorp.ispadmin.presentation.ui.features.supportTicket.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.apirequestmodel.AssistanceTicketRequest
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionFastSearchResponse
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.toTitleCase
import com.dscorp.ispadmin.observability.ObsBreadcrumbCategory
import com.dscorp.ispadmin.observability.ObservabilityClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateSupportTicketViewModel(
    private val repository: IRepository,
    private val observabilityClient: ObservabilityClient,
) : ViewModel() {

    private companion object {
        const val OBS_FEATURE = "support_ticket"
        const val OBS_SCREEN = "create_support_ticket"
    }

    private val _uiState = MutableStateFlow(CreateSupportTicketUiState())
    val uiState: StateFlow<CreateSupportTicketUiState> = _uiState.asStateFlow()

    val categories = TICKET_CATEGORIES

    init {
        getPlaces()
    }

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
                observabilityClient.reportError(
                    throwable = e,
                    message = "Fallo al cargar lugares para crear ticket",
                    tags = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "action" to "load_places")
                )
                _uiState.update {
                    it.copy(
                        error = "Error al cargar lugares: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

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

    fun updateIsClient(isClient: Boolean) {
        _uiState.update {
            if (isClient) {
                it.copy(
                    isClient = true,
                    selectedPlace = null,
                    customerNameError = null
                )
            } else {
                it.copy(
                    isClient = false,
                    selectedSubscription = null,
                    customerNameError = null,
                    phone = "",
                    phoneError = null
                ).clearedClientDetails()
            }
        }
    }

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

    fun updateSelectedSubscription(subscription: SubscriptionFastSearchResponse?) {
        if (subscription == null) {
            _uiState.update {
                it.copy(
                    selectedSubscription = null,
                    subscriptionError = null,
                    phone = "",
                    phoneError = null
                ).clearedClientDetails()
            }
            return
        }

        _uiState.update {
            it.copy(
                selectedSubscription = subscription,
                subscriptionError = null,
                isLoadingSubscription = true,
                hasLoadedClientDetails = false,
                subscriptionLoadError = null,
                phone = "",
                phoneError = null,
                clientName = "",
                clientIp = "",
                clientLocation = ""
            )
        }
        loadSubscriptionDetails(subscription.id)
    }

    private fun loadSubscriptionDetails(subscriptionId: Int) {
        viewModelScope.launch {
            try {
                val details = repository.subscriptionById(subscriptionId)
                _uiState.update { current ->
                    if (current.selectedSubscription?.id != subscriptionId) {
                        current
                    } else {
                        val fullName = details.getFullName().trim()
                        current.copy(
                            isLoadingSubscription = false,
                            hasLoadedClientDetails = true,
                            subscriptionLoadError = null,
                            phone = details.phone.orEmpty(),
                            clientName = fullName,
                            clientIp = details.ip.orEmpty(),
                            clientLocation = details.place?.name.orEmpty(),
                            clientPlan = details.plan?.name.orEmpty(),
                            clientStatus = details.serviceStatus,
                            selectedSubscription = current.selectedSubscription?.copy(fullName = fullName)
                        )
                    }
                }
            } catch (e: Exception) {
                observabilityClient.reportError(
                    throwable = e,
                    message = "Fallo al cargar datos de la suscripción para ticket",
                    tags = mapOf(
                        "feature" to OBS_FEATURE,
                        "screen" to OBS_SCREEN,
                        "action" to "load_subscription",
                        "entityId" to subscriptionId
                    )
                )
                _uiState.update { current ->
                    if (current.selectedSubscription?.id != subscriptionId) {
                        current
                    } else {
                        current.copy(
                            isLoadingSubscription = false,
                            hasLoadedClientDetails = false,
                            subscriptionLoadError = "Error al cargar datos del cliente: ${e.message}",
                            phone = "",
                            clientName = "",
                            clientIp = "",
                            clientLocation = ""
                        )
                    }
                }
            }
        }
    }

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
                observabilityClient.reportError(
                    throwable = e,
                    message = "Fallo al buscar suscripciones para ticket",
                    tags = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "action" to "find_subscription")
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al buscar suscripciones: ${e.message}"
                    )
                }
            }
        }
    }

    fun createTicket() {
        viewModelScope.launch {
            observabilityClient.addBreadcrumb(
                category = ObsBreadcrumbCategory.USER_ACTION,
                message = "$OBS_FEATURE.create",
                data = mapOf(
                    "feature" to OBS_FEATURE,
                    "screen" to OBS_SCREEN,
                    "category" to _uiState.value.category,
                    "isClient" to _uiState.value.isClient,
                    "entityId" to _uiState.value.selectedSubscription?.id
                )
            )
            try {
                _uiState.update { it.copy(isLoading = true) }

                val state = _uiState.value
                val isValid = validateForm(state)

                if (isValid) {
                    val ticketRequest = AssistanceTicketRequest(
                        phone = state.phone,
                        category = state.category,
                        description = state.description,
                        subscriptionId = state.selectedSubscription?.id,
                        customerName = if (state.isClient) {
                            state.clientName.ifBlank { state.selectedSubscription?.fullName ?: "" }
                        } else {
                            state.customerName
                        },
                        placeName = state.selectedPlace?.name
                    )

                    val created = repository.createTicket(ticketRequest)
                    val assignError = state.assigneeId?.let { assigneeId ->
                        runCatching {
                            repository.assignSupportTicketToUser(created.id, AssistanceTicketStatus.ASSIGNED, assigneeId)
                        }.exceptionOrNull()?.let { "Ticket #${created.id} creado, pero no se pudo asignar" }
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isTicketCreated = true,
                            error = assignError,
                        )
                    }
                } else {
                    updateFormErrors()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Por favor complete correctamente todos los campos"
                        )
                    }
                }
            } catch (e: Exception) {
                observabilityClient.reportError(
                    throwable = e,
                    message = "Fallo al crear ticket de soporte",
                    tags = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "action" to "create", "category" to _uiState.value.category)
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al crear ticket: ${e.message}"
                    )
                }
            }
        }
    }

    private fun validateForm(state: CreateSupportTicketUiState): Boolean {
        if (state.category.isEmpty() || state.description.isEmpty()) {
            return false
        }

        return if (state.isClient) {
            state.selectedSubscription != null && state.hasLoadedClientDetails
        } else {
            state.phone.isNotEmpty() &&
                state.phone.length == 9 &&
                state.selectedPlace != null &&
                state.customerName.isNotBlank()
        }
    }

    private fun updateFormErrors() {
        val state = _uiState.value
        _uiState.update {
            it.copy(
                phoneError = if (!state.isClient) {
                    if (state.phone.isEmpty()) "El teléfono es obligatorio"
                    else if (state.phone.length != 9) "El teléfono debe tener 9 dígitos" else null
                } else {
                    null
                },
                categoryError = if (state.category.isEmpty()) "La categoría es obligatoria" else null,
                descriptionError = if (state.description.isEmpty()) "La descripción es obligatoria" else null,
                subscriptionError = when {
                    state.isClient && state.selectedSubscription == null -> "Debe seleccionar un cliente"
                    state.isClient && !state.hasLoadedClientDetails -> "Debe cargar los datos del cliente"
                    else -> null
                },
                placeError = if (!state.isClient && state.selectedPlace == null) "Debe seleccionar un lugar" else null,
                customerNameError = if (!state.isClient && state.customerName.isBlank()) "El nombre completo es obligatorio" else null
            )
        }
    }

    fun loadTechnicians() {
        if (_uiState.value.technicians.isNotEmpty()) return
        viewModelScope.launch {
            runCatching { repository.getTechnicians() }
                .onSuccess { users ->
                    val options = users.mapNotNull { user ->
                        user.id?.let { id -> id to "${user.name} ${user.lastName.orEmpty()}".trim().toTitleCase() }
                    }
                    _uiState.update { it.copy(technicians = options) }
                }
                .onFailure { e ->
                    observabilityClient.reportError(
                        throwable = e,
                        message = "Fallo al cargar técnicos para crear ticket",
                        tags = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "action" to "load_technicians")
                    )
                }
        }
    }

    fun updateAssignee(userId: Int?) {
        _uiState.update { it.copy(assigneeId = userId) }
    }

    fun resetError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetTicketCreated() {
        _uiState.update { it.copy(isTicketCreated = false) }
    }

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

data class CreateSupportTicketUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isTicketCreated: Boolean = false,

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
    val clientName: String = "",
    val clientIp: String = "",
    val clientLocation: String = "",
    val clientPlan: String = "",
    val clientStatus: ServiceStatus? = null,
    val isLoadingSubscription: Boolean = false,
    val hasLoadedClientDetails: Boolean = false,
    val subscriptionLoadError: String? = null,

    val subscriptions: List<SubscriptionFastSearchResponse> = emptyList(),
    val places: List<Place> = emptyList(),
    /** Optional technician for the new ticket: (id, name). */
    val technicians: List<Pair<Int, String>> = emptyList(),
    val assigneeId: Int? = null,
)

private fun CreateSupportTicketUiState.clearedClientDetails() = copy(
    clientName = "",
    clientPlan = "",
    clientStatus = null,
    clientIp = "",
    clientLocation = "",
    isLoadingSubscription = false,
    hasLoadedClientDetails = false,
    subscriptionLoadError = null
)
