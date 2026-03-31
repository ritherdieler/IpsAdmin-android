package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.domain.model.EquipmentCondition
import com.dscorp.ispadmin.domain.model.GeoLocation
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.domain.usecase.InstallationOrderUseCase
import com.dscorp.ispadmin.presentation.extension.removeSpecialCharacters
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.FormFieldKey
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionFormState
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionState
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterSubscriptionComposeViewModel(
    private val getAvailableOnuListUseCase: GetAvailableOnuListUseCase,
    private val getPlanListUseCase: GetPlanListUseCase,
    private val getPlaceListUseCase: GetPlaceListUseCase,
    private val getPlaceFromLocationUseCase: GetPlaceFromLocationUseCase,
    private val getNapBoxListUseCase: GetNapBoxListUseCase,
    private val registerSubscriptionUseCase: RegisterSubscriptionUseCase,
    private val getUserSessionUseCase: GetUserSessionUseCase,
    private val getCoreDevicesUseCase: GetCoreDevicesUseCase,
    private val getNearNapBoxesUseCase: GetNearNapBoxesUseCase,
    private val installationOrderUseCase: InstallationOrderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterSubscriptionState())
    val uiState: StateFlow<RegisterSubscriptionState> = _uiState.asStateFlow()

    fun loadInitialFormData() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }

        try {
            val onuListDeferred = async { getAvailableOnuListUseCase() }
            val planListDeferred = async { getPlanListUseCase() }
            val placeListDeferred = async { getPlaceListUseCase() }
            val napBoxListDeferred = async { getNapBoxListUseCase() }
            val userSessionDeferred = async { getUserSessionUseCase() }
            val coreDevicesDeferred = async { getCoreDevicesUseCase() }

            val onuList = onuListDeferred.await()
            val planList = planListDeferred.await()
            val placeList = placeListDeferred.await()
            val napBoxList = napBoxListDeferred.await()
            val userSession = userSessionDeferred.await()
            val coreDevices = coreDevicesDeferred.await()

            val user = userSession.getOrThrow()
            val cachedNapBoxes = napBoxList.getOrNull() ?: emptyList()
            val cachedPlans = planList.getOrNull() ?: emptyList()

            // Filtrar planes por tipo de instalación actual (FIBER por defecto)
            val filteredPlans = cachedPlans.filter { it.type == InstallationType.FIBER }

            // Seleccionar automáticamente el plan si solo hay uno disponible
            val selectedPlan = getAutoSelectedPlan(filteredPlans, null)

            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    currentUser = user,
                    cachedNapBoxList = cachedNapBoxes,
                    cachedPlanList = cachedPlans,
                    registerSubscriptionForm = current.registerSubscriptionForm.copy(
                        onuList = onuList.getOrNull() ?: emptyList(),
                        planList = filteredPlans,
                        placeList = placeList.getOrNull() ?: emptyList(),
                        napBoxList = cachedNapBoxes,
                        selectedHostDevice = coreDevices.getOrThrow().firstOrNull(),
                        selectedPlan = selectedPlan
                    )
                )
            }
        } catch (e: Exception) {
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun refreshOnuList() = viewModelScope.launch {
        _uiState.update { it.copy(isRefreshingOnuList = true) }

        try {
            val onuList = getAvailableOnuListUseCase()

            _uiState.update { current ->
                val currentForm = current.registerSubscriptionForm
                val refreshedOnuList = onuList.getOrNull() ?: emptyList()
                val selectedOnu = currentForm.selectedOnu?.takeIf { selected ->
                    refreshedOnuList.any { it.sn == selected.sn }
                }

                current.copy(
                    isRefreshingOnuList = false,
                    registerSubscriptionForm = currentForm.copy(
                        onuList = refreshedOnuList,
                        selectedOnu = selectedOnu,
                        onuError = when {
                            currentForm.selectedOnu != null && selectedOnu == null ->
                                "La ONU seleccionada ya no está disponible"
                            currentForm.onuError != null -> currentForm.copy(
                                onuList = refreshedOnuList,
                                selectedOnu = selectedOnu
                            ).validate(FormFieldKey.ONU)
                            else -> null
                        }
                    )
                )
            }
        } catch (e: Exception) {
            _uiState.update { current ->
                current.copy(
                    isRefreshingOnuList = false,
                    error = e.message ?: "Error al actualizar la lista de ONUs"
                )
            }
        }
    }

    fun onFirstNameChanged(value: String) {
        if (value.length > 50) return

        updateValidatedForm(FormFieldKey.FIRST_NAME) { form ->
            form.copy(firstName = value)
        }
    }

    fun onLastNameChanged(value: String) {
        if (value.length > 50) return

        updateValidatedForm(FormFieldKey.LAST_NAME) { form ->
            form.copy(lastName = value)
        }
    }

    fun onDniChanged(value: String) {
        if (value.length > 8) return

        updateValidatedForm(FormFieldKey.DNI) { form ->
            form.copy(dni = value)
        }
    }

    fun onAddressChanged(value: String) {
        updateValidatedForm(FormFieldKey.ADDRESS) { form ->
            form.copy(address = value)
        }
    }

    fun onPhoneChanged(value: String) {
        if (value.length > 9) return

        updateValidatedForm(FormFieldKey.PHONE) { form ->
            form.copy(phone = value)
        }
    }

    fun onPlanSelected(value: PlanResponse) {
        updateValidatedForm(FormFieldKey.PLAN) { form ->
            form.copy(selectedPlan = value)
        }
    }

    fun onPlaceSelected(value: Place) {
        val filteredNapBoxes = getFilteredNapBoxesForPlace(value.id)

        updateValidatedForm(FormFieldKey.PLACE, FormFieldKey.NAP_BOX) { form ->
            form.copy(
                selectedPlace = value,
                napBoxList = filteredNapBoxes,
                selectedNapBox = form.selectedNapBox?.takeIf { selected ->
                    filteredNapBoxes.any { it.id == selected.id }
                }
            )
        }
    }

    fun onOnuSelected(value: Onu) {
        updateValidatedForm(FormFieldKey.ONU) { form ->
            form.copy(selectedOnu = value)
        }
    }

    fun onNapBoxSelected(value: NapBoxResponse) {
        updateValidatedForm(FormFieldKey.NAP_BOX) { form ->
            form.copy(selectedNapBox = value)
        }
    }

    fun onNoteChanged(value: String) {
        if (value.length > 180) return
        
        _uiState.update { current ->
            current.copy(
                registerSubscriptionForm = current.registerSubscriptionForm.copy(
                    note = value
                )
            )
        }
    }

    fun onEquipmentConditionChanged(value: EquipmentCondition) {
        _uiState.update { current ->
            current.copy(
                registerSubscriptionForm = current.registerSubscriptionForm.copy(
                    equipmentCondition = value
                )
            )
        }
    }

    fun onPlaceSelectionCleared() {
        updateValidatedForm(FormFieldKey.PLACE, FormFieldKey.NAP_BOX) { form ->
            form.copy(
                selectedPlace = null,
                selectedNapBox = null,
                napBoxList = getFilteredNapBoxesForPlace(null)
            )
        }
    }

    fun onNapBoxSelectionCleared() {
        updateValidatedForm(FormFieldKey.NAP_BOX) { form ->
            form.copy(selectedNapBox = null)
        }
    }

    fun onInstallationTypeSelected(type: InstallationType) {
        val filteredPlans = getFilteredPlansForInstallationType(type)

        // Si no hay planes disponibles para este tipo, no hacer nada
        if (filteredPlans.isEmpty()) return

        val currentSelectedPlan = currentUiState().registerSubscriptionForm.selectedPlan
        val selectedPlan = getAutoSelectedPlan(filteredPlans, currentSelectedPlan)

        _uiState.update {
            it.copy(
                registerSubscriptionForm = it.registerSubscriptionForm.copy(
                    installationType = type,
                    planList = filteredPlans,
                    selectedPlan = selectedPlan,
                    selectedOnu = null,
                    selectedNapBox = null,
                ).validated(FormFieldKey.PLAN, FormFieldKey.ONU, FormFieldKey.NAP_BOX)
            )
        }
    }

    fun getPlaceFromCurrentLocation(latitude: Double, longitude: Double) = viewModelScope.launch {
        getPlaceFromLocationUseCase(latitude, longitude).fold(
            onSuccess = { place ->
                onPlaceSelected(place)
            },
            onFailure = { error ->

            }
        )
    }

    fun saveSubscription() {
        val form = uiState.value.registerSubscriptionForm
        val validatedForm = form.validated()

        if (!validatedForm.isValid()) {
            _uiState.update {
                it.copy(registerSubscriptionForm = validatedForm)
            }
            return
        }

        _uiState.update {
            it.copy(isLoading = true)
        }

        val subscription = createSubscriptionFromForm(validatedForm)

        viewModelScope.launch {
            registerSubscriptionUseCase(subscription, uiState.value.orderId).fold(
                onSuccess = { registeredSubscription ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            registeredSubscription = registeredSubscription,
                            error = null,
                            orderId = null
                        )
                    }

                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Error al registrar la suscripción"
                        )
                    }
                }
            )
        }
    }

    private fun createSubscriptionFromForm(
        form: RegisterSubscriptionFormState
    ): Subscription {
        val user = currentUiState().currentUser
            ?: throw IllegalStateException("Usuario no disponible para crear suscripción")
        
        return Subscription(
            firstName = form.firstName.removeSpecialCharacters(),
            lastName = form.lastName.removeSpecialCharacters(),
            dni = form.dni,
            address = form.address,
            phone = form.phone,
            subscriptionDate = form.subscriptionDate,
            planId = form.selectedPlan!!.id,
            placeId = form.selectedPlace!!.id,
            technicianId = user.id,
            hostDeviceId = form.selectedHostDevice?.id,
            location = GeoLocation(
                form.location?.latitude ?: 0.0,
                form.location?.longitude ?: 0.0
            ),
            installationType = form.installationType,
            note = form.note,
            napBoxId = form.selectedNapBox?.id,
            onu = form.selectedOnu,
            equipmentCondition = form.equipmentCondition
        )
    }

    fun clearRegisteredSubscription() {
        _uiState.update {
            it.copy(
                registeredSubscription = null,
                error = null
            )
        }
    }

    fun clearError() {
        _uiState.update {
            it.copy(
                error = null
            )
        }
    }

    fun onLocationChanged(currentLocation: LatLng) {
        _uiState.update {
            it.copy(
                registerSubscriptionForm = it.registerSubscriptionForm.copy(
                    location = currentLocation
                )
            )
        }
    }
    
    /**
     * Actualiza el estado de GPS
     */
    fun onGpsStateChanged(isEnabled: Boolean) {
        _uiState.update { it.copy(isGpsEnabled = isEnabled) }
        
        if (!isEnabled) {
            _uiState.update { it.copy(shouldShowGpsDialog = true) }
        }
    }
    
    /**
     * Actualiza el estado de permisos de ubicación
     */
    fun onLocationPermissionChanged(hasPermission: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = hasPermission) }
    }
    
    /**
     * Cierra el diálogo de GPS
     */
    fun dismissGpsDialog() {
        _uiState.update { it.copy(shouldShowGpsDialog = false) }
    }
    
    /**
     * Procesa la ubicación actual cuando se obtiene del servicio de ubicación
     */
    fun processCurrentLocation(latitude: Double, longitude: Double) {
        onLocationChanged(LatLng(latitude, longitude))
        getPlaceFromCurrentLocation(latitude, longitude)
        getNearbyNapBoxes(latitude, longitude)
    }

    fun getNearbyNapBoxes(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingNearbyNapBoxes = true) }

            getNearNapBoxesUseCase(latitude, longitude).fold(
                onSuccess = { napBoxes ->
                    val currentForm = currentUiState().registerSubscriptionForm
                    val selectedPlace = currentForm.selectedPlace
                    val filteredNapBoxes = selectedPlace?.let { place ->
                        napBoxes.filter { it.placeId == place.id?.toInt() }
                    } ?: napBoxes
                    val selectedNapBox = currentForm.selectedNapBox?.takeIf { selected ->
                        filteredNapBoxes.any { it.id == selected.id }
                    }

                    _uiState.update {
                        it.copy(
                            isLoadingNearbyNapBoxes = false,
                            cachedNapBoxList = napBoxes,
                            registerSubscriptionForm = it.registerSubscriptionForm.copy(
                                napBoxList = filteredNapBoxes,
                                selectedNapBox = selectedNapBox,
                                napBoxError = when {
                                    currentForm.selectedNapBox != null && selectedNapBox == null ->
                                        "La caja NAP seleccionada ya no está disponible"
                                    currentForm.napBoxError != null -> it.registerSubscriptionForm.copy(
                                        napBoxList = filteredNapBoxes,
                                        selectedNapBox = selectedNapBox
                                    ).validate(FormFieldKey.NAP_BOX)
                                    else -> null
                                }
                            )
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingNearbyNapBoxes = false,
                            error = error.message ?: "Error al obtener cajas NAP cercanas"
                        )
                    }
                }
            )
        }
    }

    /**
     * Carga los datos de una orden de instalación para prellenar el formulario
     */
    fun loadInstallationOrderData(orderId: Int) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, orderId = orderId) }

        try {
            val order = installationOrderUseCase.getInstallationOrderById(orderId)
            val selectedPlace = order.place

            val currentInstallationType = currentUiState().registerSubscriptionForm.installationType
            val filteredPlans = getFilteredPlansForInstallationType(currentInstallationType)
            val selectedPlan = getAutoSelectedPlan(filteredPlans, null)
            val filteredNapBoxes = getFilteredNapBoxesForPlace(selectedPlace?.id)

            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    registerSubscriptionForm = current.registerSubscriptionForm.copy(
                        firstName = order.customerFirstName,
                        lastName = order.customerLastName,
                        address = order.customerAddress,
                        phone = order.customerPhone,
                        dni = order.customerDni,
                        selectedPlace = selectedPlace,
                        selectedPlan = selectedPlan,
                        selectedNapBox = null,
                        napBoxList = filteredNapBoxes
                    )
                )
            }
        } catch (e: Exception) {
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    error = e.message ?: "Error al cargar los datos de la orden"
                )
            }
        }
    }

    /**
     * Cierra una orden de instalación después de registrar la suscripción
     */
    fun closeInstallationOrder(orderId: Int) = viewModelScope.launch {
        try {
            installationOrderUseCase.closeInstallationOrder(orderId)
        } catch (e: Exception) {
            _uiState.update { current ->
                current.copy(
                    error = e.message ?: "Error al cerrar la orden de instalación"
                )
            }
        }
    }

    private fun currentUiState() = _uiState.value

    private fun updateValidatedForm(
        vararg fields: FormFieldKey,
        transform: (RegisterSubscriptionFormState) -> RegisterSubscriptionFormState
    ) {
        _uiState.update { current ->
            current.copy(
                registerSubscriptionForm = transform(current.registerSubscriptionForm).validated(*fields)
            )
        }
    }
    
    /**
     * Filtra planes según el tipo de instalación
     */
    private fun getFilteredPlansForInstallationType(type: InstallationType): List<PlanResponse> {
        return currentUiState().cachedPlanList.filter { it.type == type }
    }
    
    /**
     * Determina qué plan debe estar seleccionado automáticamente:
     * 1. Si el plan actual es compatible con los planes filtrados, lo mantiene
     * 2. Si solo hay un plan disponible, lo selecciona automáticamente
     * 3. En otro caso, retorna null
     */
    private fun getAutoSelectedPlan(
        filteredPlans: List<PlanResponse>,
        currentSelectedPlan: PlanResponse?
    ): PlanResponse? {
        return when {
            currentSelectedPlan != null && filteredPlans.any { it.id == currentSelectedPlan.id } -> currentSelectedPlan
            filteredPlans.size == 1 -> filteredPlans.first()
            else -> null
        }
    }
    
    /**
     * Filtra las cajas NAP por el lugar seleccionado
     */
    private fun getFilteredNapBoxesForPlace(placeId: String?): List<NapBoxResponse> {
        if (placeId == null) return currentUiState().cachedNapBoxList
        val placeIdInt = placeId.toIntOrNull() ?: return currentUiState().cachedNapBoxList
        return currentUiState().cachedNapBoxList.filter { it.placeId == placeIdInt }
    }
}
