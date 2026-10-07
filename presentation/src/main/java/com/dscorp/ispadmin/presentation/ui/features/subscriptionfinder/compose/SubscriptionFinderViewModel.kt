package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.apirequestmodel.MoveOnuRequest
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.CustomerData
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.domain.model.SubscriptionResume
import com.dscorp.ispadmin.domain.model.extensions.isAValidAddress
import com.dscorp.ispadmin.domain.model.extensions.isAValidName
import com.dscorp.ispadmin.domain.model.extensions.isValidDni
import com.dscorp.ispadmin.domain.model.extensions.isValidEmail
import com.dscorp.ispadmin.domain.model.extensions.isValidPhone
import com.dscorp.ispadmin.domain.usecase.service.ReactivateServiceUseCase
import com.dscorp.ispadmin.domain.usecase.service.RebootFiberOnuUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.RetryTr069ProvisioningUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.SearchSubscriptionsUseCase
import com.dscorp.ispadmin.observability.ObsBreadcrumbCategory
import com.dscorp.ispadmin.observability.ObservabilityClient
import com.dscorp.ispadmin.presentation.extension.removeAccents
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asSharedFlow
const val REQUEST_DELAY = 500L

data class SubscriptionFinderUiState(
    val subscriptions: Map<ServiceStatus, List<SubscriptionResume>> = emptyMap(),
    val cancelSubscriptionState: CancelSubscriptionState = CancelSubscriptionState.Empty,
    val reactivateServiceState: ReactivateServiceState = ReactivateServiceState.Empty,
    val saveSubscriptionState: SaveSubscriptionState = SaveSubscriptionState.Success,
    val napBoxesState: NapBoxesState = NapBoxesState.Loading,
    val placesState: PlacesState = PlacesState(),
    val selectedSubscription: SubscriptionResume? = null,
    val customerFormData: CustomerFormData? = null,
    val showLocationUpdateDialog: Boolean = false,
    val showReactivateDialog: Boolean = false,
    val reactivationNotes: String = "",
    val editableLatitude: String = "",
    val editableLongitude: String = "",
    val isFetchingCurrentLocation: Boolean = false,
    val lastUsedFilter: SubscriptionFilter? = null,
    val rebootOnuState: RebootOnuState = RebootOnuState.Empty,
    val retryTr069State: RetryTr069State = RetryTr069State.Empty,
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val searchPerformed: Boolean = false,
    val currentPage: Int = 0,
    val canLoadMore: Boolean = false,
)

data class CustomerFormData(
    val name: String = "",
    val nameError: String? = null,
    val lastName: String = "",
    val lastNameError: String? = null,
    val phone: String = "",
    val phoneError: String? = null,
    val dni: String = "",
    val dniError: String? = null,
    val address: String = "",
    val addressError: String? = null,
    val email: String = "",
    val emailError: String? = null,
    val place: String = "",
    val placeError: String? = null,
    val placeId: Int = 0,
    val subscriptionId: Int = 0,
    val sharedServiceCount: Int = 0,
) {

    private fun isValidEmail() = email.isEmpty() || email.isValidEmail()

    fun isValid(): Boolean {
        return nameError == null &&
                lastNameError == null &&
                phoneError == null &&
                dniError == null &&
                addressError == null &&
                emailError == null &&
                name.isNotBlank() &&
                lastName.isNotBlank() &&
                phone.isNotBlank() &&
                dni.isNotBlank() &&
                address.isNotBlank() &&
                isValidEmail()
    }
}

