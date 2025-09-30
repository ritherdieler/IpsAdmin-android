package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.apirequestmodel.MoveOnuRequest
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.CustomerData
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionResume
import com.dscorp.ispadmin.domain.model.extensions.isAValidAddress
import com.dscorp.ispadmin.domain.model.extensions.isAValidName
import com.dscorp.ispadmin.domain.model.extensions.isValidDni
import com.dscorp.ispadmin.domain.model.extensions.isValidEmail
import com.dscorp.ispadmin.domain.model.extensions.isValidPhone
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

const val REQUEST_DELAY = 500L

data class SubscriptionFinderUiState(
    val subscriptions: Map<ServiceStatus, List<SubscriptionResume>> = emptyMap(),
    val cancelSubscriptionState: CancelSubscriptionState = CancelSubscriptionState.Empty,
    val saveSubscriptionState: SaveSubscriptionState = SaveSubscriptionState.Success,
    val napBoxesState: NapBoxesState = NapBoxesState.Loading,
    val placesState: PlacesState = PlacesState(),
    val selectedSubscription: SubscriptionResume? = null,
    val customerFormData: CustomerFormData? = null,
    val showLocationUpdateDialog: Boolean = false,
    val editableLatitude: String = "",
    val editableLongitude: String = "",
    val isFetchingCurrentLocation: Boolean = false,
    val lastUsedFilter: SubscriptionFilter? = null
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
    private val repository: IRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionFinderUiState())
    val uiState: StateFlow<SubscriptionFinderUiState> = _uiState.asStateFlow()

    private val subscriptionsFlow = MutableStateFlow<List<SubscriptionResume>>(emptyList())

    val documentNumberFlow = MutableSharedFlow<SubscriptionFilter>(extraBufferCapacity = 1)

    init {
        findSubscription()
        getPlaces()

        // Observe subscriptionsFlow and update the UI state
        viewModelScope.launch {
            subscriptionsFlow.map { list ->
                list.map {
                    if (it.serviceStatus != ServiceStatus.CANCELLED) it.copy(serviceStatus = ServiceStatus.ACTIVE)
                    else it.copy(serviceStatus = ServiceStatus.CANCELLED)
                }.groupBy { it.serviceStatus }
            }.collect { groupedSubscriptions ->
                _uiState.update { it.copy(subscriptions = groupedSubscriptions) }
            }
        }
    }

    fun resetNapBoxFlow() {
        _uiState.update { it.copy(napBoxesState = NapBoxesState.Loading) }
    }

    @OptIn(FlowPreview::class)
    fun findSubscription() = viewModelScope.launch {
        documentNumberFlow.debounce(REQUEST_DELAY)
            .collect { filterType ->
                // Guardar el último filtro usado
                _uiState.update { it.copy(lastUsedFilter = filterType) }
                
                val response = when (filterType) {
                    is SubscriptionFilter.BY_DATE -> {
                        if (filterType.startDate.isEmpty() || filterType.endDate.isEmpty()) {
                            subscriptionsFlow.value = emptyList()
                            return@collect
                        }
                        repository.findSubscriptionBySubscriptionDate(
                            filterType.startDate,
                            filterType.endDate
                        )
                    }

                    is SubscriptionFilter.BY_DOCUMENT -> {
                        if (filterType.documentNumber.isEmpty()) {
                            subscriptionsFlow.value = emptyList()
                            return@collect
                        } else {
                            repository.findSubscriptionByDNI(filterType.documentNumber)
                        }
                    }

                    is SubscriptionFilter.BY_NAME -> {
                        if (filterType.name.isEmpty() && filterType.lastName.isEmpty()) {
                            subscriptionsFlow.value = emptyList()
                            return@collect
                        } else {
                            repository.findSubscriptionByNameAndLastName(
                                filterType.name,
                                filterType.lastName
                            )
                        }
                    }

                    is SubscriptionFilter.BY_IP -> {
                        if (filterType.ip.isEmpty()) {
                            subscriptionsFlow.value = emptyList()
                            return@collect
                        } else {
                            repository.findSubscriptionByIP(filterType.ip)
                        }
                    }
                }
                subscriptionsFlow.value = response
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

    fun cancelSubscription(subscriptionId: Int) = viewModelScope.launch {
        try {
            _uiState.update { it.copy(cancelSubscriptionState = CancelSubscriptionState.Loading) }
            repository.cancelSubscription(subscriptionId)
            _uiState.update { it.copy(cancelSubscriptionState = CancelSubscriptionState.Success) }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { it.copy(cancelSubscriptionState = CancelSubscriptionState.Error) }
        }
    }

    fun removeSubscriptionFromList(id: Int) {
        subscriptionsFlow.value = subscriptionsFlow.value.filter { it.id != id }
        _uiState.update { it.copy(cancelSubscriptionState = CancelSubscriptionState.Empty) }
    }

    fun getNapBoxes() = viewModelScope.launch {
        try {
            _uiState.update { it.copy(napBoxesState = NapBoxesState.Loading) }
            val response = repository.getNapBoxes()
            _uiState.update { it.copy(napBoxesState = NapBoxesState.NapBoxListLoaded(response)) }
        } catch (e: Exception) {
            _uiState.update { it.copy(napBoxesState = NapBoxesState.Error) }
            e.printStackTrace()
        }
    }

    fun changeNapBox(request: MoveOnuRequest) = viewModelScope.launch {
        try {
            _uiState.update { it.copy(napBoxesState = NapBoxesState.Loading) }
            repository.changeSubscriptionNapBox(request)
            _uiState.update { it.copy(napBoxesState = NapBoxesState.NapBoxChanged) }
        } catch (e: Exception) {
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
            val errorMessage = validateCustomerFormField(field, value)

            val updatedFormData = when (field) {
                "name" -> formData.copy(name = value, nameError = errorMessage)
                "lastName" -> formData.copy(lastName = value, lastNameError = errorMessage)
                "phone" -> formData.copy(phone = value, phoneError = errorMessage)
                "dni" -> formData.copy(dni = value, dniError = errorMessage)
                "address" -> formData.copy(address = value, addressError = errorMessage)
                "email" -> formData.copy(email = value, emailError = errorMessage)
                "place" -> formData.copy(place = value)
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
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { it.copy(saveSubscriptionState = SaveSubscriptionState.Error) }
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