package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.domain.model.EquipmentCondition
import com.dscorp.ispadmin.domain.model.GeoLocation
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.NetworkDevice
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.domain.model.subscription.RegisterSubscriptionFormConstraints
import com.dscorp.ispadmin.domain.model.subscription.napBoxToPreselectAfterNearbyRefresh
import com.dscorp.ispadmin.domain.model.subscription.subscriptionFacadePhotoError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionNapBoxErrorAfterNearbyRefresh
import com.dscorp.ispadmin.domain.model.subscription.subscriptionOnuErrorAfterListRefresh
import com.dscorp.ispadmin.domain.usecase.InstallationOrderUseCase
import com.dscorp.ispadmin.domain.usecase.catalog.GetRegistrationCatalogUseCase
import com.dscorp.ispadmin.domain.usecase.catalog.RefreshRegistrationCatalogUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetAvailableOnuListUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetNearNapBoxesUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetPlaceFromLocationUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetUserSessionUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.ObserveOfflineRegistrationModeUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.RegisterSubscriptionResult
import com.dscorp.ispadmin.domain.usecase.subscription.PollRegistrationProgressUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.RegisterSubscriptionUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.RetryTr069ProvisioningUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.toSubscriptionOrNull
import com.dscorp.ispadmin.observability.ObsBreadcrumbCategory
import com.dscorp.ispadmin.observability.ObservabilityClient
import com.dscorp.ispadmin.presentation.extension.removeSpecialCharacters
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.mapper.toNapBoxResponse
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.mapper.toNetworkDevice
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.mapper.toOnu
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.mapper.toPlace
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.mapper.toPlanResponse
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.FormFieldKey
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.LocationCaptureMethod
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionFormState
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionIntent
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionState
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionUiEvent
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.TvCpeKind
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.canAdvanceWizardStep
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.isRegistrationVlanSelectable
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.wizardFieldsFor
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class RegisterSubscriptionComposeViewModel(
    private val getAvailableOnuListUseCase: GetAvailableOnuListUseCase,
    private val getRegistrationCatalogUseCase: GetRegistrationCatalogUseCase,
    private val refreshRegistrationCatalogUseCase: RefreshRegistrationCatalogUseCase,
    private val getPlaceFromLocationUseCase: GetPlaceFromLocationUseCase,
    private val registerSubscriptionUseCase: RegisterSubscriptionUseCase,
    private val getUserSessionUseCase: GetUserSessionUseCase,
    private val getNearNapBoxesUseCase: GetNearNapBoxesUseCase,
    private val installationOrderUseCase: InstallationOrderUseCase,
    private val observeOfflineRegistrationModeUseCase: ObserveOfflineRegistrationModeUseCase,
    private val retryTr069ProvisioningUseCase: RetryTr069ProvisioningUseCase,
    private val pollRegistrationProgressUseCase: PollRegistrationProgressUseCase,
    private val observabilityClient: ObservabilityClient,
    private val mainImmediate: CoroutineDispatcher = Dispatchers.Main.immediate
) : ViewModel() {

    private companion object {
        const val OBS_FEATURE = "subscription"
        const val OBS_SCREEN = "register_subscription"
    }

    private val _uiState = MutableStateFlow(RegisterSubscriptionState())
    val uiState: StateFlow<RegisterSubscriptionState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<RegisterSubscriptionUiEvent>(
        replay = 0,
        extraBufferCapacity = 16
    )
    val uiEvent: SharedFlow<RegisterSubscriptionUiEvent> = _uiEvent.asSharedFlow()

    fun onFacadePhotoSelected(uri: Uri) {
        updateValidatedForm(FormFieldKey.FACADE_PHOTO) { form ->
            form.copy(
                facadePhotoUri = uri,
                facadePhotoError = null
            )
        }
    }

    private val locationRequestGeneration = AtomicInteger(0)
    private var locationPipelineJob: Job? = null

    private var loadScreenJob: Job? = null
    private var refreshOnuJob: Job? = null
    private var registerSubscriptionJob: Job? = null
    private var retryTr069Job: Job? = null
    private var offlineModeJob: Job? = null

    fun loadScreenData(installationOrderId: Int?) {
        loadScreenJob?.cancel()
        observabilityClient.addBreadcrumb(
            category = ObsBreadcrumbCategory.NAVIGATION,
            message = "$OBS_FEATURE.load_screen_data",
            data = mapOf("feature" to OBS_FEATURE, "orderId" to installationOrderId)
        )
        loadScreenJob = viewModelScope.launch(mainImmediate) {
            try {
                _uiState.update { it.copy(isLoading = true) }
                observeOfflineMode()
                applyInitialCatalogData().exceptionOrNull()?.let { throwable ->
                    _uiState.update { it.copy(isLoading = false) }
                    observabilityClient.reportError(
                        throwable = throwable,
                        message = "Fallo al cargar catálogos iniciales",
                        tags = mapOf(
                            "feature" to OBS_FEATURE,
                            "screen" to OBS_SCREEN,
                            "action" to "load_initial_catalog",
                            "orderId" to installationOrderId
                        )
                    )
                    _uiEvent.emit(
                        RegisterSubscriptionUiEvent.Error(
                            throwable.message ?: "Unknown error"
                        )
                    )
                    return@launch
                }
                if (installationOrderId != null) {
                    _uiState.update { it.copy(orderId = installationOrderId) }
                    mergeInstallationOrderData(installationOrderId).exceptionOrNull()
                        ?.let { throwable ->
                            _uiState.update { it.copy(isLoading = false) }
                            observabilityClient.reportError(
                                throwable = throwable,
                                message = "Fallo al cargar datos de la orden de instalación",
                                tags = mapOf(
                                    "feature" to OBS_FEATURE,
                                    "screen" to OBS_SCREEN,
                                    "action" to "merge_installation_order",
                                    "orderId" to installationOrderId
                                )
                            )
                            _uiEvent.emit(
                                RegisterSubscriptionUiEvent.Error(
                                    throwable.message ?: "Error al cargar los datos de la orden"
                                )
                            )
                            return@launch
                        }
                }
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: CancellationException) {
                _uiState.update { it.copy(isLoading = false) }
                throw e
            }
        }
    }

    private suspend fun applyInitialCatalogData(): Result<Unit> = coroutineScope {
        refreshRegistrationCatalogUseCase()
        val catalogResult = getRegistrationCatalogUseCase()
        val userSession = getUserSessionUseCase()

        if (userSession.isFailure) {
            return@coroutineScope Result.failure(userSession.exceptionOrNull()!!)
        }
        val user = userSession.getOrNull()
            ?: return@coroutineScope Result.failure(IllegalStateException("Usuario no disponible"))
        val catalog = catalogResult.getOrNull()
            ?: return@coroutineScope Result.failure(
                catalogResult.exceptionOrNull()
                    ?: IllegalStateException("Catálogo no disponible")
            )

        val coreList = catalog.coreDevices.map { it.toNetworkDevice() }
        val activeCores = coreList.filter { !it.disabled }
        if (activeCores.isEmpty()) {
            return@coroutineScope Result.failure(
                IllegalStateException("No hay routers core disponibles")
            )
        }
        val autoSelected = if (activeCores.size == 1) activeCores.first() else null

        val cachedNapBoxes = catalog.napBoxes.map { it.toNapBoxResponse() }
        val cachedPlans = catalog.plans.map { it.toPlanResponse() }
        val filteredPlans = cachedPlans.filter { it.type == InstallationType.FIBER }
        val selectedPlan = getAutoSelectedPlan(filteredPlans, null)

        _uiState.update { current ->
            current.copy(
                currentUser = user,
                cachedNapBoxList = cachedNapBoxes,
                cachedPlanList = cachedPlans,
                registerSubscriptionForm = current.registerSubscriptionForm.copy(
                    onuList = catalog.onus.map { it.toOnu() },
                    planList = filteredPlans,
                    placeList = catalog.places.map { it.toPlace() },
                    napBoxList = cachedNapBoxes,
                    coreDeviceList = coreList,
                    selectedHostDevice = autoSelected,
                    hostDeviceError = null,
                    selectedPlan = selectedPlan
                )
            )
        }
        Result.success(Unit)
    }

    private suspend fun mergeInstallationOrderData(orderId: Int): Result<Unit> {
        return installationOrderUseCase.getInstallationOrderByIdResult(orderId).map { order ->
            val selectedPlace = order.place
            val currentInstallationType = _uiState.value.registerSubscriptionForm.installationType
            val filteredPlans =
                _uiState.value.cachedPlanList.filter { it.type == currentInstallationType }
            val selectedPlan = getAutoSelectedPlan(filteredPlans, null)
            val filteredNapBoxes = getFilteredNapBoxesForPlace(selectedPlace?.id)

            _uiState.update { current ->
                current.copy(
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
            Unit
        }
    }

    fun onIntent(intent: RegisterSubscriptionIntent) {
        when (intent) {
            is RegisterSubscriptionIntent.FirstNameChanged -> onFirstNameChanged(intent.value)
            is RegisterSubscriptionIntent.LastNameChanged -> onLastNameChanged(intent.value)
            is RegisterSubscriptionIntent.DniChanged -> onDniChanged(intent.value)
            is RegisterSubscriptionIntent.AddressChanged -> onAddressChanged(intent.value)
            is RegisterSubscriptionIntent.PhoneChanged -> onPhoneChanged(intent.value)
            is RegisterSubscriptionIntent.PlanSelected -> onPlanSelected(intent.value)
            is RegisterSubscriptionIntent.PlaceSelected -> onPlaceSelected(intent.value)
            is RegisterSubscriptionIntent.OnuSelected -> onOnuSelected(intent.value)
            is RegisterSubscriptionIntent.NapBoxSelected -> onNapBoxSelected(intent.value)
            is RegisterSubscriptionIntent.HostDeviceSelected -> onHostDeviceSelected(intent.device)
            RegisterSubscriptionIntent.PlaceSelectionCleared -> onPlaceSelectionCleared()
            RegisterSubscriptionIntent.NapBoxSelectionCleared -> onNapBoxSelectionCleared()
            is RegisterSubscriptionIntent.InstallationTypeSelected ->
                onInstallationTypeSelected(intent.type)
            RegisterSubscriptionIntent.RefreshOnuList -> refreshOnuList()
            is RegisterSubscriptionIntent.NoteChanged -> onNoteChanged(intent.value)
            is RegisterSubscriptionIntent.EquipmentConditionChanged ->
                onEquipmentConditionChanged(intent.value)
            is RegisterSubscriptionIntent.ClientIpAddressChanged ->
                onClientIpAddressChanged(intent.value)
            is RegisterSubscriptionIntent.OnVlanChanged -> onVlanChanged(intent.vlan)
            is RegisterSubscriptionIntent.TvCpeKindSelected -> onTvCpeKindSelected(intent.kind)
            is RegisterSubscriptionIntent.WifiSsid24Changed -> onWifiSsid24Changed(intent.value)
            is RegisterSubscriptionIntent.WifiPassword24Changed ->
                onWifiPassword24Changed(intent.value)
            is RegisterSubscriptionIntent.WifiSsid5Changed -> onWifiSsid5Changed(intent.value)
            is RegisterSubscriptionIntent.WifiPassword5Changed ->
                onWifiPassword5Changed(intent.value)
            is RegisterSubscriptionIntent.UseDifferentWifiNamesChanged ->
                onUseDifferentWifiNamesChanged(intent.enabled)
            is RegisterSubscriptionIntent.RegisterClick -> saveSubscription(intent.facadePhotoFile)
            is RegisterSubscriptionIntent.RetryTr069 -> retryTr069Provisioning(intent.subscriptionId)
            RegisterSubscriptionIntent.UseCurrentLocationClicked -> onUseCurrentLocationClicked()
            RegisterSubscriptionIntent.ChooseManualLocationClicked -> onChooseManualLocationClicked()
            RegisterSubscriptionIntent.DismissManualLocationMap -> onDismissManualLocationMap()
            is RegisterSubscriptionIntent.LocationCoordinatesSelected ->
                onLocationCoordinatesSelected(intent.latitude, intent.longitude)
            RegisterSubscriptionIntent.WizardContinueClicked -> onWizardContinueClicked()
            RegisterSubscriptionIntent.WizardBackClicked -> onWizardBackClicked()
        }
    }

    private fun onUseCurrentLocationClicked() {
        _uiState.update {
            it.copy(
                registerSubscriptionForm = it.registerSubscriptionForm.copy(
                    locationCaptureMethod = LocationCaptureMethod.CURRENT
                )
            )
        }
        viewModelScope.launch(mainImmediate) {
            _uiEvent.emit(RegisterSubscriptionUiEvent.RequestCurrentLocation)
        }
    }

    private fun onChooseManualLocationClicked() {
        _uiState.update {
            it.copy(
                showManualLocationMap = true,
                registerSubscriptionForm = it.registerSubscriptionForm.copy(
                    locationCaptureMethod = LocationCaptureMethod.MANUAL
                )
            )
        }
    }

    private fun onDismissManualLocationMap() {
        _uiState.update { it.copy(showManualLocationMap = false) }
    }

    private fun onLocationCoordinatesSelected(latitude: Double, longitude: Double) {
        _uiState.update { it.copy(showManualLocationMap = false) }
        processCurrentLocation(latitude, longitude)
    }

    private fun onWizardContinueClicked() {
        val current = currentUiState()
        val fields = wizardFieldsFor(current.wizardStep, current.registerSubscriptionForm)
        val validated = current.registerSubscriptionForm.validated(*fields.toTypedArray())
        val nextStep = if (canAdvanceWizardStep(current.wizardStep, validated)) {
            current.wizardStep.next() ?: current.wizardStep
        } else {
            current.wizardStep
        }
        _uiState.update {
            it.copy(
                registerSubscriptionForm = validated,
                wizardStep = nextStep,
            )
        }
    }

    private fun onWizardBackClicked() {
        _uiState.update { current ->
            val previous = current.wizardStep.previous() ?: return@update current
            current.copy(wizardStep = previous)
        }
    }

    fun retryTr069Provisioning(subscriptionId: Int) {
        if (_uiState.value.tr069RetryLoading) return
        retryTr069Job?.cancel()
        retryTr069Job = viewModelScope.launch(mainImmediate) {
            try {
                _uiState.update { it.copy(tr069RetryLoading = true) }
                retryTr069ProvisioningUseCase(subscriptionId).fold(
                    onSuccess = { updated ->
                        _uiState.update { it.copy(tr069RetryLoading = false) }
                        _uiEvent.emit(RegisterSubscriptionUiEvent.Success(updated))
                        if (updated.tr069ProvisionStatus == "MANUAL_REQUIRED") {
                            _uiEvent.emit(
                                RegisterSubscriptionUiEvent.Error(
                                    updated.tr069Message
                                        ?: "No se pudo completar el aprovisionamiento TR-069"
                                )
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(tr069RetryLoading = false) }
                        _uiEvent.emit(
                            RegisterSubscriptionUiEvent.Error(
                                error.message ?: "No se pudo reintentar el aprovisionamiento TR-069"
                            )
                        )
                    }
                )
            } catch (e: CancellationException) {
                _uiState.update { it.copy(tr069RetryLoading = false) }
                throw e
            }
        }
    }

    fun refreshOnuList() {
        refreshOnuJob?.cancel()
        refreshOnuJob = viewModelScope.launch(mainImmediate) {
            try {
                _uiState.update { it.copy(isRefreshingOnuList = true) }
                getAvailableOnuListUseCase().fold(
                    onSuccess = { refreshedOnuList ->
                        _uiState.update { current ->
                            val currentForm = current.registerSubscriptionForm
                            val selectedOnu = currentForm.selectedOnu?.takeIf { selected ->
                                refreshedOnuList.any { it.sn == selected.sn }
                            }
                            current.copy(
                                isRefreshingOnuList = false,
                                registerSubscriptionForm = currentForm.copy(
                                    onuList = refreshedOnuList,
                                    selectedOnu = selectedOnu,
                                    onuError = subscriptionOnuErrorAfterListRefresh(
                                        requiresOnu = currentForm.requiresOnu(),
                                        previousSelected = currentForm.selectedOnu,
                                        newSelected = selectedOnu,
                                        newList = refreshedOnuList,
                                        previousFieldError = currentForm.onuError
                                    )
                                )
                            )
                        }
            },
            onFailure = { error ->
                _uiState.update { it.copy(isRefreshingOnuList = false) }
                observabilityClient.reportError(
                    throwable = error,
                    message = "Fallo al actualizar lista de ONUs",
                    tags = mapOf(
                        "feature" to OBS_FEATURE,
                        "screen" to OBS_SCREEN,
                        "action" to "refresh_onu_list"
                    )
                )
                _uiEvent.emit(
                    RegisterSubscriptionUiEvent.Error(
                        error.message ?: "Error al actualizar la lista de ONUs"
                    )
                )
            }
            )
        } catch (e: CancellationException) {
            _uiState.update { it.copy(isRefreshingOnuList = false) }
            throw e
        }
    }
}

private fun onFirstNameChanged(value: String) {
    val upperValue = value.uppercase()
    if (upperValue.length > RegisterSubscriptionFormConstraints.MAX_PERSON_NAME_LENGTH) return

    updateValidatedForm(FormFieldKey.FIRST_NAME) { form ->
        form.copy(firstName = upperValue)
    }
}

private fun onLastNameChanged(value: String) {
    val upperValue = value.uppercase()
        if (upperValue.length > RegisterSubscriptionFormConstraints.MAX_PERSON_NAME_LENGTH) return

    updateValidatedForm(FormFieldKey.LAST_NAME) { form ->
        form.copy(lastName = upperValue)
    }
}

private fun onDniChanged(value: String) {
    if (value.length > RegisterSubscriptionFormConstraints.MAX_DNI_INPUT_LENGTH) return

    updateValidatedForm(FormFieldKey.DNI) { form ->
        form.copy(dni = value)
    }
}

private fun onAddressChanged(value: String) {
    updateValidatedForm(FormFieldKey.ADDRESS) { form ->
        form.copy(address = value)
    }
}

private fun onPhoneChanged(value: String) {
    if (value.length > RegisterSubscriptionFormConstraints.MAX_PHONE_LENGTH) return

    updateValidatedForm(FormFieldKey.PHONE) { form ->
        form.copy(phone = value)
    }
}

private fun onPlanSelected(value: PlanResponse) {
    observabilityClient.addBreadcrumb(
        category = ObsBreadcrumbCategory.USER_ACTION,
        message = "$OBS_FEATURE.plan_selected",
        data = mapOf("feature" to OBS_FEATURE, "planId" to value.id)
    )
    updateValidatedForm(FormFieldKey.PLAN) { form ->
        form.copy(selectedPlan = value)
    }
}

private fun onPlaceSelected(value: Place) {
    val filteredNapBoxes = getFilteredNapBoxesForPlace(value.id)
    observabilityClient.addBreadcrumb(
        category = ObsBreadcrumbCategory.USER_ACTION,
        message = "$OBS_FEATURE.place_selected",
        data = mapOf("feature" to OBS_FEATURE, "placeId" to value.id)
    )

    updateValidatedForm(FormFieldKey.PLACE, FormFieldKey.NAP_BOX) { form ->
        form.copy(
            selectedPlace = value,
            napBoxList = filteredNapBoxes,
            selectedNapBox = form.selectedNapBox?.takeIf { selected ->
                filteredNapBoxes.any { it.id == selected.id }
            } ?: filteredNapBoxes.firstOrNull()
        )
    }
}

private fun onOnuSelected(value: Onu) {
    updateValidatedForm(FormFieldKey.ONU) { form ->
        form.copy(selectedOnu = value)
    }
}

private fun onNapBoxSelected(value: NapBoxResponse) {
    updateValidatedForm(FormFieldKey.NAP_BOX) { form ->
        form.copy(selectedNapBox = value)
    }
}

private fun onHostDeviceSelected(device: NetworkDevice) {
    updateValidatedForm(FormFieldKey.HOST_DEVICE) { form ->
        form.copy(selectedHostDevice = device)
    }
}

private fun onNoteChanged(value: String) {
    updateValidatedForm(FormFieldKey.NOTE) { form ->
        form.copy(note = value)
    }
}

private fun onEquipmentConditionChanged(value: EquipmentCondition) {
    updateValidatedForm(FormFieldKey.EQUIPMENT_CONDITION) { form ->
        form.copy(equipmentCondition = value)
    }
}

private fun onClientIpAddressChanged(value: String) {
    updateValidatedForm(FormFieldKey.CLIENT_IP_ADDRESS) { form ->
        form.copy(clientIpAddress = value)
    }
}

private fun onVlanChanged(vlan: String) {
    if (!isRegistrationVlanSelectable(vlan)) return
    _uiState.update { current ->
        current.copy(
            registerSubscriptionForm = current.registerSubscriptionForm.copy(vlan = vlan)
        )
    }
}

private fun onTvCpeKindSelected(kind: TvCpeKind) {
    updateValidatedForm(FormFieldKey.TV_CPE_KIND, FormFieldKey.ONU) { form ->
        form.copy(
            tvCpeKind = kind,
            selectedOnu = if (kind == TvCpeKind.ONU) form.selectedOnu else null,
            onuError = if (kind == TvCpeKind.ONU) form.onuError else null,
        )
    }
}

private fun onWifiSsid24Changed(value: String) {
    if (value.length > RegisterSubscriptionFormConstraints.MAX_WIFI_SSID_LENGTH) return
    updateValidatedForm(FormFieldKey.WIFI_SSID_24) { form ->
        form.copy(wifiSsid24 = value)
    }
}

private fun onWifiPassword24Changed(value: String) {
    if (value.length > RegisterSubscriptionFormConstraints.MAX_WIFI_PASSWORD_LENGTH) return
    updateValidatedForm(FormFieldKey.WIFI_PASSWORD_24) { form ->
        form.copy(wifiPassword24 = value)
    }
}

private fun onWifiSsid5Changed(value: String) {
    if (value.length > RegisterSubscriptionFormConstraints.MAX_WIFI_SSID_LENGTH) return
    updateValidatedForm(FormFieldKey.WIFI_SSID_5) { form ->
        form.copy(wifiSsid5 = value)
    }
}

private fun onWifiPassword5Changed(value: String) {
    if (value.length > RegisterSubscriptionFormConstraints.MAX_WIFI_PASSWORD_LENGTH) return
    updateValidatedForm(FormFieldKey.WIFI_PASSWORD_5) { form ->
        form.copy(wifiPassword5 = value)
    }
}

private fun onUseDifferentWifiNamesChanged(enabled: Boolean) {
    updateValidatedForm(FormFieldKey.WIFI_SSID_24, FormFieldKey.WIFI_SSID_5) { form ->
        form.copy(useDifferentWifiNames = enabled)
    }
}

private fun observeOfflineMode() {
    if (offlineModeJob?.isActive == true) return
    offlineModeJob = viewModelScope.launch(mainImmediate) {
        observeOfflineRegistrationModeUseCase().getOrElse { return@launch }.collect { offline ->
            _uiState.update { current ->
                current.copy(
                    isOfflineMode = offline,
                    registerSubscriptionForm = current.registerSubscriptionForm.copy(
                        requiresClientIpAddress = offline
                    )
                )
            }
        }
    }
}

private fun onPlaceSelectionCleared() {
    updateValidatedForm(FormFieldKey.PLACE, FormFieldKey.NAP_BOX) { form ->
        form.copy(
            selectedPlace = null,
            selectedNapBox = null,
            napBoxList = getFilteredNapBoxesForPlace(null)
        )
    }
}

private fun onNapBoxSelectionCleared() {
    updateValidatedForm(FormFieldKey.NAP_BOX) { form ->
        form.copy(selectedNapBox = null)
    }
}

private fun onInstallationTypeSelected(type: InstallationType) {
    val filteredPlans = getFilteredPlansForInstallationType(type)

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
                tvCpeKind = null,
                tvCpeKindError = null,
                wifiSsid24 = "",
                wifiPassword24 = "",
                wifiSsid5 = "",
                wifiPassword5 = "",
                useDifferentWifiNames = false,
                wifiSsid24Error = null,
                wifiPassword24Error = null,
                wifiSsid5Error = null,
                wifiPassword5Error = null,
            ).validated(
                FormFieldKey.PLAN,
                FormFieldKey.ONU,
                FormFieldKey.NAP_BOX,
                FormFieldKey.TV_CPE_KIND,
            )
        )
    }
}

fun processCurrentLocation(latitude: Double, longitude: Double) {
    onLocationChanged(LatLng(latitude, longitude))
    locationPipelineJob?.cancel()
    val expectedGen = locationRequestGeneration.incrementAndGet()
    _uiState.update { it.copy(isLoadingLocation = true) }
    locationPipelineJob = viewModelScope.launch(mainImmediate) {
        try {
            coroutineScope {
                launch { resolvePlaceFromLocation(expectedGen, latitude, longitude) }
                launch { fetchNearbyNapBoxes(expectedGen, latitude, longitude) }
            }
        } finally {
            if (expectedGen == locationRequestGeneration.get()) {
                _uiState.update { it.copy(isLoadingLocation = false) }
            }
        }
    }
}

fun getNearbyNapBoxes(latitude: Double, longitude: Double) {
    locationPipelineJob?.cancel()
    val expectedGen = locationRequestGeneration.incrementAndGet()
    locationPipelineJob = viewModelScope.launch(mainImmediate) {
        fetchNearbyNapBoxes(expectedGen, latitude, longitude)
    }
}

private suspend fun resolvePlaceFromLocation(
    expectedGen: Int,
    latitude: Double,
    longitude: Double
) {
    getPlaceFromLocationUseCase(latitude, longitude).fold(
        onSuccess = { place ->
            if (expectedGen != locationRequestGeneration.get()) return@fold
            onPlaceSelected(place)
        },
        onFailure = { error ->
            if (expectedGen != locationRequestGeneration.get()) return@fold
            observabilityClient.reportError(
                throwable = error,
                message = "Fallo al resolver lugar desde ubicación",
                tags = mapOf(
                    "feature" to OBS_FEATURE,
                    "screen" to OBS_SCREEN,
                    "action" to "resolve_place_from_location",
                    "latitude" to latitude,
                    "longitude" to longitude
                )
            )
            _uiEvent.emit(
                RegisterSubscriptionUiEvent.Error(
                    error.message ?: "No se pudo obtener el lugar desde la ubicación"
                )
            )
        }
    )
}

private suspend fun fetchNearbyNapBoxes(
    expectedGen: Int,
    latitude: Double,
    longitude: Double
) {
    _uiState.update { it.copy(isLoadingNearbyNapBoxes = true) }
    try {
        getNearNapBoxesUseCase(latitude, longitude).fold(
            onSuccess = { napBoxes ->
                if (expectedGen != locationRequestGeneration.get()) return@fold
                val currentForm = currentUiState().registerSubscriptionForm
                val selectedPlace = currentForm.selectedPlace
                val selectedNapBox = napBoxToPreselectAfterNearbyRefresh(
                    nearbyOrdered = napBoxes,
                    placeId = selectedPlace?.id?.toInt(),
                )
                val filteredNapBoxes = selectedPlace?.id?.toInt()?.let { placeId ->
                    napBoxes.filter { it.placeId == placeId }
                } ?: napBoxes

                _uiState.update {
                    it.copy(
                        isLoadingNearbyNapBoxes = false,
                        cachedNapBoxList = napBoxes,
                        registerSubscriptionForm = it.registerSubscriptionForm.copy(
                            napBoxList = filteredNapBoxes,
                            selectedNapBox = selectedNapBox,
                            napBoxError = subscriptionNapBoxErrorAfterNearbyRefresh(
                                requiresNapBox = currentForm.requiresNapBox(),
                                previousSelected = currentForm.selectedNapBox,
                                newSelected = selectedNapBox,
                                newList = filteredNapBoxes,
                                previousFieldError = currentForm.napBoxError
                            )
                        )
                    )
                }
            },
            onFailure = { error ->
                if (expectedGen != locationRequestGeneration.get()) return@fold
                _uiState.update {
                    it.copy(isLoadingNearbyNapBoxes = false)
                }
                observabilityClient.reportError(
                    throwable = error,
                    message = "Fallo al obtener cajas NAP cercanas",
                    tags = mapOf(
                        "feature" to OBS_FEATURE,
                        "screen" to OBS_SCREEN,
                        "action" to "fetch_nearby_nap_boxes",
                        "latitude" to latitude,
                        "longitude" to longitude
                    )
                )
                _uiEvent.emit(
                    RegisterSubscriptionUiEvent.Error(
                        error.message ?: "Error al obtener cajas NAP cercanas"
                    )
                )
            }
        )
    } finally {
        if (expectedGen == locationRequestGeneration.get()) {
            _uiState.update { it.copy(isLoadingNearbyNapBoxes = false) }
        }
    }
}

fun saveSubscription(facadePhotoFile: File? = null) {
    val form = uiState.value.registerSubscriptionForm
    val validatedForm = form.validated()
    val hasFacadePhoto = form.facadePhotoUri != null || facadePhotoFile != null
    observabilityClient.addBreadcrumb(
        category = ObsBreadcrumbCategory.USER_ACTION,
        message = "$OBS_FEATURE.register_click",
        data = mapOf(
            "feature" to OBS_FEATURE,
            "installationType" to form.installationType.name,
            "hasFacadePhoto" to hasFacadePhoto,
            "hasOrder" to (uiState.value.orderId != null)
        )
    )
    val invalidFields = form.blockingFields().filter { field ->
        when (field) {
            FormFieldKey.FACADE_PHOTO -> !hasFacadePhoto
            else -> validatedForm.validate(field) != null
        }
    }

    if (invalidFields.isNotEmpty()) {
        observabilityClient.reportLog(
            message = "Registro bloqueado por validación de formulario",
            severity = "warning",
            tags = mapOf(
                "feature" to OBS_FEATURE,
                "screen" to OBS_SCREEN,
                "action" to "save_subscription_validation",
                "invalidFields" to invalidFields.map { it.name }
            )
        )
        _uiState.update {
            it.copy(
                registerSubscriptionForm = validatedForm.copy(
                    facadePhotoError = if (!hasFacadePhoto) {
                        subscriptionFacadePhotoError(false)
                    } else {
                        null
                    }
                )
            )
        }
        return
    }

    if (registerSubscriptionJob?.isActive == true) {
        return
    }

    val subscription = buildSubscriptionFromForm(validatedForm)
    if (subscription == null) {
        observabilityClient.reportError(
            throwable = IllegalStateException("Usuario no disponible para crear suscripción"),
            message = "Usuario no disponible al construir suscripción",
            tags = mapOf(
                "feature" to OBS_FEATURE,
                "screen" to OBS_SCREEN,
                "action" to "build_subscription"
            )
        )
        viewModelScope.launch(mainImmediate) {
            _uiEvent.emit(
                RegisterSubscriptionUiEvent.Error("Usuario no disponible para crear suscripción")
            )
        }
        return
    }

    val orderIdSnapshot = uiState.value.orderId

    registerSubscriptionJob = viewModelScope.launch(mainImmediate) {
        try {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    registrationProgressMessage = "Registrando y autorizando…"
                )
            }

            registerSubscriptionUseCase(
                subscription,
                orderIdSnapshot,
                facadePhotoFile = facadePhotoFile
            ).fold(
                onSuccess = { outcome ->
                    when (outcome) {
                        is RegisterSubscriptionResult.Registered -> {
                            observabilityClient.addBreadcrumb(
                                category = ObsBreadcrumbCategory.STATE,
                                message = "$OBS_FEATURE.register_success",
                                data = mapOf("feature" to OBS_FEATURE, "orderId" to orderIdSnapshot)
                            )
                            val enriched = outcome.subscription.copy(
                                wifiSsid24 = outcome.subscription.wifiSsid24
                                    ?: subscription.wifiSsid24,
                                wifiSsid5 = outcome.subscription.wifiSsid5
                                    ?: subscription.wifiSsid5,
                                wifiPassword24 = subscription.wifiPassword24,
                                wifiPassword5 = subscription.wifiPassword5
                            )
                            val subscriptionId = enriched.resolvedSubscriptionId()
                            if (subscriptionId != null &&
                                (enriched.provisioningPending || enriched.tr069ProvisionStatus == "PENDING")
                            ) {
                                pollRegistrationProgress(subscriptionId, enriched)
                            } else {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        orderId = null,
                                        registrationProgressMessage = "Registrando…"
                                    )
                                }
                                _uiEvent.emit(RegisterSubscriptionUiEvent.Success(enriched))
                            }
                        }
                        is RegisterSubscriptionResult.QueuedOffline -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    orderId = null
                                )
                            }
                            observabilityClient.addBreadcrumb(
                                category = ObsBreadcrumbCategory.STATE,
                                message = "$OBS_FEATURE.register_queued_offline",
                                data = mapOf("feature" to OBS_FEATURE, "orderId" to orderIdSnapshot)
                            )
                            _uiEvent.emit(RegisterSubscriptionUiEvent.QueuedOffline)
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false)
                    }
                    observabilityClient.reportError(
                        throwable = error,
                        message = "Fallo al registrar suscripción",
                        tags = mapOf(
                            "feature" to OBS_FEATURE,
                            "screen" to OBS_SCREEN,
                            "action" to "save_subscription",
                            "entityId" to orderIdSnapshot,
                            "orderId" to orderIdSnapshot,
                            "installationType" to subscription.installationType?.name,
                            "hasFacadePhoto" to (facadePhotoFile != null),
                            "planId" to subscription.planId,
                            "placeId" to subscription.placeId,
                            "hasNapBox" to (subscription.napBoxId != null),
                            "hasOnu" to (subscription.onu != null)
                        )
                    )
                    _uiEvent.emit(
                        RegisterSubscriptionUiEvent.Error(
                            error.message ?: "Error al registrar la suscripción"
                        )
                    )
                }
            )
        } catch (e: CancellationException) {
            _uiState.update { it.copy(isLoading = false) }
            throw e
        }
    }
}