class SubscriptionFinderViewModel(
    private val repository: IRepository,
    private val reactivateServiceUseCase: ReactivateServiceUseCase,
    private val rebootFiberOnuUseCase: RebootFiberOnuUseCase,
    private val searchSubscriptionsUseCase: SearchSubscriptionsUseCase,
    private val observabilityClient: ObservabilityClient,
    private val retryTr069ProvisioningUseCase: RetryTr069ProvisioningUseCase,
    private val subscriptionActionsRepository: com.dscorp.ispadmin.domain.repository.SubscriptionActionsRepository? = null,
) : ViewModel() {

    private companion object {
        const val OBS_FEATURE = "subscription"
        const val OBS_SCREEN = "subscription_finder"
        const val PAGE_SIZE = 20
    }

    private var currentQuery: String = ""
    private var currentStatus: String? = null
    private var initialized = false

    private val _uiState = MutableStateFlow(SubscriptionFinderUiState())
    val uiState: StateFlow<SubscriptionFinderUiState> = _uiState.asStateFlow()
    // Emite mensajes de confirmacion o error despues de intentar guardar datos del cliente.
    private val _customerSaveMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val customerSaveMessages = _customerSaveMessages.asSharedFlow()
    private val subscriptionsFlow = MutableStateFlow<List<SubscriptionResume>>(emptyList())

    val documentNumberFlow = MutableSharedFlow<SubscriptionFilter>(extraBufferCapacity = 1)

    fun initialize() {
        if (initialized) return
        initialized = true
        observeSubscriptions()
        findSubscription()
        getPlaces()
    }

    fun observeSubscriptions() = viewModelScope.launch {
        subscriptionsFlow.map { list ->
            list.map {
                if (it.serviceStatus != ServiceStatus.CANCELLED) it.copy(serviceStatus = ServiceStatus.ACTIVE)
                else it.copy(serviceStatus = ServiceStatus.CANCELLED)
            }.groupBy { it.serviceStatus }
        }.collect { groupedSubscriptions ->
            _uiState.update { it.copy(subscriptions = groupedSubscriptions) }
        }
    }

    fun resetNapBoxFlow() {
        _uiState.update { it.copy(napBoxesState = NapBoxesState.Loading) }
    }

    @OptIn(FlowPreview::class)
    fun findSubscription() = viewModelScope.launch {
        documentNumberFlow.debounce(REQUEST_DELAY)
            .collect { filterType ->
                _uiState.update { it.copy(lastUsedFilter = filterType) }
                observabilityClient.addBreadcrumb(
                    category = ObsBreadcrumbCategory.USER_ACTION,
                    message = "$OBS_FEATURE.find",
                    data = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "filterType" to filterType::class.simpleName)
                )
                if (filterType is SubscriptionFilter.BY_NAME) {
                    searchByName(filterType)
                } else {
                    legacyFind(filterType)
                }
            }
    }

    private suspend fun searchByName(filterType: SubscriptionFilter.BY_NAME) {
        val query = "${filterType.name} ${filterType.lastName}".trim()
        if (query.isEmpty()) {
            currentQuery = ""
            subscriptionsFlow.value = emptyList()
            _uiState.update {
                it.copy(
                    isSearching = false,
                    searchPerformed = false,
                    searchError = null,
                    currentPage = 0,
                    canLoadMore = false
                )
            }
            return
        }
        currentQuery = query
        _uiState.update { it.copy(isSearching = true, searchError = null, searchPerformed = true) }
        searchSubscriptionsUseCase(query = query, status = currentStatus, page = 0, size = PAGE_SIZE).fold(
            onSuccess = { result ->
                subscriptionsFlow.value = result.items
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        currentPage = result.page,
                        canLoadMore = result.canLoadMore
                    )
                }
            },
            onFailure = { error ->
                observabilityClient.reportError(
                    throwable = error,
                    message = "Fallo en búsqueda de suscripciones",
                    tags = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "action" to "search", "filterType" to filterType::class.simpleName)
                )
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        canLoadMore = false,
                        searchError = error.message ?: "No se pudo completar la búsqueda"
                    )
                }
            }
        )
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isSearching || !state.canLoadMore) return
        if (state.lastUsedFilter !is SubscriptionFilter.BY_NAME || currentQuery.isEmpty()) return

        viewModelScope.launch {
            val nextPage = state.currentPage + 1
            _uiState.update { it.copy(isSearching = true, searchError = null) }
            searchSubscriptionsUseCase(query = currentQuery, status = currentStatus, page = nextPage, size = PAGE_SIZE).fold(
                onSuccess = { result ->
                    subscriptionsFlow.value = subscriptionsFlow.value + result.items
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            currentPage = result.page,
                            canLoadMore = result.canLoadMore
                        )
                    }
                },
                onFailure = { error ->
                    observabilityClient.reportError(
                        throwable = error,
                        message = "Fallo al cargar más resultados",
                        tags = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "action" to "search_next_page")
                    )
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            searchError = error.message ?: "No se pudieron cargar más resultados"
                        )
                    }
                }
            )
        }
    }

    fun clearSearchError() {
        _uiState.update { it.copy(searchError = null) }
    }

    private suspend fun legacyFind(filterType: SubscriptionFilter) {
        _uiState.update { it.copy(isSearching = true, searchError = null, canLoadMore = false) }
        try {
            val response = when (filterType) {
                is SubscriptionFilter.BY_DATE -> {
                    if (filterType.startDate.isEmpty() || filterType.endDate.isEmpty()) {
                        subscriptionsFlow.value = emptyList()
                        _uiState.update { it.copy(isSearching = false, searchPerformed = false) }
                        return
                    }
                    repository.findSubscriptionBySubscriptionDate(
                        filterType.startDate,
                        filterType.endDate
                    )
                }

                is SubscriptionFilter.BY_DOCUMENT -> {
                    if (filterType.documentNumber.isEmpty()) {
                        subscriptionsFlow.value = emptyList()
                        _uiState.update { it.copy(isSearching = false, searchPerformed = false) }
                        return
                    } else {
                        repository.findSubscriptionByDNI(filterType.documentNumber)
                    }
                }

                is SubscriptionFilter.BY_IP -> {
                    if (filterType.ip.isEmpty()) {
                        subscriptionsFlow.value = emptyList()
                        _uiState.update { it.copy(isSearching = false, searchPerformed = false) }
                        return
                    } else {
                        repository.findSubscriptionByIP(filterType.ip)
                    }
                }

                is SubscriptionFilter.BY_CODE -> {
                    val code = filterType.code.trim()
                    if (code.isEmpty()) {
                        subscriptionsFlow.value = emptyList()
                        _uiState.update { it.copy(isSearching = false, searchPerformed = false) }
                        return
                    }
                    val subscriptionId = code.toIntOrNull()
                    if (subscriptionId == null) {
                        subscriptionsFlow.value = emptyList()
                        _uiState.update { it.copy(isSearching = false, searchPerformed = false) }
                        return
                    }
                    runCatching { repository.subscriptionById(subscriptionId).toDomain() }
                        .getOrNull()
                        ?.let { listOf(it) }
                        ?: emptyList()
                }

                else -> emptyList()
            }
            subscriptionsFlow.value = response
            _uiState.update { it.copy(isSearching = false, searchPerformed = true) }
        } catch (e: Exception) {
            observabilityClient.reportError(
                throwable = e,
                message = "Fallo en búsqueda de suscripciones",
                tags = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "action" to "find", "filterType" to filterType::class.simpleName)
            )
            _uiState.update {
                it.copy(
                    isSearching = false,
                    searchError = e.message ?: "No se pudo completar la búsqueda"
                )
            }
        }
    }

    // Función para recargar los datos con el último filtro usado
    fun reloadLastSearch() {
        viewModelScope.launch {
            _uiState.value.lastUsedFilter?.let { filter ->
                documentNumberFlow.emit(filter)
            }
        }
    }

    fun setSelectedSubscription(subscription: SubscriptionResume?) {
        _uiState.update { it.copy(selectedSubscription = subscription) }
    }

    fun cancelSubscription(subscriptionId: Int) {
        if (_uiState.value.cancelSubscriptionState == CancelSubscriptionState.Loading) return

        viewModelScope.launch {
            observabilityClient.addBreadcrumb(
                category = ObsBreadcrumbCategory.USER_ACTION,
                message = "$OBS_FEATURE.cancel",
                data = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "entityId" to subscriptionId)
            )
            try {
                _uiState.update { it.copy(cancelSubscriptionState = CancelSubscriptionState.Loading) }
            val responsibleId = repository.getUserSession()?.id
                ?: throw IllegalStateException("Usuario no encontrado")

            repository.cancelSubscription(subscriptionId, responsibleId)
            _uiState.update { it.copy(cancelSubscriptionState = CancelSubscriptionState.Success) }
        }catch (e: Exception) {
            observabilityClient.reportError(
                throwable = e,
                message = "Fallo al cancelar suscripción",
                tags = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "action" to "cancel", "entityId" to subscriptionId)
            )
            e.printStackTrace()
            _uiState.update { it.copy(cancelSubscriptionState = CancelSubscriptionState.Error) }
        }
    }
}

    fun removeSubscriptionFromList(id: Int) {
        subscriptionsFlow.value = subscriptionsFlow.value.filter { it.id != id }
        _uiState.update { it.copy(cancelSubscriptionState = CancelSubscriptionState.Empty) }
    }

    fun showReactivateDialog(show: Boolean) {
        _uiState.update { 
            it.copy(
                showReactivateDialog = show,
                reactivationNotes = if (!show) "" else it.reactivationNotes
            ) 
        }
    }

    fun updateReactivationNotes(notes: String) {
        _uiState.update { it.copy(reactivationNotes = notes) }
    }

    fun reactivateService(subscriptionId: Int) = viewModelScope.launch {
        _uiState.update { it.copy(reactivateServiceState = ReactivateServiceState.Loading) }
        
        val notes = _uiState.value.reactivationNotes.takeIf { it.isNotBlank() }
        reactivateServiceUseCase(subscriptionId, notes).fold(
            onSuccess = {
                _uiState.update { 
                    it.copy(
                        reactivateServiceState = ReactivateServiceState.Success,
                        showReactivateDialog = false,
                        reactivationNotes = ""
                    ) 
                }
                reloadLastSearch()
            },
            onFailure = { error ->
                observabilityClient.reportError(
                    throwable = error,
                    message = "Fallo al reactivar servicio",
                    tags = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "action" to "reactivate", "entityId" to subscriptionId)
                )
                error.printStackTrace()
                _uiState.update { it.copy(reactivateServiceState = ReactivateServiceState.Error(error.message)) }
            }
        )
    }

    fun clearReactivateServiceState() {
        _uiState.update { it.copy(reactivateServiceState = ReactivateServiceState.Empty) }
    }

    fun rebootFiberOnu(subscriptionId: Int) = viewModelScope.launch {
        _uiState.update { it.copy(rebootOnuState = RebootOnuState.Loading) }
        rebootFiberOnuUseCase(subscriptionId).fold(
            onSuccess = {
                _uiState.update { it.copy(rebootOnuState = RebootOnuState.Success) }
            },
            onFailure = { error ->
                observabilityClient.reportError(
                    throwable = error,
                    message = "Fallo al reiniciar ONU de fibra",
                    tags = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "action" to "reboot_onu", "entityId" to subscriptionId)
                )
                error.printStackTrace()
                _uiState.update {
                    it.copy(rebootOnuState = RebootOnuState.Error(error.message))
                }
            }
        )
    }

    fun clearRebootOnuState() {
        _uiState.update { it.copy(rebootOnuState = RebootOnuState.Empty) }
    }

    fun retryTr069(subscriptionId: Int) = viewModelScope.launch {
        _uiState.update { it.copy(retryTr069State = RetryTr069State.Loading) }
        retryTr069ProvisioningUseCase(subscriptionId).fold(
            onSuccess = { updated ->
                subscriptionsFlow.value = subscriptionsFlow.value.map { item ->
                    if (item.id == subscriptionId) {
                        item.copy(tr069ProvisionStatus = updated.tr069ProvisionStatus)
                    } else {
                        item
                    }
                }
                _uiState.update {
                    it.copy(retryTr069State = RetryTr069State.Success(retryTr069Feedback(updated)))
                }
            },
            onFailure = { error ->
                observabilityClient.reportError(
                    throwable = error,
                    message = "Fallo al reintentar aprovisionamiento TR-069",
                    tags = mapOf(
                        "feature" to OBS_FEATURE,
                        "screen" to OBS_SCREEN,
                        "action" to "retry_tr069",
                        "entityId" to subscriptionId
                    )
                )
                _uiState.update {
                    it.copy(retryTr069State = RetryTr069State.Error(error.message))
                }
            }
        )
    }

    fun clearRetryTr069State() {
        _uiState.update { it.copy(retryTr069State = RetryTr069State.Empty) }
    }

    private fun retryTr069Feedback(subscription: Subscription): String {
        return when (subscription.tr069ProvisionStatus) {
            "COMPLETE" -> "Aprovisionamiento TR-069 completado"
            else -> subscription.tr069Message?.takeIf { it.isNotBlank() }
                ?: "Reintento enviado. El aprovisionamiento TR-069 sigue en curso."
        }
    }

    fun getNapBoxes() = viewModelScope.launch {
        observabilityClient.addBreadcrumb(
            category = ObsBreadcrumbCategory.USER_ACTION,
            message = "$OBS_FEATURE.load_nap_boxes",
            data = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN)
        )
        try {
            _uiState.update { it.copy(napBoxesState = NapBoxesState.Loading) }
            val response = repository.getNapBoxes()
            _uiState.update { it.copy(napBoxesState = NapBoxesState.NapBoxListLoaded(response)) }
        } catch (e: Exception) {
            observabilityClient.reportError(
                throwable = e,
                message = "Fallo al cargar lista de cajas NAP",
                tags = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "action" to "load_nap_boxes")
            )
            _uiState.update { it.copy(napBoxesState = NapBoxesState.Error) }
            e.printStackTrace()
        }
    }

    fun changeNapBox(request: MoveOnuRequest) = viewModelScope.launch {
        observabilityClient.addBreadcrumb(
            category = ObsBreadcrumbCategory.USER_ACTION,
            message = "$OBS_FEATURE.change_nap_box",
            data = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "entityId" to request.subscriptionId)
        )
        try {
            _uiState.update { it.copy(napBoxesState = NapBoxesState.Loading) }
            repository.changeSubscriptionNapBox(request)
            _uiState.update { it.copy(napBoxesState = NapBoxesState.NapBoxChanged) }
        } catch (e: Exception) {
            observabilityClient.reportError(
                throwable = e,
                message = "Fallo al cambiar caja NAP",
                tags = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "action" to "change_nap_box", "entityId" to request.subscriptionId)
            )
            _uiState.update { it.copy(napBoxesState = NapBoxesState.Error) }
            e.printStackTrace()
        }
    }

    private fun getPlaces() = viewModelScope.launch {
        try {
            _uiState.update { it.copy(placesState = it.placesState.copy(isLoading = true)) }
            val places = repository.getPlaces()
            _uiState.update {
                it.copy(
                    placesState = it.placesState.copy(
                        places = places,
                        isLoading = false
                    )
                )
            }
        } catch (e: Exception) {
            observabilityClient.reportError(
                throwable = e,
                message = "Fallo al cargar lista de lugares",
                tags = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "action" to "load_places")
            )
            e.printStackTrace()
            _uiState.update {
                it.copy(
                    placesState = it.placesState.copy(
                        isLoading = false,
                        error = e.message
                    )
                )
            }
        }
    }

    fun onPlaceSelected(place: Place) {
        _uiState.update {
            it.copy(
                placesState = it.placesState.copy(selectedPlace = place)
            )
        }
    }

    fun initCustomerFormData(subscription: SubscriptionResume) {
        val customer = subscription.customer.apply {
            name = name.removeAccents()
            lastName = lastName.removeAccents()
            address = address.removeAccents()
        }

        // First, find and select the current place in placesState
        val currentPlace = _uiState.value.placesState.places.find { place ->
            place.id?.toIntOrNull() == subscription.placeId.toInt()
        }

        // Update place selection first
        if (currentPlace != null) {
            onPlaceSelected(currentPlace)
        }

        // Validate the initial data
        val nameError = validateCustomerFormField("name", customer.name)
        val lastNameError = validateCustomerFormField("lastName", customer.lastName)
        val phoneError = validateCustomerFormField("phone", customer.phone)
        val dniError = validateCustomerFormField("dni", customer.dni)
        val addressError = validateCustomerFormField("address", customer.address)
        val emailError = validateCustomerFormField("email", customer.email)

        // Then update the form data
        _uiState.update { currentState ->
            currentState.copy(
                customerFormData = CustomerFormData(
                    name = customer.name,
                    nameError = nameError,
                    lastName = customer.lastName,
                    lastNameError = lastNameError,
                    phone = customer.phone,
                    phoneError = phoneError,
                    dni = customer.dni,
                    dniError = dniError,
                    address = customer.address,
                    addressError = addressError,
                    email = customer.email,
                    emailError = emailError,
                    place = customer.place,
                    placeId = subscription.placeId.toInt(),
                    subscriptionId = subscription.id,
                )
            )
        }
        subscription.customerId?.let { customerId ->
            viewModelScope.launch {
                val count = runCatching { subscriptionActionsRepository?.getCustomer(customerId) }
                    .getOrNull()?.subscriptions?.size ?: return@launch
                _uiState.update { current ->
                    val form = current.customerFormData ?: return@update current
                    current.copy(customerFormData = form.copy(sharedServiceCount = count))
                }
            }
        }
    }

    fun validateCustomerFormField(field: String, value: String): String? {
        return when (field) {
            "name" -> if (!value.isAValidName()) "Nombre inválido" else null
            "lastName" -> if (!value.isAValidName()) "Apellido inválido" else null
            "phone" -> if (!value.isValidPhone()) "Teléfono requiere 9 dígitos" else null
            "dni" -> if (!value.isValidDni()) "DNI requiere 8 dígitos" else null
            "address" -> if (!value.isAValidAddress()) "Dirección inválida" else null
            "email" -> if (value.isNotEmpty() && !value.isValidEmail()) "Email inválido" else null
            else -> null
        }
    }

    fun updateCustomerFormField(field: String, value: String) {
        _uiState.value.customerFormData?.let { formData ->
            val normalizedValue = when (field) {
                "name", "lastName" -> value.uppercase()
                else -> value
            }

            val errorMessage = validateCustomerFormField(field, normalizedValue)

            val updatedFormData = when (field) {
                "name" -> formData.copy(name = normalizedValue, nameError = errorMessage)
                "lastName" -> formData.copy(lastName = normalizedValue, lastNameError = errorMessage)
                "phone" -> formData.copy(phone = normalizedValue, phoneError = errorMessage)
                "dni" -> formData.copy(dni = normalizedValue, dniError = errorMessage)
                "address" -> formData.copy(address = normalizedValue, addressError = errorMessage)
                "email" -> formData.copy(email = normalizedValue, emailError = errorMessage)
                "place" -> formData.copy(place = normalizedValue)
                else -> formData
            }
            _uiState.update { it.copy(customerFormData = updatedFormData) }
        }
    }

    fun updateCustomerPlaceId(placeId: Int, placeName: String) {
        _uiState.value.customerFormData?.let { formData ->
            val updatedFormData = formData.copy(
                placeId = placeId,
                place = placeName
            )
            _uiState.update { it.copy(customerFormData = updatedFormData) }
        }
    }

    fun saveCustomerData() = viewModelScope.launch {
        val subscriptionId = _uiState.value.customerFormData?.subscriptionId
        observabilityClient.addBreadcrumb(
            category = ObsBreadcrumbCategory.USER_ACTION,
            message = "$OBS_FEATURE.save_customer",
            data = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "entityId" to subscriptionId)
        )
        try {
            val formData = _uiState.value.customerFormData ?: return@launch

            // Validate all fields before saving
            val nameError = validateCustomerFormField("name", formData.name)
            val lastNameError = validateCustomerFormField("lastName", formData.lastName)
            val phoneError = validateCustomerFormField("phone", formData.phone)
            val dniError = validateCustomerFormField("dni", formData.dni)
            val addressError = validateCustomerFormField("address", formData.address)
            val emailError = validateCustomerFormField("email", formData.email)

            // Update form data with any validation errors
            val updatedFormData = formData.copy(
                nameError = nameError,
                lastNameError = lastNameError,
                phoneError = phoneError,
                dniError = dniError,
                addressError = addressError,
                emailError = emailError
            )

            _uiState.update { it.copy(customerFormData = updatedFormData) }

            // Only proceed if all validations pass
            if (!updatedFormData.isValid()) {
                return@launch
            }

            _uiState.update { it.copy(saveSubscriptionState = SaveSubscriptionState.Loading) }

            val customerData = CustomerData(
                subscriptionId = formData.subscriptionId,
                name = formData.name,
                lastName = formData.lastName,
                phone = formData.phone,
                dni = formData.dni,
                address = formData.address,
                email = formData.email,
                place = formData.place,
                placeId = formData.placeId
            )

            repository.updateCustomerData(customerData)
            _uiState.update {

                subscriptionsFlow.value = subscriptionsFlow.value.map { subscription ->
                    if (subscription.id == formData.subscriptionId) {
                        subscription.copy(
                            customer = customerData,
                            placeId = formData.placeId.toString()
                        )
                    } else {
                        subscription
                    }
                }

                it.copy(saveSubscriptionState = SaveSubscriptionState.Success)
            }

            // Informa a la pantalla solamente cuando los cambios fueron guardados correctamente.
            _customerSaveMessages.emit("Datos del cliente actualizados correctamente")
        } catch (e: Exception) {
            observabilityClient.reportError(
                throwable = e,
                message = "Fallo al guardar datos del cliente",
                tags = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "action" to "save_customer", "entityId" to subscriptionId)
            )
            e.printStackTrace()
            _uiState.update { it.copy(saveSubscriptionState = SaveSubscriptionState.Error) }

            // Informa a la pantalla cuando el backend no pudo guardar los cambios.
            _customerSaveMessages.emit("No se pudieron actualizar los datos del cliente")
        }
    }

    fun toggleLocationUpdateDialog(show: Boolean) {
        _uiState.update {
            it.copy(
                showLocationUpdateDialog = show,
                editableLatitude = if (show) it.selectedSubscription?.location?.latitude?.toString() ?: "" else "",
                editableLongitude = if (show) it.selectedSubscription?.location?.longitude?.toString() ?: "" else "",
                isFetchingCurrentLocation = false,
                saveSubscriptionState = if (!show) SaveSubscriptionState.Success else it.saveSubscriptionState
            )
        }
    }

    fun updateCoordinatesFromMap(latLng: LatLng) {
        _uiState.update { 
            it.copy(
                editableLatitude = latLng.latitude.toString(),
                editableLongitude = latLng.longitude.toString(),
                isFetchingCurrentLocation = false
            )
        }
    }

    /**
     * Updates coordinates from current location received from location client
     */
    fun updateCurrentLocation(latLng: LatLng) {
        updateCoordinatesFromMap(latLng)
    }

    /**
     * Updates the loading state for fetching current location
     */
    fun setFetchingCurrentLocation(isFetching: Boolean) {
        _uiState.update { it.copy(isFetchingCurrentLocation = isFetching) }
    }

    /**
     * Called if location retrieval fails
     */
    fun onLocationError() {
        _uiState.update { it.copy(isFetchingCurrentLocation = false) }
    }

    fun updateSubscriptionLocation() = viewModelScope.launch {
        val currentState = _uiState.value
        val latitudeStr = currentState.editableLatitude
        val longitudeStr = currentState.editableLongitude

        currentState.selectedSubscription?.let { subscription ->
            observabilityClient.addBreadcrumb(
                category = ObsBreadcrumbCategory.USER_ACTION,
                message = "$OBS_FEATURE.update_location",
                data = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "entityId" to subscription.id)
            )
            try {
                val latitude = latitudeStr.toDouble()
                val longitude = longitudeStr.toDouble()

                _uiState.update { it.copy(saveSubscriptionState = SaveSubscriptionState.Loading) }
                
                repository.updateSubscriptionLocation(subscription.id, latitude, longitude)
                
                val updatedLocation = subscription.location.copy(latitude = latitude, longitude = longitude)
                val updatedSubscription = subscription.copy(location = updatedLocation)
                
                val updatedList = subscriptionsFlow.value.map { sub ->
                    if (sub.id == subscription.id) updatedSubscription else sub
                }
                subscriptionsFlow.value = updatedList
                
                _uiState.update { it.copy(
                    saveSubscriptionState = SaveSubscriptionState.Success,
                    showLocationUpdateDialog = false,
                    selectedSubscription = updatedSubscription
                ) }
            } catch (e: Exception) {
                observabilityClient.reportError(
                    throwable = e,
                    message = "Fallo al actualizar ubicación de suscripción",
                    tags = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "action" to "update_location", "entityId" to subscription.id)
                )
                e.printStackTrace()
                _uiState.update { it.copy(saveSubscriptionState = SaveSubscriptionState.Error) }
            }
        }
    }
}

