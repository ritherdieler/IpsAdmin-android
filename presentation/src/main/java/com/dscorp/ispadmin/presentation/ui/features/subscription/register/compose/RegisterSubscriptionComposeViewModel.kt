package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.domain.model.AccessMode
import com.dscorp.ispadmin.domain.model.EquipmentCondition
import com.dscorp.ispadmin.domain.model.GeoLocation
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.NetworkDevice
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.OnuRegistrationCancellationIntent
import com.dscorp.ispadmin.domain.model.OnuRegistrationOperation
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.RegistrationProgress
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.domain.model.canManuallyRetry
import com.dscorp.ispadmin.domain.model.canOpenRegistrationForm
import com.dscorp.ispadmin.domain.model.subscription.RegisterSubscriptionFormConstraints
import com.dscorp.ispadmin.domain.model.subscription.napBoxToPreselectAfterNearbyRefresh
import com.dscorp.ispadmin.domain.model.subscription.subscriptionFacadePhotoError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionNapBoxErrorAfterNearbyRefresh
import com.dscorp.ispadmin.domain.model.subscription.subscriptionOnuErrorAfterListRefresh
import com.dscorp.ispadmin.domain.repository.OnuRegistrationCancellationIntentStore
import com.dscorp.ispadmin.domain.repository.OnuRegistrationSelectionStore
import com.dscorp.ispadmin.domain.repository.OnuRegistrationTarget
import com.dscorp.ispadmin.domain.repository.StartOnuRegistrationRequest
import com.dscorp.ispadmin.domain.repository.SubscriptionActionsRepository
import com.dscorp.ispadmin.domain.usecase.InstallationOrderUseCase
import com.dscorp.ispadmin.domain.usecase.catalog.GetRegistrationCatalogUseCase
import com.dscorp.ispadmin.domain.usecase.catalog.RefreshRegistrationCatalogUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetAvailableOnuListUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetNearNapBoxesUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetPlaceFromLocationUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetUserSessionUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.ObserveOfflineRegistrationModeUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.OnuRegistrationOperationUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.PollRegistrationProgressUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.RegisterSubscriptionUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.RegistrationSubmission
import com.dscorp.ispadmin.domain.usecase.subscription.RetryTr069ProvisioningUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.SubmitAndTrackRegistrationUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.toSubscriptionOrNull
import com.dscorp.ispadmin.observability.ObsBreadcrumbCategory
import com.dscorp.ispadmin.observability.ObservabilityClient
import com.dscorp.ispadmin.observability.WorkflowStatus
import com.dscorp.ispadmin.presentation.extension.removeSpecialCharacters
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.E2eAccessModeResolver
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
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.SubmissionState
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.File
import java.util.UUID
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
    private val mainImmediate: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val onuRegistrationOperationUseCase: OnuRegistrationOperationUseCase? = null,
    private val cancellationIntentStore: OnuRegistrationCancellationIntentStore? = null,
    private val onuRegistrationSelectionStore: OnuRegistrationSelectionStore? = null,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val subscriptionActionsRepository: SubscriptionActionsRepository? = null,
    private val submitAndTrackRegistrationUseCase: SubmitAndTrackRegistrationUseCase =
        SubmitAndTrackRegistrationUseCase(registerSubscriptionUseCase, onuRegistrationOperationUseCase),
) : ViewModel() {

    private companion object {
        const val OBS_FEATURE = "subscription"
        const val OBS_SCREEN = "register_subscription"
        const val CLIENT_REQUEST_ID_KEY = "register_subscription_client_request_id"
        const val DNI_CHECK_MIN_LENGTH = 8
        const val DNI_CHECK_DEBOUNCE_MS = 600L
        const val DRAFT_SAVE_DEBOUNCE_MS = 2_000L
        const val PREAUTH_POLL_INTERVAL_MS = 3_000L
        const val PREAUTH_MAX_POLLS = 100
        const val OBS_WORKFLOW_NAME = "registro_suscripcion"
        const val OBS_WORKFLOW_CATEGORY = "registration"
    }

    private val _uiState = MutableStateFlow(
        RegisterSubscriptionState(
            wizardStep = if (onuRegistrationOperationUseCase != null) {
                RegisterSubscriptionWizardStep.ONU_SELECTION
            } else RegisterSubscriptionWizardStep.CLIENT_LOCATION,
            preauthorizationEnabled = onuRegistrationOperationUseCase != null,
            registerSubscriptionForm = RegisterSubscriptionFormState(
                accessMode = E2eAccessModeResolver.resolve(),
            )
        )
    )
    val uiState: StateFlow<RegisterSubscriptionState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<RegisterSubscriptionUiEvent>(
        replay = 0,
        extraBufferCapacity = 16
    )
    val uiEvent: SharedFlow<RegisterSubscriptionUiEvent> = _uiEvent.asSharedFlow()

    fun onFacadePhotoSelected(uri: Uri, photoFile: File? = null) {
        updateValidatedForm(FormFieldKey.FACADE_PHOTO) { form ->
            form.copy(
                facadePhotoUri = uri,
                facadePhotoUrl = null,
                facadePhotoError = null
            )
        }
        val operation = _uiState.value.preauthorizationOperation
        if (onuRegistrationOperationUseCase != null && operation?.canOpenRegistrationForm() == true && photoFile != null) {
            photoUploadJob?.cancel()
            photoUploadJob = viewModelScope.launch(mainImmediate) {
                _uiState.update { it.copy(preauthorizationError = null, isLoading = true) }
                try {
                    val url = onuRegistrationOperationUseCase.uploadPhoto(operation.id, photoFile)
                    _uiState.update { state ->
                        state.copy(isLoading = false, registerSubscriptionForm = state.registerSubscriptionForm.copy(facadePhotoUrl = url))
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    _uiState.update { it.copy(isLoading = false, preauthorizationError = error.message ?: "No se pudo cargar la foto") }
                } finally {
                    photoFile.delete()
                    if (cancellationRequested) {
                        viewModelScope.launch(mainImmediate) {
                            yield()
                            startDeferredCancellation()
                        }
                    }
                }
            }
        }
    }

    private val locationRequestGeneration = AtomicInteger(0)
    private var locationPipelineJob: Job? = null

    private var loadScreenJob: Job? = null
    private var refreshOnuJob: Job? = null
    private var registerSubscriptionJob: Job? = null
    private var retryTr069Job: Job? = null
    private var offlineModeJob: Job? = null
    private var photoUploadJob: Job? = null
    private var cancellationJob: Job? = null
    private var resumeProvisioningJob: Job? = null
    private var registrationWorkflowId: String? = null
    private var dniCheckJob: Job? = null
    private var cancellationRequested = false
    private var subscriptionSubmissionInFlight = false
    private var registrationRequestKeyInFlight: String? = null
    private var registrationSerialInFlight: String? = null
    private var pendingCancellationIntent: OnuRegistrationCancellationIntent? = null

    init {
        onuRegistrationOperationUseCase?.let { operationUseCase ->
            viewModelScope.launch(mainImmediate) {
                uiState.map { state ->
                    val operation = state.preauthorizationOperation
                    if (operation?.canOpenRegistrationForm() == true) {
                        operation.id to buildRegistrationDraft(state)
                    } else null
                }.distinctUntilChanged().collectLatest { snapshot ->
                    if (snapshot != null) {
                        delay(DRAFT_SAVE_DEBOUNCE_MS)
                        try {
                            operationUseCase.saveDraft(snapshot.first, snapshot.second)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            _uiState.update { it.copy(preauthorizationError = error.message ?: "No se pudo guardar el borrador") }
                        }
                    }
                }
            }
        }
    }

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
                if (onuRegistrationOperationUseCase != null) {
                    restorePreauthorizationSelection()
                    restoreActivePreauthorization()
                }
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: CancellationException) {
                _uiState.update { it.copy(isLoading = false) }
                throw e
            } finally {
                if (cancellationRequested) startDeferredCancellation()
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
            is RegisterSubscriptionIntent.AccessModeSelected ->
                onAccessModeSelected(intent.mode)
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
            is RegisterSubscriptionIntent.RetryTr069 -> retryTr069Provisioning(intent.subscription)
            RegisterSubscriptionIntent.RetryOnuRegistration -> retryOnuRegistration()
            RegisterSubscriptionIntent.CancelOnuRegistration -> cancelOnuRegistration()
            RegisterSubscriptionIntent.ConfirmCancelOnuRegistration -> confirmCancelOnuRegistration()
            RegisterSubscriptionIntent.DismissCancelOnuRegistration ->
                _uiState.update { it.copy(showCancelConfirmation = false) }
            RegisterSubscriptionIntent.DismissRegistrationResult ->
                _uiState.update { it.copy(submission = SubmissionState.Idle) }
            RegisterSubscriptionIntent.LeaveDuringRegistration -> onLeaveDuringRegistration()
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
        if (current.preauthorizationEnabled) {
            when (current.wizardStep) {
                RegisterSubscriptionWizardStep.ONU_SELECTION -> {
                    val validated = current.registerSubscriptionForm.validated(FormFieldKey.ONU)
                    val onuError = validated.validate(FormFieldKey.ONU)
                    if (validated.selectedOnu != null && onuError == null) {
                        _uiState.update { it.copy(registerSubscriptionForm = validated) }
                        startOnuPreauthorization()
                    } else {
                        _uiState.update {
                            it.copy(
                                registerSubscriptionForm = validated.copy(
                                    onuError = onuError ?: "Selecciona una ONU para autorizarla."
                                )
                            )
                        }
                    }
                }
                RegisterSubscriptionWizardStep.ONU_CONFIRMATION -> startOnuPreauthorization()
                RegisterSubscriptionWizardStep.WAITING_FOR_ACS -> Unit
                else -> advanceExistingWizard(current)
            }
            return
        }
        advanceExistingWizard(current)
    }

    private fun advanceExistingWizard(current: RegisterSubscriptionState) {
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

    private fun startOnuPreauthorization() {
        val useCase = onuRegistrationOperationUseCase ?: return
        val selectedOnu = currentUiState().registerSubscriptionForm.selectedOnu ?: return
        val vlan = currentUiState().registerSubscriptionForm.vlan.toIntOrNull() ?: return
        val requestKey = UUID.randomUUID().toString()
        registrationRequestKeyInFlight = requestKey
        registrationSerialInFlight = selectedOnu.sn
        registerSubscriptionJob?.cancel()
        registerSubscriptionJob = viewModelScope.launch(mainImmediate) {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isPreauthorizationRequestInProgress = true,
                    preauthorizationError = null,
                )
            }
            try {
                val operation = useCase.start(
                    StartOnuRegistrationRequest(
                        requestKey = requestKey,
                        serial = selectedOnu.sn,
                        target = OnuRegistrationTarget(
                            oltId = selectedOnu.olt_id,
                            ponType = selectedOnu.pon_type,
                            board = selectedOnu.board,
                            port = selectedOnu.port,
                            onuType = selectedOnu.onu_type_name,
                            vlan = vlan,
                        ),
                    ),
                )
                showPreauthorizationOperation(awaitPreauthorizationSettled(useCase, operation), restoreDraft = false)
            } catch (cancelled: CancellationException) {
                _uiState.update {
                    it.copy(isLoading = false, isPreauthorizationRequestInProgress = false)
                }
                throw cancelled
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isPreauthorizationRequestInProgress = false,
                        preauthorizationError = error.message ?: "No se pudo autorizar la ONU",
                    )
                }
                _uiEvent.emit(RegisterSubscriptionUiEvent.Error(error.message ?: "No se pudo autorizar la ONU"))
            } finally {
                if (cancellationRequested) {
                    viewModelScope.launch(mainImmediate) {
                        yield()
                        startDeferredCancellation()
                    }
                }
            }
        }
    }

    private suspend fun awaitPreauthorizationSettled(
        useCase: OnuRegistrationOperationUseCase,
        initial: OnuRegistrationOperation,
    ): OnuRegistrationOperation {
        var latest = initial
        repeat(PREAUTH_MAX_POLLS) {
            if (!latest.isAuthorizationInFlight()) return latest
            _uiState.update { it.copy(preauthorizationOperation = latest) }
            delay(PREAUTH_POLL_INTERVAL_MS)
            latest = useCase.get(latest.id)
        }
        return latest
    }

    private fun OnuRegistrationOperation.isAuthorizationInFlight(): Boolean =
        phase == "OLT_AUTHORIZATION" && state in setOf("PENDING", "RUNNING")

    private fun retryOnuRegistration() {
        val useCase = onuRegistrationOperationUseCase ?: return
        val operation = currentUiState().preauthorizationOperation ?: return
        if (!operation.canManuallyRetry() || currentUiState().isLoading) return
        registerSubscriptionJob?.cancel()
        registerSubscriptionJob = viewModelScope.launch(mainImmediate) {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isPreauthorizationRequestInProgress = true,
                    preauthorizationError = null,
                )
            }
            try {
                showPreauthorizationOperation(useCase.retry(operation.id, operation.revision), restoreDraft = true)
            } catch (cancelled: CancellationException) {
                _uiState.update {
                    it.copy(isLoading = false, isPreauthorizationRequestInProgress = false)
                }
                throw cancelled
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isPreauthorizationRequestInProgress = false,
                        preauthorizationError = error.message ?: "No se pudo consultar ACS",
                    )
                }
            }
        }
    }

    private fun onLeaveDuringRegistration() {
        val current = currentUiState()
        observabilityClient.reportLog(
            message = "$OBS_FEATURE.register_abandoned_during_submit",
            severity = "warning",
            tags = mapOf(
                "feature" to OBS_FEATURE,
                "screen" to OBS_SCREEN,
                "operationId" to current.preauthorizationOperation?.id,
                "submission" to current.submission::class.simpleName,
            ),
        )
    }

    private fun cancelOnuRegistration() {
        val current = currentUiState()
        if (current.registrationCancelled || current.cancellationInProgress) return
        _uiState.update { it.copy(showCancelConfirmation = true) }
    }

    private fun confirmCancelOnuRegistration() {
        val current = currentUiState()
        if (current.registrationCancelled || current.cancellationInProgress) return
        _uiState.update { it.copy(showCancelConfirmation = false, registrationCancelled = false) }
        resumeProvisioningJob?.cancel()
        cancellationRequested = true
        _uiState.update { it.copy(cancellationInProgress = true) }
        if (subscriptionSubmissionInFlight) {
            viewModelScope.launch(mainImmediate) {
                try {
                    persistCancellationIntent(current.preauthorizationOperation)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    showCancellationError(error.message ?: "No se pudo guardar la cancelación pendiente.")
                }
            }
            return
        }
        startDeferredCancellation()
    }

    private fun startDeferredCancellation() {
        if (!cancellationRequested || cancellationJob?.isActive == true) return
        cancellationJob = viewModelScope.launch(mainImmediate) { performRegistrationCancellation() }
    }

    private suspend fun performRegistrationCancellation() {
        val useCase = onuRegistrationOperationUseCase
        if (useCase == null) {
            cancellationRequested = false
            completeRegistrationCancellation()
            return
        }
        val knownOperation = currentUiState().preauthorizationOperation
        try {
            val intent = persistCancellationIntent(knownOperation)
            cancellationRequested = false
            showCancellationInProgress()
            awaitCancellationDependencies()
            val currentOperation = loadCancellationOperation(useCase, knownOperation)
            if (currentOperation == null) {
                if (intent != null) {
                    val active = useCase.active()
                    if (active != null && matchesCancellationIntent(intent, active)) {
                        _uiState.update { it.copy(preauthorizationOperation = active) }
                        pollCancellationUntilComplete(useCase, requestOperationCancellation(useCase, active))
                    } else if (active == null) {
                        completeRegistrationCancellation()
                        if (!_uiState.value.registrationCancelled) {
                            showCancellationError("La operación aún no aparece en el servidor. La intención se conservará para volver a intentarlo.")
                        }
                    } else {
                        showCancellationError("La operación activa no coincide con la ONU seleccionada. La intención se conservará sin cancelar otra operación.")
                    }
                } else if (!_uiState.value.registrationCancelled) {
                    completeRegistrationCancellation()
                }
                return
            }
            val persistedIntent = pendingCancellationIntent ?: intent
            if (persistedIntent != null && !matchesCancellationIntent(persistedIntent, currentOperation)) {
                showCancellationError("La operación activa no coincide con la ONU seleccionada. La intención se conservará sin cancelar otra operación.")
                return
            }
            if (persistedIntent != null && currentOperation.id != persistedIntent.operationId) {
                pendingCancellationIntent = persistedIntent.copy(operationId = currentOperation.id)
                cancellationIntentStore?.save(pendingCancellationIntent!!)
            }
            val requested = requestOperationCancellation(useCase, currentOperation)
            _uiState.update { it.copy(preauthorizationOperation = requested) }
            pollCancellationUntilComplete(useCase, requested)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            showCancellationError(error.message ?: "No se pudo completar la cancelación. Pulsa cancelar para reintentar.")
        }
    }

    private suspend fun awaitCancellationDependencies() {
        val currentJob = kotlinx.coroutines.currentCoroutineContext()[Job]
        listOf(loadScreenJob, registerSubscriptionJob, photoUploadJob)
            .filterNotNull()
            .distinct()
            .forEach { job -> if (job !== currentJob) job.join() }
    }

    private suspend fun persistCancellationIntent(
        operation: OnuRegistrationOperation?,
    ): OnuRegistrationCancellationIntent? {
        val current = currentUiState()
        val operatorId = current.currentUser?.id?.toLong() ?: operation?.operatorId
        val serial = operation?.serial ?: registrationSerialInFlight
            ?: current.registerSubscriptionForm.selectedOnu?.sn
        val requestKey = operation?.registrationRequestKey
            ?: registrationRequestKeyInFlight
            ?: operation?.id?.let { "operation:$it" }
        if (operatorId == null || serial.isNullOrBlank() || requestKey.isNullOrBlank()) {
            return pendingCancellationIntent
        }
        val existing = pendingCancellationIntent?.takeIf {
            it.operatorId == operatorId && it.requestKey == requestKey && it.serial.equals(serial, true)
        } ?: cancellationIntentStore?.get(operatorId)?.takeIf {
            it.requestKey == requestKey && it.serial.equals(serial, true)
        }
        val intent = OnuRegistrationCancellationIntent(
            operatorId = operatorId,
            requestKey = requestKey,
            serial = serial,
            operationId = operation?.id ?: existing?.operationId,
        )
        cancellationIntentStore?.save(intent)
        pendingCancellationIntent = intent
        return intent
    }

    private fun matchesCancellationIntent(
        intent: OnuRegistrationCancellationIntent,
        operation: OnuRegistrationOperation,
    ): Boolean {
        if (!intent.serial.equals(operation.serial, ignoreCase = true)) return false
        return when {
            intent.operationId != null -> intent.operationId == operation.id
            intent.requestKey.startsWith("operation:") -> intent.requestKey == "operation:${operation.id}"
            else -> intent.requestKey == operation.registrationRequestKey
        }
    }

    private fun showCancellationInProgress() {
        _uiState.update {
            it.copy(
                isLoading = true,
                isRegistering = true,
                cancellationInProgress = true,
                preauthorizationError = null,
                registrationProgressMessage = "Cancelando y limpiando la provisión…",
            )
        }
    }

    private fun showCancellationError(message: String) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isRegistering = false,
                cancellationInProgress = false,
                preauthorizationError = message,
            )
        }
    }

    private suspend fun loadCancellationOperation(
        useCase: OnuRegistrationOperationUseCase,
        known: OnuRegistrationOperation?,
    ): OnuRegistrationOperation? = try {
        known?.let { useCase.get(it.id) }
            ?: pendingCancellationIntent?.operationId?.let { operationId ->
                runCatching { useCase.get(operationId) }.getOrNull()
            }
            ?: useCase.active()
    } catch (error: Exception) {
        if (known?.state == "CANCELLED" && known.subscriptionId != null) {
            val cleanup = useCase.cleanupCancelled(known.id)
            if (cleanup.status == "COMPLETE") {
                completeRegistrationCancellation(known)
                null
            } else {
                throw IllegalStateException(cleanup.message ?: "La limpieza continúa pendiente", error)
            }
        } else {
            throw error
        }
    }

    private suspend fun requestOperationCancellation(
        useCase: OnuRegistrationOperationUseCase,
        operation: OnuRegistrationOperation,
    ): OnuRegistrationOperation = if (operation.state in setOf("CANCEL_REQUESTED", "CANCELLING", "CANCELLED")) {
        operation
    } else {
        useCase.cancel(operation.id, operation.revision)
    }

    private suspend fun pollCancellationUntilComplete(
        useCase: OnuRegistrationOperationUseCase,
        initial: OnuRegistrationOperation,
    ) {
        var latest = initial
        repeat(40) {
            if (finishCancellationIfTerminal(useCase, latest)) return
            delay(750)
            latest = useCase.get(latest.id)
            _uiState.update { it.copy(preauthorizationOperation = latest) }
        }
        _uiState.update {
            it.copy(
                isLoading = false,
                isRegistering = latest.state in setOf("CANCEL_REQUESTED", "CANCELLING"),
                cancellationInProgress = false,
                preauthorizationOperation = latest,
                preauthorizationError = "La limpieza sigue en curso. Pulsa cancelar para volver a consultar.",
            )
        }
    }

    private suspend fun finishCancellationIfTerminal(
        useCase: OnuRegistrationOperationUseCase,
        operation: OnuRegistrationOperation,
    ): Boolean = when (operation.state) {
        "CANCELLED" -> {
            finishCancelledOperation(useCase, operation)
            true
        }
        "CANCEL_FAILED" -> {
            showCancellationFailure(operation)
            true
        }
        else -> false
    }

    private suspend fun finishCancelledOperation(
        useCase: OnuRegistrationOperationUseCase,
        operation: OnuRegistrationOperation,
    ) {
        if (operation.subscriptionId != null && !hardCleanupCompleted(useCase, operation)) return
        completeRegistrationCancellation(operation)
    }

    private suspend fun hardCleanupCompleted(
        useCase: OnuRegistrationOperationUseCase,
        operation: OnuRegistrationOperation,
    ): Boolean {
        val cleanup = useCase.cleanupCancelled(operation.id)
        if (cleanup.status == "COMPLETE") return true
        _uiState.update {
            it.copy(
                isLoading = false,
                isRegistering = false,
                cancellationInProgress = false,
                preauthorizationOperation = operation,
                preauthorizationError = cleanup.message ?: "La limpieza quedó incompleta. Pulsa cancelar para reintentar.",
            )
        }
        return false
    }

    private fun showCancellationFailure(operation: OnuRegistrationOperation) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isRegistering = false,
                cancellationInProgress = false,
                preauthorizationOperation = operation,
                preauthorizationError = preauthorizationFailure(operation),
            )
        }
    }

    private suspend fun completeRegistrationCancellation(
        operation: OnuRegistrationOperation? = currentUiState().preauthorizationOperation,
    ) {
        val serial = operation?.serial ?: pendingCancellationIntent?.serial
            ?: currentUiState().registerSubscriptionForm.selectedOnu?.sn
        if (serial.isNullOrBlank()) {
            markRegistrationCancellationComplete()
            return
        }
        val availableOnus = getAvailableOnuListUseCase().getOrElse { error ->
            showCancellationError(
                "La limpieza terminó, pero no se pudo actualizar la lista de ONUs. Pulsa actualizar para reintentar. ${error.message.orEmpty()}".trim()
            )
            return
        }
        completeCancellationWithAvailableOnus(operation, serial, availableOnus)
    }

    private suspend fun completeCancellationWithAvailableOnus(
        operation: OnuRegistrationOperation?,
        serial: String,
        availableOnus: List<Onu>,
    ) {
        val onuIsAvailable = availableOnus.any { it.sn.equals(serial, ignoreCase = true) }
        if (!onuIsAvailable) {
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    isRegistering = false,
                    cancellationInProgress = false,
                    isRefreshingOnuList = false,
                    registrationCancelled = false,
                    preauthorizationOperation = operation ?: current.preauthorizationOperation,
                    preauthorizationError = "La limpieza está confirmada, pero la ONU todavía no aparece disponible. Actualiza la lista para volver a registrarla.",
                    wizardStep = RegisterSubscriptionWizardStep.ONU_SELECTION,
                    registerSubscriptionForm = current.registerSubscriptionForm.copy(
                        onuList = availableOnus,
                        selectedOnu = null,
                    ),
                )
            }
            return
        }

        val intent = pendingCancellationIntent
        if (intent != null) {
            cancellationIntentStore?.clear(intent.operatorId, intent.requestKey)
        }
        pendingCancellationIntent = null
        registrationRequestKeyInFlight = null
        registrationSerialInFlight = null
        markRegistrationCancellationComplete(availableOnus)
    }

    private fun markRegistrationCancellationComplete(availableOnus: List<Onu>? = null) {
        clearStoredOnuSelection()
        finishRegistrationWorkflow(
            status = WorkflowStatus.INTERRUPTED,
            subscriptionId = currentUiState().preauthorizationOperation?.subscriptionId,
            reason = "cancelled",
        )
        _uiState.update { current ->
            current.copy(
                isLoading = false,
                isRegistering = false,
                cancellationInProgress = false,
                isRefreshingOnuList = false,
                registrationCancelled = true,
                preauthorizationOperation = null,
                preauthorizationError = null,
                registrationProgressMessage = "",
                wizardStep = RegisterSubscriptionWizardStep.ONU_SELECTION,
                registerSubscriptionForm = clearRegistrationForm(current.registerSubscriptionForm).copy(
                    onuList = availableOnus ?: current.registerSubscriptionForm.onuList,
                ),
            )
        }
    }

    private suspend fun restoreActivePreauthorization() {
        val useCase = onuRegistrationOperationUseCase ?: return
        try {
            val operatorId = currentUiState().currentUser?.id?.toLong()
            pendingCancellationIntent = operatorId?.let { cancellationIntentStore?.get(it) }
            val intent = pendingCancellationIntent
            val operationById = intent?.operationId?.let { operationId ->
                runCatching { useCase.get(operationId) }.getOrNull()
            }
            val operation = operationById ?: useCase.active()
            if (operation == null) {
                if (intent != null) {
                    completeRegistrationCancellation()
                    if (_uiState.value.registrationCancelled) return
                    _uiState.update {
                        it.copy(
                            cancellationInProgress = false,
                            preauthorizationError = "La cancelación sigue pendiente de sincronizar con el servidor. Actualiza la lista o vuelve a intentarlo.",
                        )
                    }
                }
                return
            }
            if (intent != null) {
                if (!matchesCancellationIntent(intent, operation)) {
                    showPreauthorizationOperation(operation, restoreDraft = true)
                    _uiState.update {
                        it.copy(preauthorizationError = "Hay una cancelación pendiente para ${intent.serial}; no se canceló la operación activa porque su identidad no coincide.")
                    }
                    return
                }
                _uiState.update {
                    it.copy(
                        preauthorizationEnabled = true,
                        preauthorizationOperation = operation,
                        isRegistering = true,
                        cancellationInProgress = true,
                        registrationProgressMessage = "Retomando la cancelación de ${intent.serial}…",
                    )
                }
                cancellationRequested = true
                return
            }
            val linkedSubscriptionId = operation.subscriptionId
            if (linkedSubscriptionId != null) {
                val cancelling = operation.state in setOf("CANCEL_REQUESTED", "CANCELLING", "CANCEL_FAILED", "CANCELLED")
                _uiState.update {
                    it.copy(
                        preauthorizationEnabled = true,
                        preauthorizationOperation = operation,
                        isRegistering = true,
                        submission = if (cancelling) it.submission else SubmissionState.Provisioning(linkedSubscriptionId),
                        registrationProgressMessage = if (cancelling) {
                            "Retomando limpieza de la cancelación…"
                        } else "Retomando provisión de la suscripción #$linkedSubscriptionId…",
                    )
                }
                ensureRegistrationWorkflow(operation.id)
                if (cancelling) {
                    cancellationRequested = true
                } else {
                    resumeProvisioning(linkedSubscriptionId)
                }
                return
            }
            if (operation.state in setOf("CANCEL_REQUESTED", "CANCELLING", "CANCEL_FAILED", "CANCELLED")) {
                _uiState.update {
                    it.copy(
                        preauthorizationEnabled = true,
                        preauthorizationOperation = operation,
                        isRegistering = true,
                        cancellationInProgress = true,
                        registrationProgressMessage = "Retomando limpieza de la cancelación…",
                    )
                }
                cancellationRequested = true
                return
            }
            showPreauthorizationOperation(operation, restoreDraft = true)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            _uiState.update { it.copy(preauthorizationError = error.message ?: "No se pudo recuperar el registro pendiente") }
        }
    }

    private suspend fun showPreauthorizationOperation(operation: OnuRegistrationOperation, restoreDraft: Boolean) {
        val useCase = onuRegistrationOperationUseCase ?: return
        ensureRegistrationWorkflow(operation.id)
        val selectedOnu = _uiState.value.registerSubscriptionForm.onuList.firstOrNull { it.sn.equals(operation.serial, true) }
        var form = _uiState.value.registerSubscriptionForm.copy(
            selectedOnu = selectedOnu ?: _uiState.value.registerSubscriptionForm.selectedOnu?.takeIf { it.sn.equals(operation.serial, true) }
        )
        var restoredWizardStep: RegisterSubscriptionWizardStep? = null
        if (operation.canOpenRegistrationForm() && restoreDraft) {
            useCase.draft(operation.id)?.let { draft ->
                form = restoreRegistrationDraft(form, draft, operation.serial)
                restoredWizardStep = draft.registrationWizardStep()
            }
        }
        val authorizedOnu = form.selectedOnu?.takeIf { it.sn.equals(operation.serial, true) }
            ?: operation.toAuthorizedOnuOrNull()
        if (authorizedOnu != null) {
            form = form.copy(
                selectedOnu = authorizedOnu,
                onuList = form.onuList.filterNot { it.sn.equals(authorizedOnu.sn, true) } + authorizedOnu,
            )
        }
        _uiState.update { current ->
            current.copy(
                isLoading = false,
                isPreauthorizationRequestInProgress = false,
                preauthorizationEnabled = true,
                preauthorizationOperation = operation,
                preauthorizationError = preauthorizationFailure(operation),
                wizardStep = when {
                    !operation.canOpenRegistrationForm() -> RegisterSubscriptionWizardStep.WAITING_FOR_ACS
                    else -> restoredWizardStep ?: RegisterSubscriptionWizardStep.CLIENT_LOCATION
                },
                registerSubscriptionForm = form,
            )
        }
        clearStoredOnuSelection()
    }

    private fun restorePreauthorizationSelection() {
        val current = currentUiState()
        val operatorId = current.currentUser?.id?.toLong() ?: return
        val serial = onuRegistrationSelectionStore?.getSelectedOnuSerial(operatorId) ?: return
        val selectedOnu = current.registerSubscriptionForm.onuList.firstOrNull {
            it.sn.equals(serial, ignoreCase = true)
        } ?: return
        _uiState.update { state ->
            if (state.preauthorizationOperation != null) state
            else state.copy(
                registerSubscriptionForm = state.registerSubscriptionForm.copy(
                    selectedOnu = selectedOnu,
                    onuError = null,
                )
            )
        }
    }

    private fun clearStoredOnuSelection() {
        val operatorId = currentUiState().currentUser?.id?.toLong() ?: return
        onuRegistrationSelectionStore?.clearSelectedOnuSerial(operatorId)
    }

    private fun preauthorizationFailure(operation: OnuRegistrationOperation): String? {
        val failure = operation.operationFailure ?: operation.checkpoints.firstNotNullOfOrNull { it.failure }
        return failure?.let { "${it.code}: ${it.message}" + (it.technicalDetails?.let { details -> " · $details" } ?: "") }
    }

    private fun clearRegistrationForm(form: RegisterSubscriptionFormState): RegisterSubscriptionFormState =
        RegisterSubscriptionFormState(
            accessMode = E2eAccessModeResolver.resolve(),
            planList = form.planList,
            selectedPlan = getAutoSelectedPlan(form.planList, null),
            placeList = form.placeList,
            coreDeviceList = form.coreDeviceList,
            selectedHostDevice = form.activeCoreDevices().singleOrNull(),
            napBoxList = form.napBoxList,
            onuList = form.onuList,
        )

    private fun buildRegistrationDraft(state: RegisterSubscriptionState): Map<String, Any?> {
        val form = state.registerSubscriptionForm
        val onu = form.selectedOnu
        val location = form.location
        return mapOf(
            "firstName" to form.firstName.removeSpecialCharacters(), "lastName" to form.lastName.removeSpecialCharacters(),
            "dni" to form.dni, "address" to form.address, "phone" to form.phone,
            "subscriptionDate" to form.subscriptionDate.takeIf { it > 0 }, "planId" to form.selectedPlan?.id,
            "placeId" to form.selectedPlace?.id,
            "location" to location?.let { mapOf("latitude" to it.latitude, "longitude" to it.longitude) },
            "technicianId" to state.currentUser?.id, "hostDeviceId" to form.selectedHostDevice?.id,
            "napBoxId" to form.selectedNapBox?.id,
            "onu" to onu?.let { mapOf("olt_id" to it.olt_id, "pon_type" to it.pon_type, "board" to it.board,
                "port" to it.port, "onu" to it.onu, "onu_type_id" to it.onu_type_id,
                "onu_type_name" to it.onu_type_name, "sn" to it.sn) },
            "installationType" to form.installationType.name, "note" to form.note,
            "facadePhotoUrl" to form.facadePhotoUrl, "vlan" to form.vlan,
            "wifiSsid24" to form.wifiSsid24, "wifiPassword24" to form.wifiPassword24,
            "wifiSsid5" to form.resolvedWifiSsid5(),
            "wifiPassword5" to form.resolvedWifiPassword5(),
            "equipmentCondition" to form.equipmentCondition.name,
            "useDifferentWifiNames" to form.useDifferentWifiNames,
            "tvCpeKind" to form.tvCpeKind?.name,
            "coupon" to form.coupon,
            "clientIpAddress" to form.clientIpAddress,
            "accessMode" to form.accessMode.name, "wizardStep" to state.wizardStep.name,
            "registrationOperationId" to state.preauthorizationOperation?.id,
        ).filterValues { it != null }
    }

    private fun Map<String, Any?>.registrationWizardStep(): RegisterSubscriptionWizardStep? {
        val restoredStep = this["wizardStep"]?.toString()?.let { value ->
            runCatching { RegisterSubscriptionWizardStep.valueOf(value) }.getOrNull()
        }
        return restoredStep?.takeUnless { it.isPreauthorizationStep() }
    }

    private fun restoreRegistrationDraft(
        current: RegisterSubscriptionFormState,
        draft: Map<String, Any?>,
        operationSerial: String,
    ): RegisterSubscriptionFormState {
        val location = draft["location"] as? Map<*, *>
        val onuData = draft["onu"] as? Map<*, *>
        val restoredOnu = current.onuList.firstOrNull { it.sn.equals(operationSerial, true) }
            ?: current.selectedOnu?.takeIf { it.sn.equals(operationSerial, true) }
            ?: onuData?.toOnuOrNull(operationSerial)
        val restoredOnuList = if (restoredOnu != null && current.onuList.none { it.sn.equals(restoredOnu.sn, true) }) {
            current.onuList + restoredOnu
        } else {
            current.onuList
        }
        val photoUrl = draft["facadePhotoUrl"]?.toString()?.takeIf(String::isNotBlank)
        val planId = draft["planId"]?.toString()
        val placeId = draft["placeId"]?.toString()
        val napBoxId = draft["napBoxId"]?.toString()
        val hostId = draft["hostDeviceId"]?.toString()?.toIntOrNull()
        val installationType = draft["installationType"]?.toString()?.let { value ->
            runCatching { InstallationType.valueOf(value) }.getOrNull()
        }
        val accessMode = draft["accessMode"]?.toString()?.let { value ->
            runCatching { AccessMode.valueOf(value) }.getOrNull()
        }
        val condition = draft["equipmentCondition"]?.toString()?.let { value ->
            runCatching { EquipmentCondition.valueOf(value) }.getOrNull()
        }
        val tvCpeKind = draft["tvCpeKind"]?.toString()?.let { value ->
            runCatching { TvCpeKind.valueOf(value) }.getOrNull()
        }
        return current.copy(
            firstName = draft.text("firstName"), lastName = draft.text("lastName"), dni = draft.text("dni"),
            address = draft.text("address"), phone = draft.text("phone"), note = draft.text("note"),
            subscriptionDate = draft["subscriptionDate"].asLongOrNull() ?: current.subscriptionDate,
            selectedPlan = current.planList.firstOrNull { it.id == planId } ?: current.selectedPlan,
            selectedPlace = current.placeList.firstOrNull { it.id == placeId || it.id.toString() == placeId } ?: current.selectedPlace,
            selectedNapBox = current.napBoxList.firstOrNull { it.id == napBoxId || it.id.toString() == napBoxId } ?: current.selectedNapBox,
            selectedHostDevice = current.coreDeviceList.firstOrNull { it.id == hostId } ?: current.selectedHostDevice,
            onuList = restoredOnuList,
            selectedOnu = restoredOnu,
            location = if (location != null) LatLng(location["latitude"].asDoubleOrNull() ?: 0.0, location["longitude"].asDoubleOrNull() ?: 0.0) else current.location,
            installationType = installationType ?: current.installationType,
            accessMode = accessMode ?: current.accessMode,
            equipmentCondition = condition ?: current.equipmentCondition,
            useDifferentWifiNames = draft["useDifferentWifiNames"] as? Boolean ?: current.useDifferentWifiNames,
            tvCpeKind = tvCpeKind ?: current.tvCpeKind,
            coupon = draft.text("coupon"), clientIpAddress = draft.text("clientIpAddress"),
            vlan = draft.text("vlan").ifBlank { current.vlan },
            wifiSsid24 = draft.text("wifiSsid24"), wifiPassword24 = draft.text("wifiPassword24"),
            wifiSsid5 = draft.text("wifiSsid5"), wifiPassword5 = draft.text("wifiPassword5"),
            facadePhotoUrl = photoUrl,
        )
    }

    private fun Map<String, Any?>.text(key: String): String = this[key]?.toString().orEmpty()
    private fun OnuRegistrationOperation.toAuthorizedOnuOrNull(): Onu? {
        if (!canOpenRegistrationForm()) return null
        val target = onuTarget ?: return null
        val evidence = oltEvidence ?: return null
        if (evidence.ontId < 0) return null
        return Onu(
            board = target.board,
            olt_id = target.oltId,
            onu = evidence.ontId.toString(),
            onu_type_id = "",
            onu_type_name = target.onuType,
            pon_type = target.ponType,
            port = target.port,
            sn = serial,
        )
    }
    private fun Map<*, *>.toOnuOrNull(expectedSerial: String): Onu? {
        val values = listOf("board", "olt_id", "onu", "onu_type_id", "onu_type_name", "pon_type", "port", "sn")
        if (values.any { this[it]?.toString().isNullOrBlank() }) return null
        val restoredOnu = Onu(
            board = this["board"].toString(),
            olt_id = this["olt_id"].toString(),
            onu = this["onu"].toString(),
            onu_type_id = this["onu_type_id"].toString(),
            onu_type_name = this["onu_type_name"].toString(),
            pon_type = this["pon_type"].toString(),
            port = this["port"].toString(),
            sn = this["sn"].toString(),
        )
        return restoredOnu.takeIf { it.sn.equals(expectedSerial, true) }
    }

    private fun Any?.asLongOrNull(): Long? = when (this) { is Number -> toLong(); else -> this?.toString()?.toLongOrNull() }
    private fun Any?.asDoubleOrNull(): Double? = when (this) { is Number -> toDouble(); else -> this?.toString()?.toDoubleOrNull() }

    private fun onWizardBackClicked() {
        _uiState.update { current ->
            val previous = current.wizardStep.previous() ?: return@update current
            current.copy(wizardStep = previous)
        }
    }

    fun retryTr069Provisioning(subscription: Subscription) {
        val subscriptionId = subscription.resolvedSubscriptionId() ?: return
        if (_uiState.value.tr069RetryLoading) return
        retryTr069Job?.cancel()
        retryTr069Job = viewModelScope.launch(mainImmediate) {
            try {
                _uiState.update {
                    it.copy(
                        tr069RetryLoading = true,
                        registrationProgressMessage = "Reintentando aprovisionamiento TR-069…",
                    )
                }
                retryTr069ProvisioningUseCase(subscriptionId).fold(
                    onSuccess = { updated ->
                        val merged = updated.copy(
                            wifiSsid24 = updated.wifiSsid24 ?: subscription.wifiSsid24,
                            wifiSsid5 = updated.wifiSsid5 ?: subscription.wifiSsid5,
                            wifiPassword24 = subscription.wifiPassword24 ?: updated.wifiPassword24,
                            wifiPassword5 = subscription.wifiPassword5 ?: updated.wifiPassword5,
                        )
                        when (merged.tr069ProvisionStatus) {
                            "COMPLETE" -> {
                                _uiState.update {
                                    it.copy(tr069RetryLoading = false, submission = SubmissionState.Completed(merged))
                                }
                                _uiEvent.emit(RegisterSubscriptionUiEvent.Success(merged))
                            }
                            "MANUAL_REQUIRED", "FAILED" -> {
                                _uiState.update {
                                    it.copy(
                                        tr069RetryLoading = false,
                                        submission = SubmissionState.NeedsAttention(
                                            merged,
                                            merged.tr069Message ?: "No se pudo completar el aprovisionamiento TR-069",
                                        ),
                                    )
                                }
                                _uiEvent.emit(RegisterSubscriptionUiEvent.Success(merged))
                                _uiEvent.emit(
                                    RegisterSubscriptionUiEvent.Error(
                                        merged.tr069Message
                                            ?: "No se pudo completar el aprovisionamiento TR-069"
                                    )
                                )
                            }
                            else -> {
                                _uiState.update {
                                    it.copy(
                                        registrationProgressMessage = "Reintentando aprovisionamiento TR-069…"
                                    )
                                }
                                pollRegistrationProgress(
                                    subscriptionId = subscriptionId,
                                    initial = merged,
                                    fromRetry = true,
                                )
                            }
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
                val refreshedOnuList = getAvailableOnuListUseCase().getOrThrow()
                val cancellationIntent = pendingCancellationIntent
                if (cancellationIntent != null && refreshedOnuList.any {
                        it.sn.equals(cancellationIntent.serial, ignoreCase = true)
                    }) {
                    completeCancellationWithAvailableOnus(
                        operation = currentUiState().preauthorizationOperation,
                        serial = cancellationIntent.serial,
                        availableOnus = refreshedOnuList,
                    )
                } else {
                    _uiState.update { current ->
                        val currentForm = current.registerSubscriptionForm
                        val preserveWizardSelection = current.wizardStep != RegisterSubscriptionWizardStep.ONU_SELECTION
                        val selectedOnu = currentForm.selectedOnu?.takeIf { selected ->
                            refreshedOnuList.any { it.sn.equals(selected.sn, true) } ||
                                preserveWizardSelection ||
                                (current.preauthorizationOperation?.canOpenRegistrationForm() == true &&
                                    selected.sn.equals(current.preauthorizationOperation.serial, true))
                        }
                        val validOnuList = if (selectedOnu != null &&
                            refreshedOnuList.none { it.sn.equals(selectedOnu.sn, true) }) {
                            refreshedOnuList + selectedOnu
                        } else refreshedOnuList
                        current.copy(
                            isRefreshingOnuList = false,
                            registerSubscriptionForm = currentForm.copy(
                                onuList = validOnuList,
                                selectedOnu = selectedOnu,
                                onuError = subscriptionOnuErrorAfterListRefresh(
                                    requiresOnu = currentForm.requiresOnu(),
                                    previousSelected = currentForm.selectedOnu,
                                    newSelected = selectedOnu,
                                    newList = validOnuList,
                                    previousFieldError = currentForm.onuError
                                )
                            )
                        )
                    }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
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
            } finally {
                if (_uiState.value.isRefreshingOnuList) {
                    _uiState.update { it.copy(isRefreshingOnuList = false) }
                }
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
    scheduleDniCheck(value.trim())
}

private fun scheduleDniCheck(dni: String) {
    dniCheckJob?.cancel()
    val repository = subscriptionActionsRepository
    if (repository == null || dni.length < DNI_CHECK_MIN_LENGTH) {
        _uiState.update { it.copy(dniWarning = null) }
        return
    }
    dniCheckJob = viewModelScope.launch(mainImmediate) {
        delay(DNI_CHECK_DEBOUNCE_MS)
        val check = try {
            repository.checkDni(dni)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return@launch
        }
        val warning = check.activeSubscriptions.takeIf { it > 0 }?.let { active ->
            "Este DNI ya tiene $active suscripción(es) activa(s). Verifica que no sea un alta duplicada."
        }
        _uiState.update { it.copy(dniWarning = warning) }
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
    val state = currentUiState()
    val operatorId = state.currentUser?.id?.toLong()
    if (state.preauthorizationEnabled && operatorId != null) {
        onuRegistrationSelectionStore?.saveSelectedOnuSerial(operatorId, value.sn)
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
                    isOfflineMode = offline && !current.preauthorizationEnabled,
                    registerSubscriptionForm = current.registerSubscriptionForm.copy(
                        requiresClientIpAddress = offline && !current.preauthorizationEnabled
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

private fun onAccessModeSelected(mode: AccessMode) {
    _uiState.update { current ->
        current.copy(
            registerSubscriptionForm = current.registerSubscriptionForm.copy(accessMode = mode)
        )
    }
}

private fun onInstallationTypeSelected(type: InstallationType) {
    val current = currentUiState()
    val currentForm = current.registerSubscriptionForm
    if (currentForm.installationType == type) return

    val filteredPlans = getFilteredPlansForInstallationType(type)

    if (filteredPlans.isEmpty()) return

    val currentSelectedPlan = currentForm.selectedPlan
    val selectedPlan = getAutoSelectedPlan(filteredPlans, currentSelectedPlan)
    val preauthorizedOnu = current.preauthorizationOperation?.serial
        ?.let { serial -> currentForm.onuList.firstOrNull { it.sn.equals(serial, ignoreCase = true) } }

    _uiState.update {
        it.copy(
            registerSubscriptionForm = it.registerSubscriptionForm.copy(
                installationType = type,
                planList = filteredPlans,
                selectedPlan = selectedPlan,
                selectedOnu = preauthorizedOnu,
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
    val hasFacadePhoto = if (uiState.value.preauthorizationOperation != null) {
        !form.facadePhotoUrl.isNullOrBlank()
    } else form.facadePhotoUri != null || facadePhotoFile != null
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
                    isRegistering = true,
                    submission = SubmissionState.Submitting,
                    registrationProgressMessage = "Registrando y autorizando…"
                )
            }

            subscriptionSubmissionInFlight = true
            val submission = try {
                submitAndTrackRegistrationUseCase.submit(subscription, orderIdSnapshot, facadePhotoFile) {
                    _uiState.update {
                        it.copy(
                            submission = SubmissionState.Reconciling,
                            registrationProgressMessage = "Verificando si el registro quedó guardado…",
                        )
                    }
                }
            } finally {
                subscriptionSubmissionInFlight = false
            }
            if (cancellationRequested) {
                performRegistrationCancellation()
                return@launch
            }

            when (submission) {
                is RegistrationSubmission.Accepted -> {
                    observabilityClient.addBreadcrumb(
                        category = ObsBreadcrumbCategory.STATE,
                        message = if (submission.reconciled) "$OBS_FEATURE.register_reconciled" else "$OBS_FEATURE.register_success",
                        data = mapOf(
                            "feature" to OBS_FEATURE,
                            "orderId" to orderIdSnapshot,
                            "subscriptionId" to submission.subscription.resolvedSubscriptionId(),
                            "operationId" to subscription.registrationOperationId,
                        )
                    )
                    val enriched = submission.subscription.copy(
                        wifiSsid24 = submission.subscription.wifiSsid24 ?: subscription.wifiSsid24,
                        wifiSsid5 = submission.subscription.wifiSsid5 ?: subscription.wifiSsid5,
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
                                isRegistering = false,
                                orderId = null,
                                submission = SubmissionState.Completed(enriched),
                                registrationProgressMessage = "Registrando…"
                            )
                        }
                        resetFormClientRequestId()
                        _uiEvent.emit(RegisterSubscriptionUiEvent.Success(enriched))
                    }
                }
                is RegistrationSubmission.Queued -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRegistering = false,
                            submission = SubmissionState.Idle,
                            orderId = null
                        )
                    }
                    resetFormClientRequestId()
                    observabilityClient.addBreadcrumb(
                        category = ObsBreadcrumbCategory.STATE,
                        message = "$OBS_FEATURE.register_queued_offline",
                        data = mapOf("feature" to OBS_FEATURE, "orderId" to orderIdSnapshot)
                    )
                    _uiEvent.emit(RegisterSubscriptionUiEvent.QueuedOffline)
                }
                is RegistrationSubmission.Uncertain -> markRegistrationUncertain(subscription.registrationOperationId)
                is RegistrationSubmission.Rejected -> {
                    val error = submission.error
                    _uiState.update {
                        it.copy(isLoading = false, isRegistering = false, submission = SubmissionState.Idle)
                    }
                    observabilityClient.reportError(
                        throwable = error,
                        message = "Fallo al registrar suscripción",
                        tags = mapOf(
                            "feature" to OBS_FEATURE,
                            "screen" to OBS_SCREEN,
                            "action" to "save_subscription",
                            "entityId" to orderIdSnapshot,
                            "operationId" to subscription.registrationOperationId,
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
            }
        } catch (e: CancellationException) {
            _uiState.update { it.copy(isLoading = false, isRegistering = false) }
            throw e
        } catch (error: Exception) {
            if (cancellationRequested) {
                performRegistrationCancellation()
            } else {
                _uiState.update { it.copy(isLoading = false, isRegistering = false) }
                _uiEvent.emit(RegisterSubscriptionUiEvent.Error(error.message ?: "Error al registrar la suscripción"))
            }
        }
    }
}

private suspend fun pollRegistrationProgress(
    subscriptionId: Int,
    initial: Subscription,
    fromRetry: Boolean = false,
) {
    _uiState.update {
        it.copy(
            isLoading = !fromRetry,
            isRegistering = !fromRetry,
            submission = if (fromRetry) it.submission else SubmissionState.Provisioning(subscriptionId),
            tr069RetryLoading = fromRetry || it.tr069RetryLoading,
            registrationProgressMessage = when {
                fromRetry -> "Reintentando aprovisionamiento TR-069…"
                !initial.tr069Message.isNullOrBlank() -> initial.tr069Message!!
                else -> "Esperando ACS…"
            }
        )
    }
    val result = pollRegistrationProgressUseCase(subscriptionId) { progress ->
        _uiState.update {
            it.copy(
                registrationProgressMessage = progress.message,
                registrationProgressCheckpoints = progress.provisioningCheckpoints,
            )
        }
    }
    if (_uiState.value.cancellationInProgress) return
    result.fold(
        onSuccess = { progress ->
            val finalSubscription = progress.toSubscriptionOrNull(registration = initial) ?: initial.copy(
                tr069ProvisionStatus = progress.tr069ProvisionStatus ?: initial.tr069ProvisionStatus,
                tr069Message = progress.tr069Message ?: initial.tr069Message,
                mikrotikProvisionStatus = progress.mikrotikProvisionStatus
                    ?: initial.mikrotikProvisionStatus,
                oltProvisionStatus = progress.oltProvisionStatus ?: initial.oltProvisionStatus,
                provisioningPending = !progress.done,
            )
            val attention = provisioningAttentionMessage(progress, finalSubscription)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRegistering = false,
                    tr069RetryLoading = false,
                    orderId = null,
                    submission = if (attention != null) {
                        SubmissionState.NeedsAttention(finalSubscription, attention)
                    } else SubmissionState.Completed(finalSubscription),
                    registrationProgressMessage = "Registrando…"
                )
            }
            resetFormClientRequestId()
            finishRegistrationWorkflow(
                status = if (attention != null) WorkflowStatus.FAILED else WorkflowStatus.SUCCESS,
                subscriptionId = subscriptionId,
                reason = progress.outcome,
            )
            _uiEvent.emit(RegisterSubscriptionUiEvent.Success(finalSubscription))
            if (attention != null) {
                _uiEvent.emit(RegisterSubscriptionUiEvent.Error(attention))
            }
        },
        onFailure = {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRegistering = false,
                    tr069RetryLoading = false,
                    orderId = null,
                    submission = SubmissionState.ProvisioningUnknown(
                        initial,
                        "La suscripción #$subscriptionId quedó registrada, pero no pudimos confirmar el final del aprovisionamiento. Revisa el progreso.",
                    ),
                    registrationProgressMessage = "Registrando…"
                )
            }
            resetFormClientRequestId()
            _uiEvent.emit(RegisterSubscriptionUiEvent.Success(initial))
        }
    )
}

private fun provisioningAttentionMessage(progress: RegistrationProgress, subscription: Subscription): String? = when {
    progress.outcome == "FAILED" -> progress.message.ifBlank { "El aprovisionamiento falló." }
    subscription.tr069ProvisionStatus == "MANUAL_REQUIRED" || subscription.tr069ProvisionStatus == "FAILED" ->
        subscription.tr069Message ?: "No se pudo completar el aprovisionamiento TR-069"
    else -> null
}

private suspend fun markRegistrationUncertain(operationId: String?) {
    val message = "No se pudo confirmar si el registro quedó guardado. Puedes reintentar: no se duplicará."
    _uiState.update {
        it.copy(isLoading = false, isRegistering = false, submission = SubmissionState.Uncertain(message))
    }
    observabilityClient.reportLog(
        message = "$OBS_FEATURE.register_result_uncertain",
        severity = "warning",
        tags = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "operationId" to operationId),
    )
    _uiEvent.emit(RegisterSubscriptionUiEvent.Error(message))
}

private fun ensureRegistrationWorkflow(operationId: String?) {
    if (operationId.isNullOrBlank() || registrationWorkflowId == operationId) return
    registrationWorkflowId = observabilityClient.startWorkflow(
        name = OBS_WORKFLOW_NAME,
        category = OBS_WORKFLOW_CATEGORY,
        context = mapOf("feature" to OBS_FEATURE),
        workflowId = operationId,
    )
}

private fun finishRegistrationWorkflow(status: WorkflowStatus, subscriptionId: Int?, reason: String? = null) {
    if (registrationWorkflowId == null) return
    registrationWorkflowId = null
    observabilityClient.endWorkflow(status, reason, mapOf("subscriptionId" to subscriptionId))
}

private fun formClientRequestId(): String =
    savedStateHandle.get<String>(CLIENT_REQUEST_ID_KEY)
        ?: UUID.randomUUID().toString().also { savedStateHandle[CLIENT_REQUEST_ID_KEY] = it }

private fun resetFormClientRequestId() {
    savedStateHandle.remove<String>(CLIENT_REQUEST_ID_KEY)
}

private fun resumeProvisioning(subscriptionId: Int) {
    resumeProvisioningJob?.cancel()
    resumeProvisioningJob = viewModelScope.launch(mainImmediate) {
        pollRegistrationProgress(subscriptionId, Subscription(subscriptionId = subscriptionId))
    }
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
        facadePhotoUrl = form.facadePhotoUrl,
        clientIpAddress = form.clientIpAddress.trim().takeIf { it.isNotEmpty() },
        ip = form.clientIpAddress.trim().takeIf { it.isNotEmpty() },
        vlan = form.vlan.takeIf { form.requiresOnu() },
        wifiSsid24 = form.wifiSsid24.trim().takeIf { form.requiresWifiConfig() },
        wifiPassword24 = form.wifiPassword24.takeIf { form.requiresWifiConfig() },
        wifiSsid5 = form.resolvedWifiSsid5().takeIf { form.requiresWifiConfig() },
        wifiPassword5 = form.resolvedWifiPassword5().takeIf { form.requiresWifiConfig() },
        accessMode = form.accessMode.name,
        registrationOperationId = currentUiState().preauthorizationOperation?.id,
        clientRequestId = currentUiState().preauthorizationOperation?.id ?: formClientRequestId(),
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