private suspend fun pollRegistrationProgress(
    subscriptionId: Int,
    initial: Subscription,
) {
    _uiState.update {
        it.copy(
            isLoading = true,
            registrationProgressMessage = initial.tr069Message?.takeIf { msg -> msg.isNotBlank() }
                ?: "Esperando ACS…"
        )
    }
    pollRegistrationProgressUseCase(subscriptionId) { progress ->
        _uiState.update {
            it.copy(registrationProgressMessage = progress.message)
        }
    }.fold(
        onSuccess = { progress ->
            val finalSubscription = progress.toSubscriptionOrNull(wifiFallback = initial) ?: initial.copy(
                tr069ProvisionStatus = progress.tr069ProvisionStatus ?: initial.tr069ProvisionStatus,
                tr069Message = progress.tr069Message ?: initial.tr069Message,
                mikrotikProvisionStatus = progress.mikrotikProvisionStatus
                    ?: initial.mikrotikProvisionStatus,
                oltProvisionStatus = progress.oltProvisionStatus ?: initial.oltProvisionStatus,
                provisioningPending = !progress.done,
            )
            _uiState.update {
                it.copy(
                    isLoading = false,
                    orderId = null,
                    registrationProgressMessage = "Registrando…"
                )
            }
            _uiEvent.emit(RegisterSubscriptionUiEvent.Success(finalSubscription))
            if (finalSubscription.tr069ProvisionStatus == "MANUAL_REQUIRED") {
                _uiEvent.emit(
                    RegisterSubscriptionUiEvent.Error(
                        finalSubscription.tr069Message
                            ?: "No se pudo completar el aprovisionamiento TR-069"
                    )
                )
            }
        },
        onFailure = { error ->
            _uiState.update {
                it.copy(
                    isLoading = false,
                    orderId = null,
                    registrationProgressMessage = "Registrando…"
                )
            }
            _uiEvent.emit(RegisterSubscriptionUiEvent.Success(initial))
            _uiEvent.emit(
                RegisterSubscriptionUiEvent.Error(
                    error.message ?: "No se pudo obtener el progreso del aprovisionamiento"
                )
            )
        }
    )
}