sealed class SaveSubscriptionState {
    object Loading : SaveSubscriptionState()
    object Success : SaveSubscriptionState()
    object Error : SaveSubscriptionState()
}

sealed class CancelSubscriptionState {
    object Empty : CancelSubscriptionState()
    object Loading : CancelSubscriptionState()
    object Success : CancelSubscriptionState()
    object Error : CancelSubscriptionState()
}

sealed class ReactivateServiceState {
    object Empty : ReactivateServiceState()
    object Loading : ReactivateServiceState()
    object Success : ReactivateServiceState()
    data class Error(val error:String?) : ReactivateServiceState()
}

sealed class RebootOnuState {
    object Empty : RebootOnuState()
    object Loading : RebootOnuState()
    object Success : RebootOnuState()
    data class Error(val message: String?) : RebootOnuState()
}

sealed class RetryTr069State {
    object Empty : RetryTr069State()
    object Loading : RetryTr069State()
    data class Success(val message: String) : RetryTr069State()
    data class Error(val message: String?) : RetryTr069State()
}

sealed class NapBoxesState {
    object Loading : NapBoxesState()
    data class NapBoxListLoaded(val items: List<NapBoxResponse>) : NapBoxesState()
    object NapBoxChanged : NapBoxesState()
    object Error : NapBoxesState()
}

data class PlacesState(
    val places: List<Place> = emptyList(),
    val selectedPlace: Place? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
