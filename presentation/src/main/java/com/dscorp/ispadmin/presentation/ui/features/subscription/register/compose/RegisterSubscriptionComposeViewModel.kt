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
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.model.extensions.isAValidAddress
import com.dscorp.ispadmin.domain.model.extensions.isAValidName
import com.dscorp.ispadmin.domain.model.extensions.isValidDni
import com.dscorp.ispadmin.domain.model.extensions.isValidPhone
import com.dscorp.ispadmin.domain.usecase.InstallationOrderUseCase
import com.dscorp.ispadmin.presentation.extension.removeSpecialCharacters
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.FormFieldKey
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionFormState
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionState
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
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

    val uiState = MutableStateFlow(RegisterSubscriptionState())

    private var cachedNapBoxList: List<NapBoxResponse> = emptyList()
    private var cachedPlanList: List<PlanResponse> = emptyList()
    private var currentUser: User? = null

    fun loadInitialFormData() = viewModelScope.launch {
        uiState.update { it.copy(isLoading = true) }

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

            currentUser = userSession.getOrThrow()
            cachedNapBoxList = napBoxList.getOrNull() ?: emptyList()
            cachedPlanList = planList.getOrNull() ?: emptyList()

            // Filtrar planes por tipo de instalación actual (FIBER por defecto)
            val filteredPlans = cachedPlanList.filter { it.type == InstallationType.FIBER }

            // Seleccionar automáticamente el plan si solo hay uno disponible
            val selectedPlan = if (filteredPlans.size == 1) filteredPlans.first() else null

            uiState.update { current ->
                current.copy(
                    isLoading = false,
                    registerSubscriptionForm = current.registerSubscriptionForm.copy(
                        onuList = onuList.getOrNull() ?: emptyList(),
                        planList = filteredPlans,
                        placeList = placeList.getOrNull() ?: emptyList(),
                        napBoxList = cachedNapBoxList,
                        selectedHostDevice = coreDevices.getOrThrow().firstOrNull(),
                        selectedPlan = selectedPlan
                    )
                )
            }
        } catch (e: Exception) {
            uiState.update { current ->
                current.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun refreshOnuList() = viewModelScope.launch {
        uiState.update { it.copy(isRefreshingOnuList = true) }

        try {
            val onuList = getAvailableOnuListUseCase()

            uiState.update { current ->
                current.copy(
                    isRefreshingOnuList = false,
                    registerSubscriptionForm = current.registerSubscriptionForm.copy(
                        onuList = onuList.getOrNull() ?: emptyList()
                    )
                )
            }
        } catch (e: Exception) {
            uiState.update { current ->
                current.copy(
                    isRefreshingOnuList = false,
                    error = e.message ?: "Error al actualizar la lista de ONUs"
                )
            }
        }
    }

    fun <T> updateField(
        fieldKey: FormFieldKey,
        value: T,
        isValid: (T) -> Boolean,
        errorMessage: String? = null
    ) {
        uiState.update { currentState ->
            val form = currentState.registerSubscriptionForm
            val updatedForm = when (fieldKey) {
                FormFieldKey.FIRST_NAME -> form.copy(
                    firstName = value as String,
                    firstNameError = if (isValid(value)) null else errorMessage
                )

                FormFieldKey.LAST_NAME -> form.copy(
                    lastName = value as String,
                    lastNameError = if (isValid(value)) null else errorMessage
                )

                FormFieldKey.DNI -> form.copy(
                    dni = value as String,
                    dniError = if (isValid(value)) null else errorMessage
                )

                FormFieldKey.ADDRESS -> form.copy(
                    address = value as String,
                    addressError = if (isValid(value)) null else errorMessage
                )

                FormFieldKey.PHONE -> form.copy(
                    phone = value as String,
                    phoneError = if (isValid(value)) null else errorMessage
                )

                FormFieldKey.PLAN -> form.copy(
                    selectedPlan = value as PlanResponse,
                    planError = if (isValid(value)) null else errorMessage
                )

                FormFieldKey.PLACE -> form.copy(
                    selectedPlace = value as Place,
                    placeError = if (isValid(value)) null else errorMessage,
                    napBoxList = cachedNapBoxList.filter { it.placeId == value.id?.toInt() }
                )

                FormFieldKey.ONU -> form.copy(
                    selectedOnu = value as Onu,
                    onuError = if (isValid(value)) null else errorMessage
                )

                FormFieldKey.NAP_BOX -> form.copy(
                    selectedNapBox = value as NapBoxResponse,
                    napBoxError = if (isValid(value)) null else errorMessage
                )

                FormFieldKey.NOTE -> form.copy(
                    note = value as String
                )

                FormFieldKey.EQUIPMENT_CONDITION -> form.copy(
                    equipmentCondition = value as EquipmentCondition
                )
            }
            currentState.copy(registerSubscriptionForm = updatedForm)
        }
    }

    fun onFirstNameChanged(value: String) {
        // Limitar a 50 caracteres
        if (value.length <= 50) {
            updateField(
                fieldKey = FormFieldKey.FIRST_NAME,
                value = value,
                isValid = { it.isAValidName() },
                errorMessage = "El nombre debe tener al menos 2 caracteres"
            )
        }
    }

    fun onLastNameChanged(value: String) {
        // Limitar a 50 caracteres
        if (value.length <= 50) {
            updateField(
                fieldKey = FormFieldKey.LAST_NAME,
                value = value,
                isValid = { it.isAValidName() },
                errorMessage = "El apellido debe tener al menos 2 caracteres"
            )
        }
    }

    fun onDniChanged(value: String) {
        // Limitar a 8 dígitos
        if (value.length <= 8) {
            updateField(
                fieldKey = FormFieldKey.DNI,
                value = value,
                isValid = { it.isValidDni() },
                errorMessage = "El DNI debe contener 8 dígitos"
            )
        }
    }

    fun onAddressChanged(value: String) {
        updateField(
            fieldKey = FormFieldKey.ADDRESS,
            value = value,
            isValid = { it.isAValidAddress() },
            errorMessage = "La dirección debe tener al menos 5 caracteres"
        )
    }

    fun onPhoneChanged(value: String) {
        // Limitar a 9 dígitos
        if (value.length <= 9) {
            updateField(
                fieldKey = FormFieldKey.PHONE,
                value = value,
                isValid = { it.isValidPhone() },
                errorMessage = "El teléfono debe tener 9 dígitos"
            )
        }
    }

    fun onPlanSelected(value: PlanResponse) {
        updateField(
            fieldKey = FormFieldKey.PLAN,
            value = value,
            isValid = { it != null },
            errorMessage = "Debe seleccionar un plan"
        )
    }

    fun onPlaceSelected(value: Place) {
        updateField(
            fieldKey = FormFieldKey.PLACE,
            value = value,
            isValid = { it != null },
            errorMessage = "Debe seleccionar un lugar"
        )
    }

    fun onOnuSelected(value: Onu) {
        updateField(
            fieldKey = FormFieldKey.ONU,
            value = value,
            isValid = { it != null },
            errorMessage = "Debe seleccionar un ONU"
        )
    }

    fun onNapBoxSelected(value: NapBoxResponse) {
        updateField(
            fieldKey = FormFieldKey.NAP_BOX,
            value = value,
            isValid = { it != null },
            errorMessage = "Debe seleccionar un NapBox"
        )
    }

    fun onNoteChanged(value: String) {
        // Limitar la entrada a 180 caracteres
        if (value.length <= 180) {
            updateField(
                fieldKey = FormFieldKey.NOTE,
                value = value,
                isValid = { true }
            )
        }
    }

    fun onEquipmentConditionChanged(value: EquipmentCondition) {
        updateField(
            fieldKey = FormFieldKey.EQUIPMENT_CONDITION,
            value = value,
            isValid = { true }
        )
    }

    fun onPlaceSelectionCleared() {
        uiState.update {
            it.copy(
                registerSubscriptionForm = it.registerSubscriptionForm.copy(
                    selectedPlace = null,
                    placeError = null
                )
            )
        }
    }

    fun onNapBoxSelectionCleared() {
        uiState.update {
            it.copy(
                registerSubscriptionForm = it.registerSubscriptionForm.copy(
                    selectedNapBox = null,
                    napBoxError = null
                )
            )
        }
    }

    fun onInstallationTypeSelected(type: InstallationType) {
        val filteredPlans = when (type) {
            InstallationType.FIBER -> cachedPlanList.filter { it.type == InstallationType.FIBER }
            InstallationType.WIRELESS -> cachedPlanList.filter { it.type == InstallationType.WIRELESS }
            InstallationType.ONLY_TV_FIBER -> cachedPlanList.filter { it.type == InstallationType.ONLY_TV_FIBER }
        }

        // Si no hay planes disponibles para este tipo, no hacer nada
        if (filteredPlans.isEmpty())
            return

        // Verificar si el plan actual es compatible con el nuevo tipo de instalación
        val currentSelectedPlan = uiState.value.registerSubscriptionForm.selectedPlan
        val isCurrentPlanCompatible = currentSelectedPlan != null && 
                                     filteredPlans.any { it.id == currentSelectedPlan.id }

        // Determinar el plan seleccionado:
        // 1. Si el plan actual es compatible, mantenerlo
        // 2. Si hay solo un plan disponible, seleccionarlo automáticamente
        // 3. En otro caso, limpiar la selección (null)
        val selectedPlan = when {
            isCurrentPlanCompatible -> currentSelectedPlan
            filteredPlans.size == 1 -> filteredPlans.first()
            else -> null
        }

        uiState.update {
            it.copy(
                registerSubscriptionForm = it.registerSubscriptionForm.copy(
                    installationType = type,
                    planList = filteredPlans,
                    selectedPlan = selectedPlan,
                    selectedOnu = null,
                    selectedNapBox = null,
                )
            )
        }
    }

    fun getPlaceFromCurrentLocation(latitude: Double, longitude: Double) = viewModelScope.launch {
        getPlaceFromLocationUseCase(latitude, longitude).fold(
            onSuccess = { place ->
                uiState.update {
                    it.copy(
                        registerSubscriptionForm = it.registerSubscriptionForm.copy(
                            selectedPlace = place
                        )
                    )
                }
            },
            onFailure = { error ->

            }
        )
    }

    fun saveSubscription() {
        val form = uiState.value.registerSubscriptionForm

        if (!form.isValid()) {
            return
        }

        uiState.update {
            it.copy(isLoading = true)
        }

        val subscription = createSubscriptionFromForm(form)

        viewModelScope.launch {
            registerSubscriptionUseCase(subscription, uiState.value.orderId).fold(
                onSuccess = { registeredSubscription ->
                    uiState.update {
                        it.copy(
                            isLoading = false,
                            registeredSubscription = registeredSubscription,
                            error = "",
                            orderId = null
                        )
                    }

                },
                onFailure = { error ->
                    uiState.update {
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
        return Subscription(
            firstName = form.firstName.removeSpecialCharacters(),
            lastName = form.lastName.removeSpecialCharacters(),
            dni = form.dni,
            address = form.address,
            phone = form.phone,
            subscriptionDate = form.subscriptionDate,
            planId = form.selectedPlan!!.id,
            placeId = form.selectedPlace!!.id,
            technicianId = currentUser!!.id,
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
        uiState.update {
            it.copy(
                registeredSubscription = null,
                error = null
            )
        }
    }

    fun clearError() {
        uiState.update {
            it.copy(
                error = null
            )
        }
    }

    fun onLocationChanged(currentLocation: LatLng) {
        uiState.update {
            it.copy(
                registerSubscriptionForm = it.registerSubscriptionForm.copy(
                    location = currentLocation
                )
            )
        }
    }

    fun getNearbyNapBoxes(latitude: Double, longitude: Double) {
        viewModelScope.launch {

            getNearNapBoxesUseCase(latitude, longitude).fold(
                onSuccess = { napBoxes ->
                    cachedNapBoxList = napBoxes
                    val filteredNapBoxes =
                        currentUiState().registerSubscriptionForm.selectedPlace?.let { place ->
                            napBoxes.filter { it.placeId == place.id?.toInt() }
                        } ?: cachedNapBoxList

                    uiState.update {
                        it.copy(
                            registerSubscriptionForm = it.registerSubscriptionForm.copy(napBoxList = filteredNapBoxes),
                        )
                    }
                },
                onFailure = {

                }
            )
        }
    }

    /**
     * Carga los datos de una orden de instalación para prellenar el formulario
     */
    fun loadInstallationOrderData(orderId: Int) = viewModelScope.launch {
        uiState.update { it.copy(isLoading = true, orderId = orderId) }

        try {
            val order = installationOrderUseCase.getInstallationOrderById(orderId)
            val selectedPlace = order.place

            // Filtrar planes por tipo de instalación actual
            val filteredPlans =
                cachedPlanList.filter { it.type == uiState.value.registerSubscriptionForm.installationType }

            // Seleccionar automáticamente el plan si solo hay uno disponible
            val selectedPlan = if (filteredPlans.size == 1) filteredPlans.first() else null

            uiState.update { current ->
                current.copy(
                    isLoading = false,
                    registerSubscriptionForm = current.registerSubscriptionForm.copy(
                        firstName = order.customerFirstName,
                        lastName = order.customerLastName,
                        address = order.customerAddress,
                        phone = order.customerPhone,
                        dni = order.customerDni,
                        selectedPlace = selectedPlace,
                        selectedPlan = selectedPlan
                    )
                )
            }
        } catch (e: Exception) {
            uiState.update { current ->
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
            uiState.update { current ->
                current.copy(
                    error = e.message ?: "Error al cerrar la orden de instalación"
                )
            }
        }
    }

    private fun currentUiState() = uiState.value
}