private fun buildSubscriptionFromForm(
    form: RegisterSubscriptionFormState
): Subscription? {
    val user = currentUiState().currentUser ?: return null

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
        onu = form.selectedOnu.takeIf { form.requiresOnu() },
        equipmentCondition = form.equipmentCondition,
        autoCut = true,
        facadePhotoUrl = null,
        clientIpAddress = form.clientIpAddress.trim().takeIf { it.isNotEmpty() },
        ip = form.clientIpAddress.trim().takeIf { it.isNotEmpty() },
        vlan = form.vlan.takeIf { form.requiresOnu() },
        wifiSsid24 = form.wifiSsid24.trim().takeIf { form.requiresWifiConfig() },
        wifiPassword24 = form.wifiPassword24.takeIf { form.requiresWifiConfig() },
        wifiSsid5 = form.resolvedWifiSsid5().takeIf { form.requiresWifiConfig() },
        wifiPassword5 = form.wifiPassword24.takeIf { form.requiresWifiConfig() }
    )
}

fun onLocationChanged(currentLocation: LatLng) {
    updateValidatedForm(FormFieldKey.LOCATION) { form ->
        form.copy(location = currentLocation)
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

private fun getFilteredPlansForInstallationType(type: InstallationType): List<PlanResponse> {
    return currentUiState().cachedPlanList.filter { it.type == type }
}

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

private fun getFilteredNapBoxesForPlace(placeId: String?): List<NapBoxResponse> {
    if (placeId == null) return currentUiState().cachedNapBoxList
    val placeIdInt = placeId.toIntOrNull() ?: return currentUiState().cachedNapBoxList
    return currentUiState().cachedNapBoxList.filter { it.placeId == placeIdInt }
}
}
