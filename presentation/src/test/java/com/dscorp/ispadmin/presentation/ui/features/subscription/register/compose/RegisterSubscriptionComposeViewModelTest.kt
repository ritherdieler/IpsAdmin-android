package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import com.dscorp.ispadmin.domain.model.AccessMode
import com.dscorp.ispadmin.domain.model.CatalogCoreDevice
import com.dscorp.ispadmin.domain.model.CatalogNapBox
import com.dscorp.ispadmin.domain.model.CatalogOnu
import com.dscorp.ispadmin.domain.model.CatalogPlan
import com.dscorp.ispadmin.domain.model.InstallationOrder
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.NetworkDevice
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.OnuRegistrationOperation
import com.dscorp.ispadmin.domain.model.OnuRegistrationCleanupReport
import com.dscorp.ispadmin.domain.model.OnuRegistrationCancellationIntent
import com.dscorp.ispadmin.domain.model.PendingSubscription
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.RegistrationCatalog
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.usecase.InstallationOrderUseCase
import com.dscorp.ispadmin.domain.usecase.catalog.GetRegistrationCatalogUseCase
import com.dscorp.ispadmin.domain.usecase.catalog.RefreshRegistrationCatalogUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetAvailableOnuListUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetNearNapBoxesUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetPlaceFromLocationUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetUserSessionUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.OnuRegistrationOperationUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.ObserveOfflineRegistrationModeUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.RegisterSubscriptionResult
import com.dscorp.ispadmin.domain.usecase.subscription.RegisterSubscriptionUseCase
import com.dscorp.ispadmin.domain.model.RegistrationProgress
import com.dscorp.ispadmin.domain.usecase.subscription.PollRegistrationProgressUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.RetryTr069ProvisioningUseCase
import com.dscorp.ispadmin.domain.repository.OnuRegistrationCancellationIntentStore
import com.dscorp.ispadmin.domain.repository.OnuRegistrationSelectionStore
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.E2eAccessModeResolver
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.LocationCaptureMethod
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.FormFieldKey
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionIntent
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionUiEvent
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.TvCpeKind
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.SubmissionState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterSubscriptionComposeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getAvailableOnuListUseCase: GetAvailableOnuListUseCase
    private lateinit var getRegistrationCatalogUseCase: GetRegistrationCatalogUseCase
    private lateinit var refreshRegistrationCatalogUseCase: RefreshRegistrationCatalogUseCase
    private lateinit var getPlaceFromLocationUseCase: GetPlaceFromLocationUseCase
    private lateinit var registerSubscriptionUseCase: RegisterSubscriptionUseCase
    private lateinit var getUserSessionUseCase: GetUserSessionUseCase
    private lateinit var getNearNapBoxesUseCase: GetNearNapBoxesUseCase
    private lateinit var installationOrderUseCase: InstallationOrderUseCase
    private lateinit var observeOfflineRegistrationModeUseCase: ObserveOfflineRegistrationModeUseCase
    private lateinit var retryTr069ProvisioningUseCase: RetryTr069ProvisioningUseCase
    private lateinit var pollRegistrationProgressUseCase: PollRegistrationProgressUseCase
    private val offlineModeFlow = MutableStateFlow(false)

    private lateinit var viewModel: RegisterSubscriptionComposeViewModel

    private val sampleUser = User(id = 1, name = "T", lastName = "U")
    private val samplePlan = PlanResponse(
        id = "p1",
        name = "Plan",
        price = 10.0,
        downloadSpeed = "100",
        uploadSpeed = "100",
        type = InstallationType.FIBER
    )
    private val sampleCoreDevice = NetworkDevice(id = 10, name = "Core-A", disabled = false)
    private val secondCoreDevice = NetworkDevice(id = 11, name = "Core-B", disabled = false)

    private lateinit var facadePhotoFile: File

    private class InMemoryCancellationIntentStore : OnuRegistrationCancellationIntentStore {
        private val intents = mutableMapOf<Long, OnuRegistrationCancellationIntent>()

        override suspend fun get(operatorId: Long): OnuRegistrationCancellationIntent? = intents[operatorId]

        override suspend fun save(intent: OnuRegistrationCancellationIntent) {
            intents[intent.operatorId] = intent
        }

        override suspend fun clear(operatorId: Long, requestKey: String) {
            if (intents[operatorId]?.requestKey == requestKey) intents.remove(operatorId)
        }
    }

    private class InMemoryOnuRegistrationSelectionStore : OnuRegistrationSelectionStore {
        private val selectedOnus = mutableMapOf<Long, String>()

        override fun getSelectedOnuSerial(operatorId: Long): String? = selectedOnus[operatorId]

        override fun saveSelectedOnuSerial(operatorId: Long, serial: String) {
            selectedOnus[operatorId] = serial
        }

        override fun clearSelectedOnuSerial(operatorId: Long) {
            selectedOnus.remove(operatorId)
        }
    }

    private fun sampleCatalog(
        plans: List<CatalogPlan> = listOf(
            CatalogPlan(
                id = "p1",
                name = "Plan",
                price = 10.0,
                downloadSpeed = "100",
                uploadSpeed = "100",
                type = "FIBER"
            )
        ),
        napBoxes: List<CatalogNapBox> = emptyList(),
        onus: List<CatalogOnu> = emptyList(),
        coreDevices: List<CatalogCoreDevice> = listOf(
            CatalogCoreDevice(id = 10, name = "Core-A", disabled = false)
        )
    ) = RegistrationCatalog(
        plans = plans,
        places = emptyList(),
        napBoxes = napBoxes,
        onus = onus,
        coreDevices = coreDevices
    )

    private fun fiberNap() = CatalogNapBox(id = "n1", placeName = "P1", placeId = 1)

    private fun fiberOnu() = CatalogOnu(
        sn = "sn1",
        board = "b",
        oltId = "olt",
        onu = "1",
        onuTypeId = "t",
        onuTypeName = "type",
        ponType = "pon",
        port = "p"
    )

    @Before
    fun setup() {
        E2eAccessModeResolver.override = null
        Dispatchers.setMain(testDispatcher)
        facadePhotoFile = File.createTempFile("facade_test", ".jpg")
        getAvailableOnuListUseCase = mockk()
        getRegistrationCatalogUseCase = mockk()
        refreshRegistrationCatalogUseCase = mockk()
        getPlaceFromLocationUseCase = mockk()
        registerSubscriptionUseCase = mockk()
        getUserSessionUseCase = mockk()
        getNearNapBoxesUseCase = mockk()
        installationOrderUseCase = mockk(relaxed = true)
        observeOfflineRegistrationModeUseCase = mockk()
        retryTr069ProvisioningUseCase = mockk()
        pollRegistrationProgressUseCase = mockk()
        coEvery { pollRegistrationProgressUseCase(any(), any()) } coAnswers { kotlinx.coroutines.awaitCancellation() }
        every { observeOfflineRegistrationModeUseCase() } returns Result.success(offlineModeFlow)

        coEvery { getAvailableOnuListUseCase() } returns Result.success(emptyList())
        coEvery { refreshRegistrationCatalogUseCase() } returns Result.success(Unit)
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(sampleCatalog())
        coEvery { getUserSessionUseCase() } returns Result.success(sampleUser)

        viewModel = RegisterSubscriptionComposeViewModel(
            getAvailableOnuListUseCase = getAvailableOnuListUseCase,
            getRegistrationCatalogUseCase = getRegistrationCatalogUseCase,
            refreshRegistrationCatalogUseCase = refreshRegistrationCatalogUseCase,
            getPlaceFromLocationUseCase = getPlaceFromLocationUseCase,
            registerSubscriptionUseCase = registerSubscriptionUseCase,
            getUserSessionUseCase = getUserSessionUseCase,
            getNearNapBoxesUseCase = getNearNapBoxesUseCase,
            installationOrderUseCase = installationOrderUseCase,
            observeOfflineRegistrationModeUseCase = observeOfflineRegistrationModeUseCase,
            retryTr069ProvisioningUseCase = retryTr069ProvisioningUseCase,
            pollRegistrationProgressUseCase = pollRegistrationProgressUseCase,
            observabilityClient = mockk(relaxed = true),
            mainImmediate = testDispatcher
        )
    }

    private fun viewModelWithPreauthorization(
        useCase: OnuRegistrationOperationUseCase,
        cancellationIntentStore: OnuRegistrationCancellationIntentStore? = null,
        onuRegistrationSelectionStore: OnuRegistrationSelectionStore? = null,
        savedStateHandle: androidx.lifecycle.SavedStateHandle = androidx.lifecycle.SavedStateHandle(),
        observabilityClient: com.dscorp.ispadmin.observability.ObservabilityClient = mockk(relaxed = true),
    ) = RegisterSubscriptionComposeViewModel(
        getAvailableOnuListUseCase = getAvailableOnuListUseCase,
        getRegistrationCatalogUseCase = getRegistrationCatalogUseCase,
        refreshRegistrationCatalogUseCase = refreshRegistrationCatalogUseCase,
        getPlaceFromLocationUseCase = getPlaceFromLocationUseCase,
        registerSubscriptionUseCase = registerSubscriptionUseCase,
        getUserSessionUseCase = getUserSessionUseCase,
        getNearNapBoxesUseCase = getNearNapBoxesUseCase,
        installationOrderUseCase = installationOrderUseCase,
        observeOfflineRegistrationModeUseCase = observeOfflineRegistrationModeUseCase,
        retryTr069ProvisioningUseCase = retryTr069ProvisioningUseCase,
        pollRegistrationProgressUseCase = pollRegistrationProgressUseCase,
        observabilityClient = observabilityClient,
        mainImmediate = testDispatcher,
        onuRegistrationOperationUseCase = useCase,
        cancellationIntentStore = cancellationIntentStore,
        onuRegistrationSelectionStore = onuRegistrationSelectionStore,
        savedStateHandle = savedStateHandle,
    )

    @After
    fun tearDown() {
        E2eAccessModeResolver.override = null
        Dispatchers.resetMain()
    }

    @Test
    fun `loadScreenData applies catalog and clears loading`() = runTest(testDispatcher) {
        val events = mutableListOf<RegisterSubscriptionUiEvent>()
        val job = launch {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.loadScreenData(null)
        advanceUntilIdle()

        assertTrue(events.isEmpty())
        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals(false, viewModel.uiState.value.isRegistering)
        assertEquals(sampleUser, viewModel.uiState.value.currentUser)
        assertEquals(samplePlan, viewModel.uiState.value.registerSubscriptionForm.selectedPlan)
        coVerify(exactly = 1) { refreshRegistrationCatalogUseCase() }
        coVerify(exactly = 1) { getRegistrationCatalogUseCase() }

        job.cancel()
    }

    @Test
    fun `reopening ONU selection restores selected ONU before authorization starts`() = runTest(testDispatcher) {
        val serial = "VSOL0031C0B6"
        val onu = Onu("b", "olt", "1", "t", "VSOLVA74", "pon", "p", serial)
        val selectionStore = InMemoryOnuRegistrationSelectionStore()
        val operationUseCase = mockk<OnuRegistrationOperationUseCase>()
        coEvery { operationUseCase.active() } returns null
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(onus = listOf(fiberOnu().copy(sn = serial)))
        )
        viewModel = viewModelWithPreauthorization(
            useCase = operationUseCase,
            onuRegistrationSelectionStore = selectionStore,
        )

        viewModel.loadScreenData(null)
        advanceUntilIdle()
        viewModel.onIntent(RegisterSubscriptionIntent.OnuSelected(onu))

        viewModel = viewModelWithPreauthorization(
            useCase = operationUseCase,
            onuRegistrationSelectionStore = selectionStore,
        )
        viewModel.loadScreenData(null)
        advanceUntilIdle()

        assertEquals(
            com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep.ONU_SELECTION,
            viewModel.uiState.value.wizardStep,
        )
        assertEquals(serial, viewModel.uiState.value.registerSubscriptionForm.selectedOnu?.sn)
    }

    @Test
    fun `selected ONU is authorized directly and exposes only ACS retry or cancel`() = runTest(testDispatcher) {
        val onu = Onu("1", "olt-lab", "3", "type-1", "VSOLVA74", "gpon", "2", "VSOL0031C0B6")
        val operationUseCase = mockk<OnuRegistrationOperationUseCase>()
        val waiting = OnuRegistrationOperation(
            id = "preauth-1", serial = onu.sn, subscriptionId = null,
            phase = "WAITING_FOR_ACS", state = "WAITING", revision = 2,
        )
        coEvery { operationUseCase.active() } returns null
        coEvery { operationUseCase.start(any()) } returns waiting
        coEvery { operationUseCase.saveDraft(any(), any()) } returns Unit
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(onus = listOf(fiberOnu().copy(sn = onu.sn, oltId = onu.olt_id, board = onu.board, port = onu.port, ponType = onu.pon_type, onuTypeName = onu.onu_type_name)))
        )
        viewModel = viewModelWithPreauthorization(operationUseCase)

        viewModel.loadScreenData(null)
        advanceUntilIdle()
        assertEquals(
            com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep.ONU_SELECTION,
            viewModel.uiState.value.wizardStep,
        )
        viewModel.onIntent(RegisterSubscriptionIntent.OnuSelected(onu))
        viewModel.onIntent(RegisterSubscriptionIntent.WizardContinueClicked)
        advanceUntilIdle()

        assertEquals(
            com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep.WAITING_FOR_ACS,
            viewModel.uiState.value.wizardStep,
        )
        assertEquals(waiting, viewModel.uiState.value.preauthorizationOperation)
        coVerify(exactly = 1) {
            operationUseCase.start(match { it.serial == onu.sn && it.target.oltId == onu.olt_id && it.target.board == onu.board && it.target.port == onu.port })
        }
        coVerify(exactly = 0) { registerSubscriptionUseCase(any(), any(), any()) }
    }

    @Test
    fun `preauthorization loading is set only while the ONU request is in flight`() = runTest(testDispatcher) {
        val onu = Onu("1", "olt-lab", "3", "type-1", "VSOLVA74", "gpon", "2", "VSOL0031C0B6")
        val operationUseCase = mockk<OnuRegistrationOperationUseCase>()
        val requestStarted = CompletableDeferred<Unit>()
        val finishRequest = CompletableDeferred<OnuRegistrationOperation>()
        val waiting = OnuRegistrationOperation(
            id = "preauth-loading",
            serial = onu.sn,
            subscriptionId = null,
            phase = "WAITING_FOR_ACS",
            state = "WAITING",
            revision = 1,
        )
        coEvery { operationUseCase.active() } returns null
        coEvery { operationUseCase.start(any()) } coAnswers {
            requestStarted.complete(Unit)
            finishRequest.await()
        }
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(onus = listOf(fiberOnu().copy(sn = onu.sn)))
        )
        viewModel = viewModelWithPreauthorization(operationUseCase)

        viewModel.loadScreenData(null)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isPreauthorizationRequestInProgress)

        viewModel.onIntent(RegisterSubscriptionIntent.OnuSelected(onu))
        viewModel.onIntent(RegisterSubscriptionIntent.WizardContinueClicked)
        testScheduler.runCurrent()

        assertTrue(requestStarted.isCompleted)
        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isPreauthorizationRequestInProgress)

        finishRequest.complete(waiting)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isPreauthorizationRequestInProgress)
    }

    @Test
    fun `retrying preauthorization keeps its request loading state until ACS responds`() = runTest(testDispatcher) {
        val operationUseCase = mockk<OnuRegistrationOperationUseCase>()
        val activeOperation = OnuRegistrationOperation(
            id = "preauth-retry-loading",
            serial = "VSOL0031C0B6",
            subscriptionId = null,
            phase = "WAITING_FOR_ACS",
            state = "FAILED",
            revision = 2,
        )
        val retriedOperation = activeOperation.copy(state = "WAITING", revision = 3)
        val requestStarted = CompletableDeferred<Unit>()
        val finishRequest = CompletableDeferred<OnuRegistrationOperation>()
        coEvery { operationUseCase.active() } returns activeOperation
        coEvery { operationUseCase.retry(activeOperation.id, activeOperation.revision) } coAnswers {
            requestStarted.complete(Unit)
            finishRequest.await()
        }
        viewModel = viewModelWithPreauthorization(operationUseCase)

        viewModel.loadScreenData(null)
        advanceUntilIdle()
        assertEquals(activeOperation, viewModel.uiState.value.preauthorizationOperation)
        assertFalse(viewModel.uiState.value.isPreauthorizationRequestInProgress)

        viewModel.onIntent(RegisterSubscriptionIntent.RetryOnuRegistration)
        testScheduler.runCurrent()

        assertTrue(requestStarted.isCompleted)
        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isPreauthorizationRequestInProgress)

        finishRequest.complete(retriedOperation)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isPreauthorizationRequestInProgress)
        assertEquals(retriedOperation, viewModel.uiState.value.preauthorizationOperation)
    }

    @Test
    fun `refreshing ONU list does not clear selection after preauthorization wizard advances`() = runTest(testDispatcher) {
        val onu = Onu("b", "olt", "1", "t", "VSOLVA74", "pon", "p", "VSOL0031C0B6")
        val operationUseCase = mockk<OnuRegistrationOperationUseCase>()
        coEvery { operationUseCase.start(any()) } returns OnuRegistrationOperation(
            id = "preauth-refresh", serial = onu.sn, subscriptionId = null,
            phase = "WAITING_FOR_ACS", state = "WAITING", revision = 1,
        )
        coEvery { operationUseCase.active() } returns null
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(onus = listOf(fiberOnu().copy(sn = onu.sn, onuTypeName = onu.onu_type_name)))
        )
        coEvery { getAvailableOnuListUseCase() } returns Result.success(emptyList())
        viewModel = viewModelWithPreauthorization(operationUseCase)

        viewModel.loadScreenData(null)
        advanceUntilIdle()
        viewModel.onIntent(RegisterSubscriptionIntent.OnuSelected(onu))
        viewModel.onIntent(RegisterSubscriptionIntent.WizardContinueClicked)
        advanceUntilIdle()
        assertEquals(
            com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep.WAITING_FOR_ACS,
            viewModel.uiState.value.wizardStep,
        )

        viewModel.refreshOnuList()
        advanceUntilIdle()

        assertEquals(onu, viewModel.uiState.value.registerSubscriptionForm.selectedOnu)
        assertEquals(onu, viewModel.uiState.value.registerSubscriptionForm.onuList.single())
    }

    @Test
    fun `opening registration restores ready preauthorization and encrypted form draft`() = runTest(testDispatcher) {
        val serial = "VSOL0031C0B6"
        val operationUseCase = mockk<OnuRegistrationOperationUseCase>()
        val ready = OnuRegistrationOperation(
            id = "preauth-ready-1", serial = serial, subscriptionId = null,
            phase = "READY_FOR_FORM", state = "READY_FOR_FORM", revision = 8,
        )
        coEvery { operationUseCase.active() } returns ready
        coEvery { operationUseCase.get(ready.id) } returns ready
        coEvery { operationUseCase.draft(ready.id) } returns mapOf(
            "firstName" to "CLIENTE RESTAURADO", "dni" to "12345678", "planId" to "p1",
            "facadePhotoUrl" to "https://storage.example/photo.jpg", "wizardStep" to "INSTALLATION",
        )
        coEvery { operationUseCase.saveDraft(any(), any()) } returns Unit
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(onus = listOf(fiberOnu().copy(sn = serial)))
        )
        viewModel = viewModelWithPreauthorization(operationUseCase)

        viewModel.loadScreenData(null)
        advanceUntilIdle()

        coVerify(exactly = 1) { operationUseCase.active() }
        assertNull(viewModel.uiState.value.preauthorizationError)
        assertEquals(
            com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep.INSTALLATION,
            viewModel.uiState.value.wizardStep,
        )
        assertEquals(ready.id, viewModel.uiState.value.preauthorizationOperation?.id)
        assertEquals("CLIENTE RESTAURADO", viewModel.uiState.value.registerSubscriptionForm.firstName)
        assertEquals("https://storage.example/photo.jpg", viewModel.uiState.value.registerSubscriptionForm.facadePhotoUrl)
        assertNull(viewModel.uiState.value.registerSubscriptionForm.validate(FormFieldKey.FACADE_PHOTO))
        coVerify(exactly = 1) { operationUseCase.draft(ready.id) }
    }

    @Test
    fun `saving registration draft preserves wizard step and separate wifi credentials`() = runTest(testDispatcher) {
        val onu = Onu("b", "olt", "1", "t", "VSOLVA74", "pon", "p", "VSOL0031C0B6")
        val operationUseCase = mockk<OnuRegistrationOperationUseCase>()
        val ready = OnuRegistrationOperation(
            id = "preauth-draft-step", serial = onu.sn, subscriptionId = null,
            phase = "READY_FOR_FORM", state = "READY_FOR_FORM", revision = 1,
        )
        coEvery { operationUseCase.active() } returns ready
        coEvery { operationUseCase.draft(ready.id) } returns null
        coEvery { operationUseCase.saveDraft(any(), any()) } returns Unit
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(sampleCatalog())
        viewModel = viewModelWithPreauthorization(operationUseCase)

        viewModel.loadScreenData(null)
        advanceUntilIdle()
        viewModel.onIntent(RegisterSubscriptionIntent.FirstNameChanged("Ana"))
        viewModel.onIntent(RegisterSubscriptionIntent.LastNameChanged("Perez"))
        viewModel.onIntent(RegisterSubscriptionIntent.DniChanged("12345678"))
        viewModel.onIntent(RegisterSubscriptionIntent.AddressChanged("Calle 123"))
        viewModel.onIntent(RegisterSubscriptionIntent.PhoneChanged("987654321"))
        viewModel.onIntent(RegisterSubscriptionIntent.PlaceSelected(Place(id = "1", name = "P1")))
        viewModel.onLocationChanged(com.google.android.gms.maps.model.LatLng(-12.0, -77.0))
        viewModel.onIntent(RegisterSubscriptionIntent.WizardContinueClicked)
        advanceUntilIdle()
        assertEquals(
            com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep.INSTALLATION,
            viewModel.uiState.value.wizardStep,
        )
        viewModel.onIntent(RegisterSubscriptionIntent.WifiPassword24Changed("clave-24"))
        viewModel.onIntent(RegisterSubscriptionIntent.WifiPassword5Changed("clave-5"))

        advanceTimeBy(2_001)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            operationUseCase.saveDraft(ready.id, match { draft ->
                draft["wizardStep"] == "INSTALLATION" &&
                    draft["wifiPassword24"] == "clave-24" &&
                    draft["wifiPassword5"] == "clave-5"
            })
        }
    }

    @Test
    fun `step 5 keeps the preauthorized ONU when the current installation type is selected`() = runTest(testDispatcher) {
        val onu = Onu("b", "olt", "1", "t", "VSOLVA74", "pon", "p", "VSOL0031C0B6")
        val operationUseCase = mockk<OnuRegistrationOperationUseCase>()
        val ready = OnuRegistrationOperation(
            id = "preauth-ready-step-5", serial = onu.sn, subscriptionId = null,
            phase = "READY_FOR_FORM", state = "READY_FOR_FORM", revision = 1,
        )
        coEvery { operationUseCase.active() } returns ready
        coEvery { operationUseCase.draft(ready.id) } returns null
        coEvery { operationUseCase.saveDraft(any(), any()) } returns Unit
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(napBoxes = listOf(fiberNap()), onus = listOf(fiberOnu().copy(sn = onu.sn)))
        )
        viewModel = viewModelWithPreauthorization(operationUseCase)

        viewModel.loadScreenData(null)
        advanceUntilIdle()
        viewModel.onIntent(RegisterSubscriptionIntent.FirstNameChanged("Juan"))
        viewModel.onIntent(RegisterSubscriptionIntent.LastNameChanged("Perez"))
        viewModel.onIntent(RegisterSubscriptionIntent.DniChanged("12345678"))
        viewModel.onIntent(RegisterSubscriptionIntent.AddressChanged("Calle larga 12345"))
        viewModel.onIntent(RegisterSubscriptionIntent.PhoneChanged("987654321"))
        viewModel.onIntent(RegisterSubscriptionIntent.PlaceSelected(Place(id = "1", name = "P1")))
        viewModel.onLocationChanged(com.google.android.gms.maps.model.LatLng(-12.0, -77.0))
        viewModel.onIntent(RegisterSubscriptionIntent.WizardContinueClicked)
        advanceUntilIdle()

        assertEquals(
            com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep.INSTALLATION,
            viewModel.uiState.value.wizardStep,
        )
        viewModel.onIntent(RegisterSubscriptionIntent.WifiSsid24Changed("CasaFibra"))
        viewModel.onIntent(RegisterSubscriptionIntent.WifiPassword24Changed("clave123"))
        assertTrue(
            com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.canAdvanceWizardStep(
                viewModel.uiState.value.wizardStep,
                viewModel.uiState.value.registerSubscriptionForm,
            )
        )

        viewModel.onIntent(RegisterSubscriptionIntent.InstallationTypeSelected(InstallationType.FIBER))

        val state = viewModel.uiState.value
        assertEquals(onu.sn, state.registerSubscriptionForm.selectedOnu?.sn)
        assertTrue(
            com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.canAdvanceWizardStep(
                state.wizardStep,
                state.registerSubscriptionForm,
            )
        )
    }

    @Test
    fun `restoring a preauthorized registration recovers its ONU from the saved draft`() = runTest(testDispatcher) {
        val onu = Onu("b", "olt", "1", "t", "VSOLVA74", "pon", "p", "VSOL0031C0B6")
        val operationUseCase = mockk<OnuRegistrationOperationUseCase>()
        val ready = OnuRegistrationOperation(
            id = "preauth-ready-onu-draft", serial = onu.sn, subscriptionId = null,
            phase = "READY_FOR_FORM", state = "READY_FOR_FORM", revision = 2,
        )
        coEvery { operationUseCase.active() } returns ready
        coEvery { operationUseCase.get(ready.id) } returns ready
        coEvery { operationUseCase.draft(ready.id) } returns mapOf(
            "onu" to mapOf(
                "olt_id" to onu.olt_id,
                "pon_type" to onu.pon_type,
                "board" to onu.board,
                "port" to onu.port,
                "onu" to onu.onu,
                "onu_type_id" to onu.onu_type_id,
                "onu_type_name" to onu.onu_type_name,
                "sn" to onu.sn,
            )
        )
        coEvery { operationUseCase.saveDraft(any(), any()) } returns Unit
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(sampleCatalog())
        viewModel = viewModelWithPreauthorization(operationUseCase)

        viewModel.loadScreenData(null)
        advanceUntilIdle()

        val restoredForm = viewModel.uiState.value.registerSubscriptionForm
        assertEquals(onu, restoredForm.selectedOnu)
        assertTrue(restoredForm.onuList.contains(onu))
        assertNull(restoredForm.validate(FormFieldKey.ONU))
    }

    @Test
    fun `restoring an authorized ONU uses operation target when catalog and draft are empty`() = runTest(testDispatcher) {
        val operationUseCase = mockk<OnuRegistrationOperationUseCase>()
        val ready = com.google.gson.Gson().fromJson(
            """{"id":"preauth-target","serial":"VSOL0031C0B6","subscriptionId":null,"phase":"READY_FOR_FORM","state":"READY_FOR_FORM","revision":5,"checkpoints":[],"onuTarget":{"oltId":"olt-lab","ponType":"gpon","board":"3","port":"2","onuType":"VSOLVA74","vlan":100},"oltEvidence":{"externalId":"olt-ont","board":3,"port":2,"ontId":12}}""",
            OnuRegistrationOperation::class.java,
        )
        assertEquals("olt-lab", ready.onuTarget?.oltId)
        assertEquals(12, ready.oltEvidence?.ontId)
        coEvery { operationUseCase.active() } returns ready
        coEvery { operationUseCase.draft(ready.id) } returns null
        coEvery { operationUseCase.saveDraft(any(), any()) } returns Unit
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(sampleCatalog(onus = emptyList()))
        viewModel = viewModelWithPreauthorization(operationUseCase)

        viewModel.loadScreenData(null)
        advanceUntilIdle()

        val form = viewModel.uiState.value.registerSubscriptionForm
        assertNull(viewModel.uiState.value.preauthorizationError)
        assertEquals(ready.id, viewModel.uiState.value.preauthorizationOperation?.id)
        assertEquals(ready.serial, form.selectedOnu?.sn)
        assertEquals("12", form.selectedOnu?.onu)
        assertNull(form.validate(FormFieldKey.ONU))

        viewModel.refreshOnuList()
        advanceUntilIdle()
        assertEquals(ready.serial, viewModel.uiState.value.registerSubscriptionForm.selectedOnu?.sn)
        assertNull(viewModel.uiState.value.registerSubscriptionForm.validate(FormFieldKey.ONU))
    }

    @Test
    fun `cancelling ready preauthorization clears sensitive form data and photo references`() = runTest(testDispatcher) {
        val operationUseCase = mockk<OnuRegistrationOperationUseCase>()
        val ready = OnuRegistrationOperation(
            id = "preauth-cancel-1", serial = "VSOL0031C0B6", subscriptionId = null,
            phase = "READY_FOR_FORM", state = "READY_FOR_FORM", revision = 4,
        )
        coEvery { operationUseCase.active() } returns ready
        coEvery { operationUseCase.get(ready.id) } returns ready
        coEvery { operationUseCase.draft(ready.id) } returns mapOf(
            "firstName" to "BORRAR", "wifiPassword24" to "secreto123",
            "facadePhotoUrl" to "https://storage.example/photo.jpg",
        )
        coEvery { operationUseCase.saveDraft(any(), any()) } returns Unit
        coEvery { operationUseCase.cancel(ready.id, ready.revision) } returns ready.copy(
            state = "CANCELLED", revision = ready.revision + 1,
        )
        coEvery { getAvailableOnuListUseCase() } returns Result.success(
            listOf(Onu("1", "olt-lab", "3", "type-1", "type", "gpon", "2", ready.serial))
        )
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(onus = listOf(fiberOnu().copy(sn = ready.serial)))
        )
        viewModel = viewModelWithPreauthorization(operationUseCase)

        viewModel.loadScreenData(null)
        advanceUntilIdle()
        viewModel.onIntent(RegisterSubscriptionIntent.CancelOnuRegistration)
        coVerify(exactly = 0) { operationUseCase.cancel(any(), any()) }
        viewModel.onIntent(RegisterSubscriptionIntent.ConfirmCancelOnuRegistration)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.preauthorizationOperation)
        assertEquals("", state.registerSubscriptionForm.firstName)
        assertEquals("", state.registerSubscriptionForm.wifiPassword24)
        assertNull(state.registerSubscriptionForm.facadePhotoUrl)
        assertNull(state.registerSubscriptionForm.facadePhotoUri)
        assertNull(state.registerSubscriptionForm.selectedOnu)
        assertEquals(
            com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep.ONU_SELECTION,
            state.wizardStep,
        )
        assertTrue(state.registerSubscriptionForm.onuList.any { it.sn == ready.serial })
        coVerify(exactly = 1) { operationUseCase.cancel(ready.id, ready.revision) }
    }

    @Test
    fun `dismissing cancellation confirmation keeps the registration active`() = runTest(testDispatcher) {
        viewModel.onIntent(RegisterSubscriptionIntent.CancelOnuRegistration)
        assertTrue(viewModel.uiState.value.showCancelConfirmation)

        viewModel.onIntent(RegisterSubscriptionIntent.DismissCancelOnuRegistration)

        assertFalse(viewModel.uiState.value.showCancelConfirmation)
        assertFalse(viewModel.uiState.value.cancellationInProgress)
        assertFalse(viewModel.uiState.value.registrationCancelled)
    }

    @Test
    fun `duplicate cancellation confirmation does not start a second cleanup`() = runTest(testDispatcher) {
        val operationUseCase = mockk<OnuRegistrationOperationUseCase>()
        val linked = OnuRegistrationOperation(
            id = "duplicate-cancel-flow",
            serial = "VSOL0031C0B6",
            subscriptionId = 84,
            phase = "PROVISIONING",
            state = "RUNNING",
            revision = 5,
        )
        val requested = linked.copy(state = "CANCEL_REQUESTED", revision = 6)
        val cancelled = linked.copy(state = "CANCELLED", revision = 7)
        coEvery { operationUseCase.active() } returns linked
        coEvery { operationUseCase.get(linked.id) } returnsMany listOf(linked, cancelled)
        coEvery { operationUseCase.cancel(linked.id, linked.revision) } returns requested
        coEvery { operationUseCase.cleanupCancelled(linked.id) } returns OnuRegistrationCleanupReport("COMPLETE")
        coEvery { getAvailableOnuListUseCase() } returns Result.success(
            listOf(Onu("1", "olt-lab", "3", "type-1", "type", "gpon", "2", linked.serial))
        )
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(sampleCatalog())
        viewModel = viewModelWithPreauthorization(operationUseCase)
        viewModel.loadScreenData(null)
        advanceUntilIdle()

        viewModel.onIntent(RegisterSubscriptionIntent.CancelOnuRegistration)
        viewModel.onIntent(RegisterSubscriptionIntent.ConfirmCancelOnuRegistration)
        viewModel.onIntent(RegisterSubscriptionIntent.ConfirmCancelOnuRegistration)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.registrationCancelled)
        coVerify(exactly = 1) { operationUseCase.cancel(linked.id, linked.revision) }
        coVerify(exactly = 1) { operationUseCase.cleanupCancelled(linked.id) }
    }

    @Test
    fun `cancelling linked registration waits for cancellation then cleans subscription`() = runTest(testDispatcher) {
        val operationUseCase = mockk<OnuRegistrationOperationUseCase>()
        val linked = OnuRegistrationOperation(
            id = "linked-cancel-flow",
            serial = "VSOL0031C0B6",
            subscriptionId = 84,
            phase = "PROVISIONING",
            state = "RUNNING",
            revision = 5,
        )
        val requested = linked.copy(state = "CANCEL_REQUESTED", revision = 6)
        val cancelled = linked.copy(state = "CANCELLED", revision = 7)
        coEvery { operationUseCase.active() } returns linked
        coEvery { operationUseCase.get(linked.id) } returnsMany listOf(linked, cancelled)
        coEvery { operationUseCase.cancel(linked.id, linked.revision) } returns requested
        coEvery { operationUseCase.cleanupCancelled(linked.id) } returns OnuRegistrationCleanupReport("COMPLETE")
        coEvery { getAvailableOnuListUseCase() } returns Result.success(
            listOf(Onu("1", "olt-lab", "3", "type-1", "type", "gpon", "2", linked.serial))
        )
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(sampleCatalog())
        viewModel = viewModelWithPreauthorization(operationUseCase)
        viewModel.loadScreenData(null)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isRegistering)
        viewModel.onIntent(RegisterSubscriptionIntent.CancelOnuRegistration)
        viewModel.onIntent(RegisterSubscriptionIntent.ConfirmCancelOnuRegistration)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.registrationCancelled)
        assertNull(viewModel.uiState.value.preauthorizationOperation)
        coVerify(exactly = 1) { operationUseCase.cancel(linked.id, linked.revision) }
        coVerify(exactly = 1) { operationUseCase.cleanupCancelled(linked.id) }
    }

    @Test
    fun `cancellation requested during submission waits for response before deleting subscription`() = runTest(testDispatcher) {
        val operationUseCase = mockk<OnuRegistrationOperationUseCase>()
        val onu = Onu("b", "olt", "1", "t", "VSOLVA74", "gpon", "p", "VSOL0031C0B6")
        val preauthorization = OnuRegistrationOperation(
            id = "cancel-during-submit",
            serial = onu.sn,
            subscriptionId = null,
            phase = "READY_FOR_FORM",
            state = "READY_FOR_FORM",
            revision = 4,
        )
        val linked = preauthorization.copy(subscriptionId = 84, phase = "PROVISIONING", state = "RUNNING", revision = 5)
        val requested = linked.copy(state = "CANCEL_REQUESTED", revision = 6)
        val cancelled = linked.copy(state = "CANCELLED", revision = 7)
        val submissionStarted = CompletableDeferred<Unit>()
        val finishSubmission = CompletableDeferred<Result<RegisterSubscriptionResult>>()
        coEvery { operationUseCase.active() } returns preauthorization
        coEvery { operationUseCase.draft(preauthorization.id) } returns mapOf("facadePhotoUrl" to "https://storage.example/photo.jpg")
        coEvery { operationUseCase.saveDraft(any(), any()) } returns Unit
        coEvery { operationUseCase.get(preauthorization.id) } returnsMany listOf(linked, cancelled)
        coEvery { operationUseCase.cancel(linked.id, linked.revision) } returns requested
        coEvery { operationUseCase.cleanupCancelled(linked.id) } returns OnuRegistrationCleanupReport("COMPLETE")
        coEvery { getAvailableOnuListUseCase() } returns Result.success(listOf(onu))
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(napBoxes = listOf(fiberNap()), onus = listOf(fiberOnu().copy(sn = onu.sn)))
        )
        coEvery { registerSubscriptionUseCase(any(), any(), facadePhotoFile = any()) } coAnswers {
            submissionStarted.complete(Unit)
            finishSubmission.await()
        }
        viewModel = viewModelWithPreauthorization(operationUseCase)
        viewModel.loadScreenData(null)
        advanceUntilIdle()
        fillValidFiberForm(NapBoxResponse(id = "n1", placeName = "P1", placeId = 1), onu)
        fillWifiFields()

        viewModel.saveSubscription(facadePhotoFile)
        testScheduler.runCurrent()
        assertTrue(submissionStarted.isCompleted)
        viewModel.onIntent(RegisterSubscriptionIntent.CancelOnuRegistration)
        viewModel.onIntent(RegisterSubscriptionIntent.ConfirmCancelOnuRegistration)
        coVerify(exactly = 0) { operationUseCase.cancel(any(), any()) }

        finishSubmission.complete(Result.success(RegisterSubscriptionResult.Registered(Subscription(subscriptionId = 84))))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.registrationCancelled)
        coVerify(exactly = 1) { operationUseCase.cancel(linked.id, linked.revision) }
        coVerify(exactly = 1) { operationUseCase.cleanupCancelled(linked.id) }
    }

    @Test
    fun `incomplete cleanup remains retryable and succeeds on the next cancellation`() = runTest(testDispatcher) {
        val operationUseCase = mockk<OnuRegistrationOperationUseCase>()
        val linked = OnuRegistrationOperation(
            id = "retry-cleanup-flow",
            serial = "VSOL0031C0B6",
            subscriptionId = 84,
            phase = "PROVISIONING",
            state = "RUNNING",
            revision = 5,
        )
        val requested = linked.copy(state = "CANCEL_REQUESTED", revision = 6)
        val cancelled = linked.copy(state = "CANCELLED", revision = 7)
        coEvery { operationUseCase.active() } returns linked
        coEvery { operationUseCase.get(linked.id) } returnsMany listOf(linked, cancelled, cancelled)
        coEvery { operationUseCase.cancel(linked.id, linked.revision) } returns requested
        coEvery { operationUseCase.cleanupCancelled(linked.id) } returnsMany listOf(
            OnuRegistrationCleanupReport("INCOMPLETE", "La ONU aún está ocupada"),
            OnuRegistrationCleanupReport("COMPLETE"),
        )
        coEvery { getAvailableOnuListUseCase() } returns Result.success(
            listOf(Onu("1", "olt-lab", "3", "type-1", "type", "gpon", "2", linked.serial))
        )
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(sampleCatalog())
        viewModel = viewModelWithPreauthorization(operationUseCase)
        viewModel.loadScreenData(null)
        advanceUntilIdle()

        viewModel.onIntent(RegisterSubscriptionIntent.CancelOnuRegistration)
        viewModel.onIntent(RegisterSubscriptionIntent.ConfirmCancelOnuRegistration)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.registrationCancelled)
        assertFalse(viewModel.uiState.value.cancellationInProgress)
        assertEquals("La ONU aún está ocupada", viewModel.uiState.value.preauthorizationError)

        viewModel.onIntent(RegisterSubscriptionIntent.CancelOnuRegistration)
        viewModel.onIntent(RegisterSubscriptionIntent.ConfirmCancelOnuRegistration)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.registrationCancelled)
        coVerify(exactly = 2) { operationUseCase.cleanupCancelled(linked.id) }
    }

    @Test
    fun `reopening an unlinked cancellation resumes cleanup and refreshes ONU list`() = runTest(testDispatcher) {
        val operationUseCase = mockk<OnuRegistrationOperationUseCase>()
        val serial = "ZTEGDC47BFFD"
        val onu = Onu("1", "olt-lab", "3", "type-1", "ZTEG", "gpon", "2", serial)
        val cancelling = OnuRegistrationOperation(
            id = "restart-cancel-1",
            serial = serial,
            subscriptionId = null,
            phase = "WAITING_FOR_ACS",
            state = "CANCEL_REQUESTED",
            revision = 6,
        )
        val cancelled = cancelling.copy(state = "CANCELLED", revision = 7)
        coEvery { operationUseCase.active() } returns cancelling
        coEvery { operationUseCase.get(cancelling.id) } returns cancelled
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(sampleCatalog())
        coEvery { getAvailableOnuListUseCase() } returns Result.success(listOf(onu))
        viewModel = viewModelWithPreauthorization(operationUseCase)

        viewModel.loadScreenData(null)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.registrationCancelled)
        assertNull(viewModel.uiState.value.preauthorizationOperation)
        assertEquals(onu, viewModel.uiState.value.registerSubscriptionForm.onuList.single())
        coVerify(exactly = 1) { operationUseCase.get(cancelling.id) }
        coVerify(exactly = 1) { getAvailableOnuListUseCase() }
    }

    @Test
    fun `reopening resumes only the cancellation intent matching the active request key`() = runTest(testDispatcher) {
        val intentStore = InMemoryCancellationIntentStore()
        val serial = "ZTEGDC47BFFD"
        val requestKey = "request-key-after-restart"
        intentStore.save(OnuRegistrationCancellationIntent(1, requestKey, serial))
        val operationUseCase = mockk<OnuRegistrationOperationUseCase>()
        val active = OnuRegistrationOperation(
            id = "operation-after-restart",
            serial = serial,
            subscriptionId = null,
            phase = "OLT_AUTHORIZATION",
            state = "RUNNING",
            revision = 11,
            registrationRequestKey = requestKey,
        )
        val cancelling = active.copy(state = "CANCEL_REQUESTED", revision = 12)
        val cancelled = cancelling.copy(state = "CANCELLED", revision = 13)
        val onu = Onu("1", "olt-lab", "3", "type-1", "ZTEG", "gpon", "2", serial)
        coEvery { operationUseCase.active() } returns active
        coEvery { operationUseCase.cancel(active.id, active.revision) } returns cancelling
        coEvery { operationUseCase.get(active.id) } returnsMany listOf(active, cancelled)
        coEvery { operationUseCase.cancel(active.id, active.revision) } returns cancelling
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(sampleCatalog())
        coEvery { getAvailableOnuListUseCase() } returns Result.success(listOf(onu))
        viewModel = viewModelWithPreauthorization(operationUseCase, intentStore)

        viewModel.loadScreenData(null)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.registrationCancelled)
        assertEquals(onu, viewModel.uiState.value.registerSubscriptionForm.onuList.single())
        assertNull(intentStore.get(1))
        coVerify(exactly = 1) { operationUseCase.cancel(active.id, active.revision) }
        coVerify(exactly = 1) { getAvailableOnuListUseCase() }
    }

    @Test
    fun `cancellation during authorization persists its request key before waiting for the response`() = runTest(testDispatcher) {
        val intentStore = InMemoryCancellationIntentStore()
        val operationUseCase = mockk<OnuRegistrationOperationUseCase>()
        val serial = "ZTEGDC47BFFD"
        val onu = Onu("1", "olt-lab", "3", "type-1", "type", "gpon", "2", serial)
        val startRequestKey = CompletableDeferred<String>()
        val finishStart = CompletableDeferred<OnuRegistrationOperation>()
        var activeOperation: OnuRegistrationOperation? = null
        val cancellationRequested = OnuRegistrationOperation(
            id = "in-flight-cancel",
            serial = serial,
            subscriptionId = null,
            phase = "OLT_AUTHORIZATION",
            state = "CANCEL_REQUESTED",
            revision = 4,
        )
        val cancelled = cancellationRequested.copy(state = "CANCELLED", revision = 5)
        coEvery { operationUseCase.active() } coAnswers { activeOperation }
        coEvery { operationUseCase.start(any()) } coAnswers {
            val request = firstArg<com.dscorp.ispadmin.domain.repository.StartOnuRegistrationRequest>()
            startRequestKey.complete(request.requestKey)
            finishStart.await()
        }
        coEvery { operationUseCase.cancel(cancellationRequested.id, any()) } returns cancellationRequested
        coEvery { operationUseCase.get(cancellationRequested.id) } returns cancelled
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(onus = listOf(fiberOnu().copy(sn = serial)))
        )
        coEvery { getAvailableOnuListUseCase() } returns Result.success(listOf(onu))
        viewModel = viewModelWithPreauthorization(operationUseCase, intentStore)

        viewModel.loadScreenData(null)
        advanceUntilIdle()
        viewModel.onIntent(RegisterSubscriptionIntent.OnuSelected(onu))
        viewModel.onIntent(RegisterSubscriptionIntent.WizardContinueClicked)
        testScheduler.runCurrent()
        val requestKey = startRequestKey.await()

        viewModel.onIntent(RegisterSubscriptionIntent.CancelOnuRegistration)
        viewModel.onIntent(RegisterSubscriptionIntent.ConfirmCancelOnuRegistration)
        testScheduler.runCurrent()

        assertEquals(requestKey, intentStore.get(1)?.requestKey)
        assertEquals(serial, intentStore.get(1)?.serial)

        activeOperation = cancellationRequested.copy(
            state = "RUNNING",
            revision = 3,
            registrationRequestKey = requestKey,
        )
        finishStart.complete(activeOperation!!)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.registrationCancelled)
        assertNull(intentStore.get(1))
    }

    @Test
    fun `uploading preauthorization photo deletes its temporary local copy`() = runTest(testDispatcher) {
        val operationUseCase = mockk<OnuRegistrationOperationUseCase>()
        val ready = OnuRegistrationOperation(
            id = "preauth-photo-1", serial = "VSOL0031C0B6", subscriptionId = null,
            phase = "READY_FOR_FORM", state = "READY_FOR_FORM", revision = 2,
        )
        val file = File.createTempFile("facade_upload_test", ".jpg")
        coEvery { operationUseCase.active() } returns ready
        coEvery { operationUseCase.draft(ready.id) } returns null
        coEvery { operationUseCase.saveDraft(any(), any()) } returns Unit
        coEvery { operationUseCase.uploadPhoto(ready.id, file) } returns "https://storage.example/photo.jpg"
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(onus = listOf(fiberOnu().copy(sn = ready.serial)))
        )
        viewModel = viewModelWithPreauthorization(operationUseCase)

        viewModel.loadScreenData(null)
        advanceUntilIdle()
        viewModel.onFacadePhotoSelected(io.mockk.mockk<android.net.Uri>(), file)
        advanceUntilIdle()

        assertFalse(file.exists())
        assertEquals("https://storage.example/photo.jpg", viewModel.uiState.value.registerSubscriptionForm.facadePhotoUrl)
        coVerify(exactly = 1) { operationUseCase.uploadPhoto(ready.id, file) }
    }

    @Test
    fun `loadScreenData does not mark registering while catalog loads`() = runTest(testDispatcher) {
        coEvery { getRegistrationCatalogUseCase() } coAnswers {
            delay(50)
            Result.success(sampleCatalog())
        }

        viewModel.loadScreenData(null)
        testScheduler.runCurrent()
        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(false, viewModel.uiState.value.isRegistering)

        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals(false, viewModel.uiState.value.isRegistering)
    }

    @Test
    fun `loadScreenData emits error when user session fails`() = runTest(testDispatcher) {
        coEvery { getUserSessionUseCase() } returns Result.failure(Exception("no session"))

        val events = mutableListOf<RegisterSubscriptionUiEvent>()
        val job = launch {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.loadScreenData(null)
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events[0] is RegisterSubscriptionUiEvent.Error)
        assertEquals("no session", (events[0] as RegisterSubscriptionUiEvent.Error).message)
        assertEquals(false, viewModel.uiState.value.isLoading)

        job.cancel()
    }

    @Test
    fun `loadScreenData merges installation order after catalog`() = runTest(testDispatcher) {
        val place = Place(id = "5", name = "Lima")
        val order = InstallationOrder(
            id = 99,
            customerFirstName = "Ana",
            customerLastName = "Lopez",
            customerAddress = "Av 1",
            customerPhone = "999999999",
            customerDni = "12345678",
            place = place
        )
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(napBoxes = listOf(CatalogNapBox(id = "1", placeName = "Lima", placeId = 5)))
        )
        coEvery { installationOrderUseCase.getInstallationOrderByIdResult(99) } returns Result.success(order)

        viewModel.loadScreenData(99)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(99, viewModel.uiState.value.orderId)
        val form = viewModel.uiState.value.registerSubscriptionForm
        assertEquals("Ana", form.firstName)
        assertEquals("Lopez", form.lastName)
        assertEquals(place, form.selectedPlace)
    }

    @Test
    fun `refreshOnuList emits error on failure`() = runTest(testDispatcher) {
        coEvery { getAvailableOnuListUseCase() } returns Result.failure(Exception("onu fail"))

        viewModel.loadScreenData(null)
        advanceUntilIdle()

        val events = mutableListOf<RegisterSubscriptionUiEvent>()
        val job = launch {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.onIntent(RegisterSubscriptionIntent.RefreshOnuList)
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertEquals("onu fail", (events[0] as RegisterSubscriptionUiEvent.Error).message)

        job.cancel()
    }

    @Test
    fun `saveSubscription emits success when register succeeds`() = runTest(testDispatcher) {
        val nap = NapBoxResponse(id = "n1", placeName = "P1", placeId = 1)
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(napBoxes = listOf(fiberNap()), onus = listOf(fiberOnu()))
        )

        viewModel.loadScreenData(null)
        advanceUntilIdle()

        val registered = Subscription(subscriptionId = 1, firstName = "A", lastName = "B")
        coEvery {
            registerSubscriptionUseCase(any(), any(), facadePhotoFile = any())
        } answers {
            assertEquals(true, firstArg<Subscription>().autoCut)
            Result.success(RegisterSubscriptionResult.Registered(registered))
        }

        viewModel.onIntent(RegisterSubscriptionIntent.FirstNameChanged("Juan"))
        viewModel.onIntent(RegisterSubscriptionIntent.LastNameChanged("Perez"))
        viewModel.onIntent(RegisterSubscriptionIntent.DniChanged("12345678"))
        viewModel.onIntent(RegisterSubscriptionIntent.AddressChanged("Calle larga 12345"))
        viewModel.onIntent(RegisterSubscriptionIntent.PhoneChanged("987654321"))
        viewModel.onIntent(RegisterSubscriptionIntent.PlanSelected(samplePlan))
        viewModel.onIntent(RegisterSubscriptionIntent.PlaceSelected(Place(id = "1", name = "P")))
        viewModel.onIntent(RegisterSubscriptionIntent.NapBoxSelected(nap))
        viewModel.onIntent(RegisterSubscriptionIntent.OnuSelected(onu))
        fillWifiFields()
        selectValidLocation()

        val events = mutableListOf<RegisterSubscriptionUiEvent>()
        val job = launch {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.saveSubscription(facadePhotoFile)
        advanceUntilIdle()

        assertEquals(1, events.size)
        val success = events[0] as RegisterSubscriptionUiEvent.Success
        assertEquals(registered.subscriptionId, success.subscription.subscriptionId)
        assertEquals(false, viewModel.uiState.value.isLoading)

        job.cancel()
    }

    @Test
    fun `saveSubscription emits QueuedOffline when register is queued locally`() = runTest(testDispatcher) {
        val nap = NapBoxResponse(id = "n1", placeName = "P1", placeId = 1)
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(napBoxes = listOf(fiberNap()), onus = listOf(fiberOnu()))
        )

        viewModel.loadScreenData(null)
        advanceUntilIdle()

        val pending = PendingSubscription(
            localId = "local-1",
            clientRequestId = "client-1",
            subscriptionJson = "{}",
            createdAt = 1L
        )
        coEvery {
            registerSubscriptionUseCase(any(), any(), facadePhotoFile = any())
        } returns Result.success(RegisterSubscriptionResult.QueuedOffline(pending))

        viewModel.onIntent(RegisterSubscriptionIntent.FirstNameChanged("Juan"))
        viewModel.onIntent(RegisterSubscriptionIntent.LastNameChanged("Perez"))
        viewModel.onIntent(RegisterSubscriptionIntent.DniChanged("12345678"))
        viewModel.onIntent(RegisterSubscriptionIntent.AddressChanged("Calle larga 12345"))
        viewModel.onIntent(RegisterSubscriptionIntent.PhoneChanged("987654321"))
        viewModel.onIntent(RegisterSubscriptionIntent.PlanSelected(samplePlan))
        viewModel.onIntent(RegisterSubscriptionIntent.PlaceSelected(Place(id = "1", name = "P")))
        viewModel.onIntent(RegisterSubscriptionIntent.NapBoxSelected(nap))
        viewModel.onIntent(RegisterSubscriptionIntent.OnuSelected(onu))
        fillWifiFields()
        selectValidLocation()

        val events = mutableListOf<RegisterSubscriptionUiEvent>()
        val job = launch {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.saveSubscription(facadePhotoFile)
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertEquals(RegisterSubscriptionUiEvent.QueuedOffline, events[0])
        assertEquals(false, viewModel.uiState.value.isLoading)

        job.cancel()
    }

    @Test
    fun `saveSubscription emits error when register fails`() = runTest(testDispatcher) {
        val nap = NapBoxResponse(id = "n1", placeName = "P1", placeId = 1)
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(napBoxes = listOf(fiberNap()), onus = listOf(fiberOnu()))
        )

        viewModel.loadScreenData(null)
        advanceUntilIdle()

        coEvery { registerSubscriptionUseCase(any(), any(), facadePhotoFile = any()) } returns Result.failure(Exception("backend"))

        viewModel.onIntent(RegisterSubscriptionIntent.FirstNameChanged("Juan"))
        viewModel.onIntent(RegisterSubscriptionIntent.LastNameChanged("Perez"))
        viewModel.onIntent(RegisterSubscriptionIntent.DniChanged("12345678"))
        viewModel.onIntent(RegisterSubscriptionIntent.AddressChanged("Calle larga 12345"))
        viewModel.onIntent(RegisterSubscriptionIntent.PhoneChanged("987654321"))
        viewModel.onIntent(RegisterSubscriptionIntent.PlanSelected(samplePlan))
        viewModel.onIntent(RegisterSubscriptionIntent.PlaceSelected(Place(id = "1", name = "P")))
        viewModel.onIntent(RegisterSubscriptionIntent.NapBoxSelected(nap))
        viewModel.onIntent(RegisterSubscriptionIntent.OnuSelected(onu))
        fillWifiFields()
        selectValidLocation()

        val events = mutableListOf<RegisterSubscriptionUiEvent>()
        val job = launch {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.saveSubscription(facadePhotoFile)
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertEquals("backend", (events[0] as RegisterSubscriptionUiEvent.Error).message)

        job.cancel()
    }

    @Test
    fun `processCurrentLocation keeps only latest nearby nap result`() = runTest(testDispatcher) {
        val slowNap = NapBoxResponse(id = "a", placeName = "A", placeId = 1)
        val fastNap = NapBoxResponse(id = "b", placeName = "B", placeId = 2)
        coEvery { getPlaceFromLocationUseCase(any(), any()) } returns Result.success(
            Place(id = "1", name = "P")
        )
        coEvery { getNearNapBoxesUseCase(1.0, 1.0) } coAnswers {
            delay(10_000)
            Result.success(listOf(slowNap))
        }
        coEvery { getNearNapBoxesUseCase(2.0, 2.0) } returns Result.success(listOf(fastNap))

        viewModel.loadScreenData(null)
        advanceUntilIdle()

        viewModel.processCurrentLocation(1.0, 1.0)
        advanceTimeBy(5)
        viewModel.processCurrentLocation(2.0, 2.0)
        advanceUntilIdle()

        assertEquals(listOf(fastNap), viewModel.uiState.value.cachedNapBoxList)
    }

    @Test
    fun `processCurrentLocation toggles isLoadingLocation`() = runTest(testDispatcher) {
        coEvery { getPlaceFromLocationUseCase(any(), any()) } coAnswers {
            delay(50)
            Result.success(Place(id = "1", name = "P"))
        }
        coEvery { getNearNapBoxesUseCase(any(), any()) } returns Result.success(emptyList())

        viewModel.loadScreenData(null)
        advanceUntilIdle()

        viewModel.processCurrentLocation(-11.0, -77.0)
        assertTrue(viewModel.uiState.value.isLoadingLocation)

        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.isLoadingLocation)
    }

    @Test
    fun `saveSubscription ignores second call while first is in progress`() = runTest(testDispatcher) {
        val nap = NapBoxResponse(id = "n1", placeName = "P1", placeId = 1)
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(napBoxes = listOf(fiberNap()), onus = listOf(fiberOnu()))
        )

        viewModel.loadScreenData(null)
        advanceUntilIdle()

        val registered = Subscription(subscriptionId = 1, firstName = "A", lastName = "B")
        coEvery { registerSubscriptionUseCase(any(), any(), facadePhotoFile = any()) } coAnswers {
            delay(100)
            Result.success(RegisterSubscriptionResult.Registered(registered))
        }

        viewModel.onIntent(RegisterSubscriptionIntent.FirstNameChanged("Juan"))
        viewModel.onIntent(RegisterSubscriptionIntent.LastNameChanged("Perez"))
        viewModel.onIntent(RegisterSubscriptionIntent.DniChanged("12345678"))
        viewModel.onIntent(RegisterSubscriptionIntent.AddressChanged("Calle larga 12345"))
        viewModel.onIntent(RegisterSubscriptionIntent.PhoneChanged("987654321"))
        viewModel.onIntent(RegisterSubscriptionIntent.PlanSelected(samplePlan))
        viewModel.onIntent(RegisterSubscriptionIntent.PlaceSelected(Place(id = "1", name = "P")))
        viewModel.onIntent(RegisterSubscriptionIntent.NapBoxSelected(nap))
        viewModel.onIntent(RegisterSubscriptionIntent.OnuSelected(onu))
        fillWifiFields()
        selectValidLocation()

        viewModel.saveSubscription(facadePhotoFile)
        viewModel.saveSubscription(facadePhotoFile)
        advanceUntilIdle()

        coVerify(exactly = 1) { registerSubscriptionUseCase(any(), any(), facadePhotoFile = any()) }
    }

    @Test
    fun `loadScreenData auto selects single active core and hides selector`() = runTest(testDispatcher) {
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(
                coreDevices = listOf(
                    CatalogCoreDevice(id = 10, name = "Core-A", disabled = false),
                    CatalogCoreDevice(id = 99, name = "Disabled", disabled = true)
                )
            )
        )

        viewModel.loadScreenData(null)
        advanceUntilIdle()

        val form = viewModel.uiState.value.registerSubscriptionForm
        assertEquals(sampleCoreDevice, form.selectedHostDevice)
        assertFalse(form.shouldShowHostDeviceSelector())
    }

    @Test
    fun `loadScreenData leaves host null when multiple active cores`() = runTest(testDispatcher) {
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(
                coreDevices = listOf(
                    CatalogCoreDevice(id = 10, name = "Core-A", disabled = false),
                    CatalogCoreDevice(id = 11, name = "Core-B", disabled = false)
                )
            )
        )

        viewModel.loadScreenData(null)
        advanceUntilIdle()

        val form = viewModel.uiState.value.registerSubscriptionForm
        assertNull(form.selectedHostDevice)
        assertTrue(form.shouldShowHostDeviceSelector())
    }

    @Test
    fun `loadScreenData emits error when no active cores`() = runTest(testDispatcher) {
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(
                coreDevices = listOf(CatalogCoreDevice(id = 1, name = "Off", disabled = true))
            )
        )

        val events = mutableListOf<RegisterSubscriptionUiEvent>()
        val job = launch {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.loadScreenData(null)
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events[0] is RegisterSubscriptionUiEvent.Error)
        assertEquals(
            "No hay routers core disponibles",
            (events[0] as RegisterSubscriptionUiEvent.Error).message
        )

        job.cancel()
    }

    @Test
    fun `HostDeviceSelected updates selected host and clears error`() = runTest(testDispatcher) {
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(
                coreDevices = listOf(
                    CatalogCoreDevice(id = 10, name = "Core-A", disabled = false),
                    CatalogCoreDevice(id = 11, name = "Core-B", disabled = false)
                )
            )
        )

        viewModel.loadScreenData(null)
        advanceUntilIdle()

        viewModel.onIntent(RegisterSubscriptionIntent.RegisterClick(facadePhotoFile))
        advanceUntilIdle()
        assertTrue(
            viewModel.uiState.value.registerSubscriptionForm.hostDeviceError != null
        )

        viewModel.onIntent(RegisterSubscriptionIntent.HostDeviceSelected(secondCoreDevice))
        advanceUntilIdle()

        val form = viewModel.uiState.value.registerSubscriptionForm
        assertEquals(secondCoreDevice, form.selectedHostDevice)
        assertNull(form.hostDeviceError)
    }

    @Test
    fun `RegisterClick fails validation when multiple cores and none selected`() = runTest(testDispatcher) {
        val nap = NapBoxResponse(id = "n1", placeName = "P1", placeId = 1)
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(
                napBoxes = listOf(fiberNap()),
                onus = listOf(fiberOnu()),
                coreDevices = listOf(
                    CatalogCoreDevice(id = 10, name = "Core-A", disabled = false),
                    CatalogCoreDevice(id = 11, name = "Core-B", disabled = false)
                )
            )
        )

        viewModel.loadScreenData(null)
        advanceUntilIdle()

        viewModel.onIntent(RegisterSubscriptionIntent.FirstNameChanged("Juan"))
        viewModel.onIntent(RegisterSubscriptionIntent.LastNameChanged("Perez"))
        viewModel.onIntent(RegisterSubscriptionIntent.DniChanged("12345678"))
        viewModel.onIntent(RegisterSubscriptionIntent.AddressChanged("Calle larga 12345"))
        viewModel.onIntent(RegisterSubscriptionIntent.PhoneChanged("987654321"))
        viewModel.onIntent(RegisterSubscriptionIntent.PlanSelected(samplePlan))
        viewModel.onIntent(RegisterSubscriptionIntent.PlaceSelected(Place(id = "1", name = "P")))
        viewModel.onIntent(RegisterSubscriptionIntent.NapBoxSelected(nap))
        viewModel.onIntent(RegisterSubscriptionIntent.OnuSelected(onu))
        fillWifiFields()

        viewModel.onIntent(RegisterSubscriptionIntent.RegisterClick(facadePhotoFile))
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.registerSubscriptionForm.hostDeviceError)
        coVerify(exactly = 0) { registerSubscriptionUseCase(any(), any(), facadePhotoFile = any()) }
    }

    @Test
    fun `loadScreenData marks form offline when MikroTik is unreachable`() = runTest(testDispatcher) {
        offlineModeFlow.value = true

        viewModel.loadScreenData(null)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isOfflineMode)
        assertTrue(viewModel.uiState.value.registerSubscriptionForm.requiresClientIpAddress)
    }

    @Test
    fun `ClientIpAddressChanged stores value on form`() = runTest(testDispatcher) {
        viewModel.loadScreenData(null)
        advanceUntilIdle()

        viewModel.onIntent(RegisterSubscriptionIntent.ClientIpAddressChanged("10.1.1.20"))
        advanceUntilIdle()

        assertEquals("10.1.1.20", viewModel.uiState.value.registerSubscriptionForm.clientIpAddress)
    }

    @Test
    fun `saveSubscription sends clientIpAddress when offline`() = runTest(testDispatcher) {
        offlineModeFlow.value = true
        val nap = NapBoxResponse(id = "n1", placeName = "P1", placeId = 1)
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(napBoxes = listOf(fiberNap()), onus = listOf(fiberOnu()))
        )
        viewModel.loadScreenData(null)
        advanceUntilIdle()

        val pending = PendingSubscription(
            localId = "local-1",
            clientRequestId = "client-1",
            subscriptionJson = "{}",
            createdAt = 1L
        )
        coEvery {
            registerSubscriptionUseCase(any(), any(), facadePhotoFile = any())
        } answers {
            assertEquals("192.168.25.10", firstArg<Subscription>().clientIpAddress)
            assertEquals("192.168.25.10", firstArg<Subscription>().ip)
            Result.success(RegisterSubscriptionResult.QueuedOffline(pending))
        }

        viewModel.onIntent(RegisterSubscriptionIntent.FirstNameChanged("Juan"))
        viewModel.onIntent(RegisterSubscriptionIntent.LastNameChanged("Perez"))
        viewModel.onIntent(RegisterSubscriptionIntent.DniChanged("12345678"))
        viewModel.onIntent(RegisterSubscriptionIntent.AddressChanged("Calle larga 12345"))
        viewModel.onIntent(RegisterSubscriptionIntent.PhoneChanged("987654321"))
        viewModel.onIntent(RegisterSubscriptionIntent.PlanSelected(samplePlan))
        viewModel.onIntent(RegisterSubscriptionIntent.PlaceSelected(Place(id = "1", name = "P")))
        viewModel.onIntent(RegisterSubscriptionIntent.NapBoxSelected(nap))
        viewModel.onIntent(RegisterSubscriptionIntent.OnuSelected(onu))
        viewModel.onIntent(RegisterSubscriptionIntent.ClientIpAddressChanged("192.168.25.10"))
        fillWifiFields()
        selectValidLocation()

        viewModel.saveSubscription(facadePhotoFile)
        advanceUntilIdle()

        coVerify(exactly = 1) { registerSubscriptionUseCase(any(), any(), facadePhotoFile = any()) }
    }

    @Test
    fun `form defaults vlan to 100`() = runTest(testDispatcher) {
        viewModel.loadScreenData(null)
        advanceUntilIdle()

        assertEquals("100", viewModel.uiState.value.registerSubscriptionForm.vlan)
    }

    @Test
    fun `OnVlanChanged updates form vlan`() = runTest(testDispatcher) {
        viewModel.loadScreenData(null)
        advanceUntilIdle()

        viewModel.onIntent(RegisterSubscriptionIntent.OnVlanChanged("100"))

        assertEquals("100", viewModel.uiState.value.registerSubscriptionForm.vlan)
    }

    @Test
    fun `OnVlanChanged ignores disabled vlan 1`() = runTest(testDispatcher) {
        viewModel.loadScreenData(null)
        advanceUntilIdle()

        viewModel.onIntent(RegisterSubscriptionIntent.OnVlanChanged("1"))

        assertEquals("100", viewModel.uiState.value.registerSubscriptionForm.vlan)
    }

    @Test
    fun `saveSubscription sends vlan 100 only for FIBER`() = runTest(testDispatcher) {
        val nap = NapBoxResponse(id = "n1", placeName = "P1", placeId = 1)
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(napBoxes = listOf(fiberNap()), onus = listOf(fiberOnu()))
        )
        viewModel.loadScreenData(null)
        advanceUntilIdle()

        coEvery {
            registerSubscriptionUseCase(any(), any(), facadePhotoFile = any())
        } answers {
            assertEquals("100", firstArg<Subscription>().vlan)
            Result.success(RegisterSubscriptionResult.Registered(Subscription(subscriptionId = 1)))
        }

        fillValidFiberForm(nap, onu)
        fillWifiFields()
        viewModel.onIntent(RegisterSubscriptionIntent.OnVlanChanged("100"))

        viewModel.saveSubscription(facadePhotoFile)
        advanceUntilIdle()

        coVerify(exactly = 1) { registerSubscriptionUseCase(any(), any(), facadePhotoFile = any()) }
    }

    @Test
    fun `saveSubscription includes wifi fields for FIBER`() = runTest(testDispatcher) {
        val nap = NapBoxResponse(id = "n1", placeName = "P1", placeId = 1)
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(napBoxes = listOf(fiberNap()), onus = listOf(fiberOnu()))
        )
        viewModel.loadScreenData(null)
        advanceUntilIdle()

        coEvery {
            registerSubscriptionUseCase(any(), any(), facadePhotoFile = any())
        } answers {
            val sent = firstArg<Subscription>()
            assertEquals("CasaFibra24", sent.wifiSsid24)
            assertEquals("clave24xx", sent.wifiPassword24)
            assertEquals("CasaFibra24 - 5G", sent.wifiSsid5)
            assertEquals("clave24xx", sent.wifiPassword5)
            Result.success(RegisterSubscriptionResult.Registered(Subscription(subscriptionId = 1)))
        }

        fillValidFiberForm(nap, onu)
        fillWifiFields()
        viewModel.saveSubscription(facadePhotoFile)
        advanceUntilIdle()

        coVerify(exactly = 1) { registerSubscriptionUseCase(any(), any(), facadePhotoFile = any()) }
    }

    @Test
    fun `form defaults accessMode to PPPoE`() {
        assertEquals(
            AccessMode.PPPOE_DYNAMIC,
            viewModel.uiState.value.registerSubscriptionForm.accessMode,
        )
    }

    @Test
    fun `AccessModeSelected updates form wan type`() {
        viewModel.onIntent(RegisterSubscriptionIntent.AccessModeSelected(AccessMode.STATIC_IP))
        assertEquals(
            AccessMode.STATIC_IP,
            viewModel.uiState.value.registerSubscriptionForm.accessMode,
        )
        viewModel.onIntent(RegisterSubscriptionIntent.AccessModeSelected(AccessMode.PPPOE_DYNAMIC))
        assertEquals(
            AccessMode.PPPOE_DYNAMIC,
            viewModel.uiState.value.registerSubscriptionForm.accessMode,
        )
    }

    @Test
    fun `saveSubscription sends PPPOE_DYNAMIC by default`() = runTest(testDispatcher) {
        val nap = NapBoxResponse(id = "n1", placeName = "P1", placeId = 1)
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(napBoxes = listOf(fiberNap()), onus = listOf(fiberOnu()))
        )
        viewModel.loadScreenData(null)
        advanceUntilIdle()

        coEvery {
            registerSubscriptionUseCase(any(), any(), facadePhotoFile = any())
        } answers {
            assertEquals("PPPOE_DYNAMIC", firstArg<Subscription>().accessMode)
            Result.success(RegisterSubscriptionResult.Registered(Subscription(subscriptionId = 1)))
        }

        fillValidFiberForm(nap, onu)
        fillWifiFields()
        viewModel.saveSubscription(facadePhotoFile)
        advanceUntilIdle()

        coVerify(exactly = 1) { registerSubscriptionUseCase(any(), any(), facadePhotoFile = any()) }
    }

    @Test
    fun `saveSubscription sends static ip when wan type is selected`() = runTest(testDispatcher) {
        val nap = NapBoxResponse(id = "n1", placeName = "P1", placeId = 1)
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(napBoxes = listOf(fiberNap()), onus = listOf(fiberOnu()))
        )
        viewModel.loadScreenData(null)
        advanceUntilIdle()

        coEvery {
            registerSubscriptionUseCase(any(), any(), facadePhotoFile = any())
        } answers {
            assertEquals("STATIC_IP", firstArg<Subscription>().accessMode)
            Result.success(RegisterSubscriptionResult.Registered(Subscription(subscriptionId = 1)))
        }

        fillValidFiberForm(nap, onu)
        fillWifiFields()
        viewModel.onIntent(RegisterSubscriptionIntent.AccessModeSelected(AccessMode.STATIC_IP))
        viewModel.saveSubscription(facadePhotoFile)
        advanceUntilIdle()

        coVerify(exactly = 1) { registerSubscriptionUseCase(any(), any(), facadePhotoFile = any()) }
    }

    @Test
    fun `saveSubscription sends static ip when e2e override is set`() = runTest(testDispatcher) {
        E2eAccessModeResolver.override = "static"
        viewModel = RegisterSubscriptionComposeViewModel(
            getAvailableOnuListUseCase = getAvailableOnuListUseCase,
            getRegistrationCatalogUseCase = getRegistrationCatalogUseCase,
            refreshRegistrationCatalogUseCase = refreshRegistrationCatalogUseCase,
            getPlaceFromLocationUseCase = getPlaceFromLocationUseCase,
            registerSubscriptionUseCase = registerSubscriptionUseCase,
            getUserSessionUseCase = getUserSessionUseCase,
            getNearNapBoxesUseCase = getNearNapBoxesUseCase,
            installationOrderUseCase = installationOrderUseCase,
            observeOfflineRegistrationModeUseCase = observeOfflineRegistrationModeUseCase,
            retryTr069ProvisioningUseCase = retryTr069ProvisioningUseCase,
            pollRegistrationProgressUseCase = pollRegistrationProgressUseCase,
            observabilityClient = mockk(relaxed = true),
            mainImmediate = testDispatcher
        )
        val nap = NapBoxResponse(id = "n1", placeName = "P1", placeId = 1)
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(napBoxes = listOf(fiberNap()), onus = listOf(fiberOnu()))
        )
        viewModel.loadScreenData(null)
        advanceUntilIdle()

        coEvery {
            registerSubscriptionUseCase(any(), any(), facadePhotoFile = any())
        } answers {
            assertEquals("STATIC_IP", firstArg<Subscription>().accessMode)
            Result.success(RegisterSubscriptionResult.Registered(Subscription(subscriptionId = 1)))
        }

        fillValidFiberForm(nap, onu)
        fillWifiFields()
        viewModel.saveSubscription(facadePhotoFile)
        advanceUntilIdle()

        coVerify(exactly = 1) { registerSubscriptionUseCase(any(), any(), facadePhotoFile = any()) }
    }

    @Test
    fun `saveSubscription sends distinct ssids and shared password when different names enabled`() =
        runTest(testDispatcher) {
            val nap = NapBoxResponse(id = "n1", placeName = "P1", placeId = 1)
            val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
            coEvery { getRegistrationCatalogUseCase() } returns Result.success(
                sampleCatalog(napBoxes = listOf(fiberNap()), onus = listOf(fiberOnu()))
            )
            viewModel.loadScreenData(null)
            advanceUntilIdle()

            coEvery {
                registerSubscriptionUseCase(any(), any(), facadePhotoFile = any())
            } answers {
                val sent = firstArg<Subscription>()
                assertEquals("CasaFibra24", sent.wifiSsid24)
                assertEquals("clave24xx", sent.wifiPassword24)
                assertEquals("CasaFibra5", sent.wifiSsid5)
                assertEquals("clave24xx", sent.wifiPassword5)
                Result.success(RegisterSubscriptionResult.Registered(Subscription(subscriptionId = 1)))
            }

            fillValidFiberForm(nap, onu)
            fillSplitWifiFields()
            viewModel.saveSubscription(facadePhotoFile)
            advanceUntilIdle()

            coVerify(exactly = 1) { registerSubscriptionUseCase(any(), any(), facadePhotoFile = any()) }
        }

    @Test
    fun `saveSubscription sends vlan null when installation is not FIBER`() = runTest(testDispatcher) {
        val wirelessPlan = PlanResponse(
            id = "w1",
            name = "Wireless",
            price = 10.0,
            downloadSpeed = "50",
            uploadSpeed = "20",
            type = InstallationType.WIRELESS
        )
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(
                plans = listOf(
                    CatalogPlan(
                        id = "p1",
                        name = "Plan",
                        price = 10.0,
                        downloadSpeed = "100",
                        uploadSpeed = "100",
                        type = "FIBER"
                    ),
                    CatalogPlan(
                        id = "w1",
                        name = "Wireless",
                        price = 10.0,
                        downloadSpeed = "50",
                        uploadSpeed = "20",
                        type = "WIRELESS"
                    )
                )
            )
        )
        viewModel.loadScreenData(null)
        advanceUntilIdle()

        coEvery {
            registerSubscriptionUseCase(any(), any(), facadePhotoFile = any())
        } answers {
            val sent = firstArg<Subscription>()
            assertNull(sent.vlan)
            assertNull(sent.wifiSsid24)
            assertNull(sent.wifiPassword24)
            assertNull(sent.wifiSsid5)
            assertNull(sent.wifiPassword5)
            Result.success(RegisterSubscriptionResult.Registered(Subscription(subscriptionId = 2)))
        }

        viewModel.onIntent(RegisterSubscriptionIntent.InstallationTypeSelected(InstallationType.WIRELESS))
        viewModel.onIntent(RegisterSubscriptionIntent.FirstNameChanged("Juan"))
        viewModel.onIntent(RegisterSubscriptionIntent.LastNameChanged("Perez"))
        viewModel.onIntent(RegisterSubscriptionIntent.DniChanged("12345678"))
        viewModel.onIntent(RegisterSubscriptionIntent.AddressChanged("Calle larga 12345"))
        viewModel.onIntent(RegisterSubscriptionIntent.PhoneChanged("987654321"))
        viewModel.onIntent(RegisterSubscriptionIntent.PlanSelected(wirelessPlan))
        viewModel.onIntent(RegisterSubscriptionIntent.PlaceSelected(Place(id = "1", name = "P")))
        viewModel.onIntent(RegisterSubscriptionIntent.OnVlanChanged("100"))
        fillWifiFields()
        selectValidLocation()

        viewModel.saveSubscription(facadePhotoFile)
        advanceUntilIdle()

        coVerify(exactly = 1) { registerSubscriptionUseCase(any(), any(), facadePhotoFile = any()) }
    }

    @Test
    fun `ONLY_TV without CPE kind blocks save`() = runTest(testDispatcher) {
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(catalogWithTv())
        viewModel.loadScreenData(null)
        advanceUntilIdle()

        viewModel.onIntent(RegisterSubscriptionIntent.InstallationTypeSelected(InstallationType.ONLY_TV_FIBER))
        fillValidTvCustomerFields()
        viewModel.onIntent(RegisterSubscriptionIntent.NapBoxSelected(NapBoxResponse(id = "n1", placeName = "P1", placeId = 1)))

        viewModel.saveSubscription(facadePhotoFile)
        advanceUntilIdle()

        coVerify(exactly = 0) { registerSubscriptionUseCase(any(), any(), facadePhotoFile = any()) }
        assertEquals(
            "Seleccione ONU o receptor óptico CATV",
            viewModel.uiState.value.registerSubscriptionForm.tvCpeKindError
        )
    }

    @Test
    fun `saveSubscription ONLY_TV ONU sends vlan and onu without wifi`() = runTest(testDispatcher) {
        val nap = NapBoxResponse(id = "n1", placeName = "P1", placeId = 1)
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(catalogWithTv())
        viewModel.loadScreenData(null)
        advanceUntilIdle()

        coEvery {
            registerSubscriptionUseCase(any(), any(), facadePhotoFile = any())
        } answers {
            val sent = firstArg<Subscription>()
            assertEquals(InstallationType.ONLY_TV_FIBER, sent.installationType)
            assertEquals("100", sent.vlan)
            assertEquals("sn1", sent.onu?.sn)
            assertNull(sent.wifiSsid24)
            Result.success(RegisterSubscriptionResult.Registered(Subscription(subscriptionId = 3)))
        }

        viewModel.onIntent(RegisterSubscriptionIntent.InstallationTypeSelected(InstallationType.ONLY_TV_FIBER))
        viewModel.onIntent(RegisterSubscriptionIntent.TvCpeKindSelected(TvCpeKind.ONU))
        fillValidTvCustomerFields()
        viewModel.onIntent(RegisterSubscriptionIntent.NapBoxSelected(nap))
        viewModel.onIntent(RegisterSubscriptionIntent.OnuSelected(onu))

        viewModel.saveSubscription(facadePhotoFile)
        advanceUntilIdle()

        coVerify(exactly = 1) { registerSubscriptionUseCase(any(), any(), facadePhotoFile = any()) }
    }

    @Test
    fun `saveSubscription ONLY_TV CATV omits onu and vlan`() = runTest(testDispatcher) {
        val nap = NapBoxResponse(id = "n1", placeName = "P1", placeId = 1)
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(catalogWithTv())
        viewModel.loadScreenData(null)
        advanceUntilIdle()

        coEvery {
            registerSubscriptionUseCase(any(), any(), facadePhotoFile = any())
        } answers {
            val sent = firstArg<Subscription>()
            assertEquals(InstallationType.ONLY_TV_FIBER, sent.installationType)
            assertNull(sent.vlan)
            assertNull(sent.onu)
            Result.success(RegisterSubscriptionResult.Registered(Subscription(subscriptionId = 4)))
        }

        viewModel.onIntent(RegisterSubscriptionIntent.InstallationTypeSelected(InstallationType.ONLY_TV_FIBER))
        viewModel.onIntent(RegisterSubscriptionIntent.TvCpeKindSelected(TvCpeKind.ONU))
        viewModel.onIntent(RegisterSubscriptionIntent.OnuSelected(onu))
        viewModel.onIntent(RegisterSubscriptionIntent.TvCpeKindSelected(TvCpeKind.OPTICAL_RECEIVER))
        fillValidTvCustomerFields()
        viewModel.onIntent(RegisterSubscriptionIntent.NapBoxSelected(nap))

        viewModel.saveSubscription(facadePhotoFile)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.registerSubscriptionForm.selectedOnu)
        coVerify(exactly = 1) { registerSubscriptionUseCase(any(), any(), facadePhotoFile = any()) }
    }

    @Test
    fun `InstallationTypeSelected clears wifi fields`() = runTest(testDispatcher) {
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(
                plans = listOf(
                    CatalogPlan(
                        id = "p1",
                        name = "Plan",
                        price = 10.0,
                        downloadSpeed = "100",
                        uploadSpeed = "100",
                        type = "FIBER"
                    ),
                    CatalogPlan(
                        id = "w1",
                        name = "Wireless",
                        price = 10.0,
                        downloadSpeed = "50",
                        uploadSpeed = "20",
                        type = "WIRELESS"
                    )
                )
            )
        )
        viewModel.loadScreenData(null)
        advanceUntilIdle()

        fillWifiFields()
        assertEquals("CasaFibra24", viewModel.uiState.value.registerSubscriptionForm.wifiSsid24)

        viewModel.onIntent(RegisterSubscriptionIntent.InstallationTypeSelected(InstallationType.WIRELESS))
        advanceUntilIdle()

        val form = viewModel.uiState.value.registerSubscriptionForm
        assertEquals("", form.wifiSsid24)
        assertEquals("", form.wifiPassword24)
        assertEquals("", form.wifiSsid5)
        assertEquals("", form.wifiPassword5)
        assertFalse(form.useDifferentWifiNames)
        assertNull(form.wifiSsid24Error)
        assertNull(form.wifiPassword24Error)
        assertNull(form.wifiSsid5Error)
        assertNull(form.wifiPassword5Error)
    }

    @Test
    fun `retryTr069 emits GenieACS error message when still MANUAL_REQUIRED`() = runTest(testDispatcher) {
        val events = mutableListOf<RegisterSubscriptionUiEvent>()
        val job = launch { viewModel.uiEvent.collect { events.add(it) } }

        coEvery { retryTr069ProvisioningUseCase(42) } returns Result.success(
            Subscription(
                subscriptionId = 42,
                tr069ProvisionStatus = "MANUAL_REQUIRED",
                tr069Message = "GenieACS HTTP 400: missing or invalid resource identifier",
            )
        )

        viewModel.onIntent(
            RegisterSubscriptionIntent.RetryTr069(
                Subscription(
                    subscriptionId = 42,
                    tr069ProvisionStatus = "MANUAL_REQUIRED",
                    wifiPassword24 = "wifi-pass-24",
                )
            )
        )
        advanceUntilIdle()

        assertTrue(events.any { it is RegisterSubscriptionUiEvent.Success })
        assertTrue(
            events.filterIsInstance<RegisterSubscriptionUiEvent.Error>().any {
                it.message.contains("GenieACS HTTP 400")
            }
        )
        assertFalse(viewModel.uiState.value.tr069RetryLoading)
        coVerify(exactly = 0) { pollRegistrationProgressUseCase(any(), any()) }
        job.cancel()
    }

    @Test
    fun `retryTr069 PENDING keeps retry loading feedback while polling`() =
        runTest(testDispatcher) {
            coEvery { retryTr069ProvisioningUseCase(2378) } returns Result.success(
                Subscription(
                    subscriptionId = 2378,
                    tr069ProvisionStatus = "PENDING",
                    tr069Message = "Esperando aprovisionamiento TR-069.",
                )
            )
            coEvery { pollRegistrationProgressUseCase(2378, any()) } coAnswers {
                assertTrue(viewModel.uiState.value.tr069RetryLoading)
                assertFalse(viewModel.uiState.value.isLoading)
                assertTrue(
                    viewModel.uiState.value.registrationProgressMessage.contains(
                        "Reintentando",
                        ignoreCase = true,
                    )
                )
                Result.success(
                    RegistrationProgress(
                        subscriptionId = 2378,
                        step = "DONE",
                        message = "Listo",
                        done = true,
                        tr069ProvisionStatus = "COMPLETE",
                        subscription = Subscription(
                            subscriptionId = 2378,
                            tr069ProvisionStatus = "COMPLETE",
                        ),
                    )
                )
            }

            viewModel.onIntent(
                RegisterSubscriptionIntent.RetryTr069(
                    Subscription(
                        subscriptionId = 2378,
                        tr069ProvisionStatus = "PENDING",
                        wifiPassword24 = "clave-wifi-24",
                    )
                )
            )
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.tr069RetryLoading)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `retryTr069 PENDING polls until COMPLETE and preserves wifi passwords`() =
        runTest(testDispatcher) {
            val events = mutableListOf<RegisterSubscriptionUiEvent>()
            val job = launch { viewModel.uiEvent.collect { events.add(it) } }

            coEvery { retryTr069ProvisioningUseCase(2378) } returns Result.success(
                Subscription(
                    subscriptionId = 2378,
                    tr069ProvisionStatus = "PENDING",
                    tr069Message = "Esperando aprovisionamiento TR-069.",
                    wifiSsid24 = "ARLENI",
                )
            )
            coEvery { pollRegistrationProgressUseCase(2378, any()) } returns Result.success(
                RegistrationProgress(
                    subscriptionId = 2378,
                    step = "DONE",
                    message = "Listo",
                    done = true,
                    tr069ProvisionStatus = "COMPLETE",
                    subscription = Subscription(
                        subscriptionId = 2378,
                        tr069ProvisionStatus = "COMPLETE",
                        wifiSsid24 = "ARLENI",
                    ),
                )
            )

            viewModel.onIntent(
                RegisterSubscriptionIntent.RetryTr069(
                    Subscription(
                        subscriptionId = 2378,
                        tr069ProvisionStatus = "PENDING",
                        wifiSsid24 = "ARLENI",
                        wifiPassword24 = "clave-wifi-24",
                        wifiPassword5 = "clave-wifi-5g",
                    )
                )
            )
            advanceUntilIdle()

            coVerify(exactly = 1) { retryTr069ProvisioningUseCase(2378) }
            coVerify(exactly = 1) { pollRegistrationProgressUseCase(2378, any()) }
            val success = events.filterIsInstance<RegisterSubscriptionUiEvent.Success>().last()
            assertEquals("COMPLETE", success.subscription.tr069ProvisionStatus)
            assertEquals("clave-wifi-24", success.subscription.wifiPassword24)
            assertEquals("clave-wifi-5g", success.subscription.wifiPassword5)
            assertFalse(viewModel.uiState.value.tr069RetryLoading)
            assertFalse(viewModel.uiState.value.isLoading)
            job.cancel()
        }

    @Test
    fun `saveSubscription polls registration progress until COMPLETE without Error`() =
        runTest(testDispatcher) {
            val nap = NapBoxResponse(id = "n1", placeName = "P1", placeId = 1)
            val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
            coEvery { getRegistrationCatalogUseCase() } returns Result.success(
                sampleCatalog(napBoxes = listOf(fiberNap()), onus = listOf(fiberOnu()))
            )
            viewModel.loadScreenData(null)
            advanceUntilIdle()

            val pending = Subscription(
                subscriptionId = 1,
                firstName = "A",
                lastName = "B",
                tr069ProvisionStatus = "PENDING",
                provisioningPending = true,
            )
            val complete = pending.copy(
                tr069ProvisionStatus = "COMPLETE",
                provisioningPending = false,
            )
            coEvery {
                registerSubscriptionUseCase(any(), any(), facadePhotoFile = any())
            } returns Result.success(RegisterSubscriptionResult.Registered(pending))
            coEvery { pollRegistrationProgressUseCase(1, any()) } coAnswers {
                assertTrue(viewModel.uiState.value.isLoading)
                Result.success(
                    RegistrationProgress(
                        subscriptionId = 1,
                        step = "DONE",
                        message = "Listo",
                        done = true,
                        tr069ProvisionStatus = "COMPLETE",
                        subscription = complete,
                    )
                )
            }

            fillValidFiberForm(nap, onu)
            fillWifiFields()

            val events = mutableListOf<RegisterSubscriptionUiEvent>()
            val job = launch { viewModel.uiEvent.collect { events.add(it) } }

            viewModel.saveSubscription(facadePhotoFile)
            advanceUntilIdle()

            coVerify(exactly = 1) { pollRegistrationProgressUseCase(1, any()) }
            coVerify(exactly = 0) { retryTr069ProvisioningUseCase(any()) }
            assertEquals(1, events.size)
            val success = events[0] as RegisterSubscriptionUiEvent.Success
            assertEquals("COMPLETE", success.subscription.tr069ProvisionStatus)
            assertTrue(events.none { it is RegisterSubscriptionUiEvent.Error })
            assertEquals(false, viewModel.uiState.value.isLoading)
            job.cancel()
        }

    @Test
    fun `saveSubscription poll timeout keeps the registered subscription as provisioning unknown`() = runTest(testDispatcher) {
        val nap = NapBoxResponse(id = "n1", placeName = "P1", placeId = 1)
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(napBoxes = listOf(fiberNap()), onus = listOf(fiberOnu()))
        )
        viewModel.loadScreenData(null)
        advanceUntilIdle()

        val pending = Subscription(
            subscriptionId = 1,
            firstName = "A",
            lastName = "B",
            tr069ProvisionStatus = "PENDING",
            provisioningPending = true,
            tr069Message = "Los SSIDs no se confirmaron en el ACS dentro del tiempo de espera.",
        )
        coEvery {
            registerSubscriptionUseCase(any(), any(), facadePhotoFile = any())
        } returns Result.success(RegisterSubscriptionResult.Registered(pending))
        coEvery { pollRegistrationProgressUseCase(1, any()) } returns Result.failure(
            IllegalStateException("Timeout esperando aprovisionamiento")
        )

        fillValidFiberForm(nap, onu)
        fillWifiFields()
        selectValidLocation()

        val events = mutableListOf<RegisterSubscriptionUiEvent>()
        val job = launch { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.saveSubscription(facadePhotoFile)
        advanceUntilIdle()

        coVerify(exactly = 1) { pollRegistrationProgressUseCase(1, any()) }
        coVerify(exactly = 0) { retryTr069ProvisioningUseCase(any()) }
        assertTrue(events.any { it is RegisterSubscriptionUiEvent.Success })
        assertTrue(events.none { it is RegisterSubscriptionUiEvent.Error })
        val submission = viewModel.uiState.value.submission
        assertTrue(submission is SubmissionState.ProvisioningUnknown)
        assertTrue((submission as SubmissionState.ProvisioningUnknown).message.contains("#1"))
        assertEquals(false, viewModel.uiState.value.isLoading)
        job.cancel()
    }

    @Test
    fun `saveSubscription MANUAL_REQUIRED does not auto retry TR-069`() = runTest(testDispatcher) {
        val nap = NapBoxResponse(id = "n1", placeName = "P1", placeId = 1)
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(napBoxes = listOf(fiberNap()), onus = listOf(fiberOnu()))
        )
        viewModel.loadScreenData(null)
        advanceUntilIdle()

        val manual = Subscription(
            subscriptionId = 1,
            firstName = "A",
            lastName = "B",
            tr069ProvisionStatus = "MANUAL_REQUIRED",
            tr069Message = "ONU no contactó al ACS",
        )
        coEvery {
            registerSubscriptionUseCase(any(), any(), facadePhotoFile = any())
        } returns Result.success(RegisterSubscriptionResult.Registered(manual))

        fillValidFiberForm(nap, onu)
        fillWifiFields()
        selectValidLocation()

        val events = mutableListOf<RegisterSubscriptionUiEvent>()
        val job = launch { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.saveSubscription(facadePhotoFile)
        advanceUntilIdle()

        coVerify(exactly = 0) { retryTr069ProvisioningUseCase(any()) }
        coVerify(exactly = 0) { pollRegistrationProgressUseCase(any(), any()) }
        assertEquals(1, events.size)
        assertEquals(
            "MANUAL_REQUIRED",
            (events[0] as RegisterSubscriptionUiEvent.Success).subscription.tr069ProvisionStatus
        )
        assertTrue(events.none { it is RegisterSubscriptionUiEvent.Error })
        job.cancel()
    }

    private fun fillWifiFields() {
        viewModel.onIntent(RegisterSubscriptionIntent.WifiSsid24Changed("CasaFibra24"))
        viewModel.onIntent(RegisterSubscriptionIntent.WifiPassword24Changed("clave24xx"))
    }

    private fun fillSplitWifiFields() {
        viewModel.onIntent(RegisterSubscriptionIntent.UseDifferentWifiNamesChanged(true))
        viewModel.onIntent(RegisterSubscriptionIntent.WifiSsid24Changed("CasaFibra24"))
        viewModel.onIntent(RegisterSubscriptionIntent.WifiPassword24Changed("clave24xx"))
        viewModel.onIntent(RegisterSubscriptionIntent.WifiSsid5Changed("CasaFibra5"))
    }

    @Test
    fun `UseCurrentLocationClicked sets current method and requests GPS`() = runTest(testDispatcher) {
        val events = mutableListOf<RegisterSubscriptionUiEvent>()
        val job = launch { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.onIntent(RegisterSubscriptionIntent.UseCurrentLocationClicked)
        advanceUntilIdle()

        assertEquals(
            LocationCaptureMethod.CURRENT,
            viewModel.uiState.value.registerSubscriptionForm.locationCaptureMethod
        )
        assertTrue(events.contains(RegisterSubscriptionUiEvent.RequestCurrentLocation))
        job.cancel()
    }

    @Test
    fun `ChooseManualLocationClicked opens map picker`() = runTest(testDispatcher) {
        viewModel.onIntent(RegisterSubscriptionIntent.ChooseManualLocationClicked)
        advanceUntilIdle()

        assertEquals(
            LocationCaptureMethod.MANUAL,
            viewModel.uiState.value.registerSubscriptionForm.locationCaptureMethod
        )
        assertTrue(viewModel.uiState.value.showManualLocationMap)
    }

    @Test
    fun `LocationCoordinatesSelected stores coords and preselects nearest nap`() = runTest(testDispatcher) {
        val nearer = NapBoxResponse(id = "near", placeName = "P", placeId = 1)
        val farther = NapBoxResponse(id = "far", placeName = "P", placeId = 1)
        coEvery { getPlaceFromLocationUseCase(-12.0, -77.0) } returns Result.success(
            Place(id = "1", name = "P")
        )
        coEvery { getNearNapBoxesUseCase(-12.0, -77.0) } returns Result.success(
            listOf(nearer, farther)
        )

        viewModel.loadScreenData(null)
        advanceUntilIdle()
        viewModel.onIntent(RegisterSubscriptionIntent.LocationCoordinatesSelected(-12.0, -77.0))
        advanceUntilIdle()

        val form = viewModel.uiState.value.registerSubscriptionForm
        assertEquals(-12.0, form.location!!.latitude, 0.0)
        assertEquals(-77.0, form.location!!.longitude, 0.0)
        assertEquals(nearer, form.selectedNapBox)
        assertFalse(viewModel.uiState.value.showManualLocationMap)
    }

    private fun fillValidFiberForm(nap: NapBoxResponse, onu: Onu) {
        viewModel.onIntent(RegisterSubscriptionIntent.FirstNameChanged("Juan"))
        viewModel.onIntent(RegisterSubscriptionIntent.LastNameChanged("Perez"))
        viewModel.onIntent(RegisterSubscriptionIntent.DniChanged("12345678"))
        viewModel.onIntent(RegisterSubscriptionIntent.AddressChanged("Calle larga 12345"))
        viewModel.onIntent(RegisterSubscriptionIntent.PhoneChanged("987654321"))
        viewModel.onIntent(RegisterSubscriptionIntent.PlanSelected(samplePlan))
        viewModel.onIntent(RegisterSubscriptionIntent.PlaceSelected(Place(id = "1", name = "P")))
        viewModel.onIntent(RegisterSubscriptionIntent.NapBoxSelected(nap))
        viewModel.onIntent(RegisterSubscriptionIntent.OnuSelected(onu))
        selectValidLocation()
    }

    private fun selectValidLocation() {
        viewModel.onLocationChanged(
            com.google.android.gms.maps.model.LatLng(-12.0, -77.0)
        )
    }

    @Test
    fun `WizardContinue stays on step 1 when client and location are invalid`() = runTest(testDispatcher) {
        viewModel.onIntent(RegisterSubscriptionIntent.WizardContinueClicked)
        advanceUntilIdle()

        assertEquals(
            com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep.CLIENT_LOCATION,
            viewModel.uiState.value.wizardStep
        )
        assertNotNull(viewModel.uiState.value.registerSubscriptionForm.firstNameError)
        assertNotNull(viewModel.uiState.value.registerSubscriptionForm.locationError)
    }

    @Test
    fun `WizardContinue advances to installation when step 1 is valid`() = runTest(testDispatcher) {
        viewModel.loadScreenData(null)
        advanceUntilIdle()
        fillStep1()
        advanceUntilIdle()

        viewModel.onIntent(RegisterSubscriptionIntent.WizardContinueClicked)
        advanceUntilIdle()

        assertEquals(
            com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep.INSTALLATION,
            viewModel.uiState.value.wizardStep
        )
        assertEquals("JUAN", viewModel.uiState.value.registerSubscriptionForm.firstName)
    }

    @Test
    fun `WizardBack returns to previous step without clearing form`() = runTest(testDispatcher) {
        viewModel.loadScreenData(null)
        advanceUntilIdle()
        fillStep1()
        advanceUntilIdle()
        viewModel.onIntent(RegisterSubscriptionIntent.WizardContinueClicked)
        advanceUntilIdle()

        viewModel.onIntent(RegisterSubscriptionIntent.WizardBackClicked)
        advanceUntilIdle()

        assertEquals(
            com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep.CLIENT_LOCATION,
            viewModel.uiState.value.wizardStep
        )
        assertEquals("JUAN", viewModel.uiState.value.registerSubscriptionForm.firstName)
        assertEquals(-12.0, viewModel.uiState.value.registerSubscriptionForm.location!!.latitude, 0.0)
    }

    @Test
    fun `WizardContinue stays on installation when fiber requirements are missing`() = runTest(testDispatcher) {
        viewModel.loadScreenData(null)
        advanceUntilIdle()
        fillStep1()
        advanceUntilIdle()
        viewModel.onIntent(RegisterSubscriptionIntent.WizardContinueClicked)
        advanceUntilIdle()

        viewModel.onIntent(RegisterSubscriptionIntent.WizardContinueClicked)
        advanceUntilIdle()

        assertEquals(
            com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep.INSTALLATION,
            viewModel.uiState.value.wizardStep
        )
    }

    @Test
    fun `WizardContinue advances to confirmation when fiber installation is valid`() =
        runTest(testDispatcher) {
            val nap = NapBoxResponse(id = "n1", placeName = "P1", placeId = 1)
            val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
            coEvery { getRegistrationCatalogUseCase() } returns Result.success(
                sampleCatalog(napBoxes = listOf(fiberNap()), onus = listOf(fiberOnu()))
            )
            viewModel.loadScreenData(null)
            advanceUntilIdle()
            fillValidFiberForm(nap, onu)
            fillWifiFields()
            advanceUntilIdle()

            viewModel.onIntent(RegisterSubscriptionIntent.WizardContinueClicked)
            advanceUntilIdle()
            viewModel.onIntent(RegisterSubscriptionIntent.WizardContinueClicked)
            advanceUntilIdle()

            assertEquals(
                com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep.CONFIRMATION,
                viewModel.uiState.value.wizardStep
            )
            assertEquals("JUAN", viewModel.uiState.value.registerSubscriptionForm.firstName)
            assertEquals(nap.id, viewModel.uiState.value.registerSubscriptionForm.selectedNapBox?.id)
        }

    @Test
    fun `zero coordinates do not allow leaving step 1`() = runTest(testDispatcher) {
        viewModel.loadScreenData(null)
        advanceUntilIdle()
        viewModel.onIntent(RegisterSubscriptionIntent.FirstNameChanged("Juan"))
        viewModel.onIntent(RegisterSubscriptionIntent.LastNameChanged("Perez"))
        viewModel.onIntent(RegisterSubscriptionIntent.DniChanged("12345678"))
        viewModel.onIntent(RegisterSubscriptionIntent.AddressChanged("Calle larga 12345"))
        viewModel.onIntent(RegisterSubscriptionIntent.PhoneChanged("987654321"))
        viewModel.onIntent(RegisterSubscriptionIntent.PlaceSelected(Place(id = "1", name = "P")))
        coEvery { getPlaceFromLocationUseCase(any(), any()) } returns Result.success(
            Place(id = "1", name = "P")
        )
        coEvery { getNearNapBoxesUseCase(any(), any()) } returns Result.success(emptyList())
        viewModel.onIntent(RegisterSubscriptionIntent.LocationCoordinatesSelected(0.0, 0.0))
        advanceUntilIdle()

        viewModel.onIntent(RegisterSubscriptionIntent.WizardContinueClicked)
        advanceUntilIdle()

        assertEquals(
            com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep.CLIENT_LOCATION,
            viewModel.uiState.value.wizardStep
        )
        assertNotNull(viewModel.uiState.value.registerSubscriptionForm.locationError)
    }

    private fun fillStep1() {
        viewModel.onIntent(RegisterSubscriptionIntent.FirstNameChanged("Juan"))
        viewModel.onIntent(RegisterSubscriptionIntent.LastNameChanged("Perez"))
        viewModel.onIntent(RegisterSubscriptionIntent.DniChanged("12345678"))
        viewModel.onIntent(RegisterSubscriptionIntent.AddressChanged("Calle larga 12345"))
        viewModel.onIntent(RegisterSubscriptionIntent.PhoneChanged("987654321"))
        viewModel.onIntent(RegisterSubscriptionIntent.PlaceSelected(Place(id = "1", name = "P")))
        selectValidLocation()
    }

    private fun fillValidTvCustomerFields() {
        viewModel.onIntent(RegisterSubscriptionIntent.FirstNameChanged("Juan"))
        viewModel.onIntent(RegisterSubscriptionIntent.LastNameChanged("Perez"))
        viewModel.onIntent(RegisterSubscriptionIntent.DniChanged("12345678"))
        viewModel.onIntent(RegisterSubscriptionIntent.AddressChanged("Calle larga 12345"))
        viewModel.onIntent(RegisterSubscriptionIntent.PhoneChanged("987654321"))
        viewModel.onIntent(RegisterSubscriptionIntent.PlanSelected(sampleTvPlan))
        viewModel.onIntent(RegisterSubscriptionIntent.PlaceSelected(Place(id = "1", name = "P")))
        selectValidLocation()
    }

    private val sampleTvPlan = PlanResponse(
        id = "tv1",
        name = "TV Cable",
        price = 20.0,
        downloadSpeed = "0",
        uploadSpeed = "0",
        type = InstallationType.ONLY_TV_FIBER
    )

    private fun catalogWithTv() = sampleCatalog(
        plans = listOf(
            CatalogPlan(
                id = "p1",
                name = "Plan",
                price = 10.0,
                downloadSpeed = "100",
                uploadSpeed = "100",
                type = "FIBER"
            ),
            CatalogPlan(
                id = "tv1",
                name = "TV Cable",
                price = 20.0,
                downloadSpeed = "0",
                uploadSpeed = "0",
                type = "ONLY_TV_FIBER"
            )
        ),
        napBoxes = listOf(fiberNap()),
        onus = listOf(fiberOnu())
    )

    @Test
    fun `reopening a linked registration resumes progress polling and shows the result`() = runTest(testDispatcher) {
        val operationUseCase = mockk<OnuRegistrationOperationUseCase>()
        val linked = OnuRegistrationOperation(
            id = "linked-resume", serial = "VSOL0031C0B6", subscriptionId = 84,
            phase = "PROVISIONING", state = "RUNNING", revision = 5,
        )
        coEvery { operationUseCase.active() } returns linked
        coEvery { operationUseCase.get(linked.id) } returns linked
        val done = Subscription(subscriptionId = 84, tr069ProvisionStatus = "COMPLETE", provisioningPending = false)
        coEvery { pollRegistrationProgressUseCase(84, any()) } returns Result.success(
            RegistrationProgress(
                subscriptionId = 84, step = "DONE", message = "Listo", done = true,
                tr069ProvisionStatus = "COMPLETE", subscription = done, outcome = "SUCCEEDED",
            )
        )
        viewModel = viewModelWithPreauthorization(operationUseCase)

        viewModel.loadScreenData(null)
        advanceUntilIdle()

        coVerify(exactly = 1) { pollRegistrationProgressUseCase(84, any()) }
        val submission = viewModel.uiState.value.submission
        assertTrue(submission is SubmissionState.Completed)
        assertEquals(84, (submission as SubmissionState.Completed).subscription.resolvedSubscriptionId())
        assertFalse(viewModel.uiState.value.isRegistering)
    }

    @Test
    fun `registration result lives in ui state even without an event collector`() = runTest(testDispatcher) {
        val nap = NapBoxResponse(id = "n1", placeName = "P1", placeId = 1)
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(napBoxes = listOf(fiberNap()), onus = listOf(fiberOnu()))
        )
        viewModel.loadScreenData(null)
        advanceUntilIdle()
        val done = Subscription(subscriptionId = 1, firstName = "A", lastName = "B", tr069ProvisionStatus = "COMPLETE")
        coEvery { registerSubscriptionUseCase(any(), any(), facadePhotoFile = any()) } returns
            Result.success(RegisterSubscriptionResult.Registered(done))
        fillValidFiberForm(nap, onu)
        fillWifiFields()
        selectValidLocation()

        viewModel.saveSubscription(facadePhotoFile)
        advanceUntilIdle()

        val submission = viewModel.uiState.value.submission
        assertTrue(submission is SubmissionState.Completed)
        viewModel.onIntent(RegisterSubscriptionIntent.DismissRegistrationResult)
        assertEquals(SubmissionState.Idle, viewModel.uiState.value.submission)
    }

    @Test
    fun `failed provisioning outcome needs attention and reports the reason`() = runTest(testDispatcher) {
        val nap = NapBoxResponse(id = "n1", placeName = "P1", placeId = 1)
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(napBoxes = listOf(fiberNap()), onus = listOf(fiberOnu()))
        )
        viewModel.loadScreenData(null)
        advanceUntilIdle()
        val pending = Subscription(subscriptionId = 1, firstName = "A", lastName = "B", tr069ProvisionStatus = "PENDING", provisioningPending = true)
        coEvery { registerSubscriptionUseCase(any(), any(), facadePhotoFile = any()) } returns
            Result.success(RegisterSubscriptionResult.Registered(pending))
        coEvery { pollRegistrationProgressUseCase(1, any()) } returns Result.success(
            RegistrationProgress(
                subscriptionId = 1, step = "FAILED", message = "El ACS no confirmó los SSID.", done = true,
                tr069ProvisionStatus = "FAILED", outcome = "FAILED",
                subscription = pending.copy(tr069ProvisionStatus = "FAILED", provisioningPending = false),
            )
        )
        fillValidFiberForm(nap, onu)
        fillWifiFields()
        selectValidLocation()
        val events = mutableListOf<RegisterSubscriptionUiEvent>()
        val job = launch { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.saveSubscription(facadePhotoFile)
        advanceUntilIdle()

        val submission = viewModel.uiState.value.submission
        assertTrue(submission is SubmissionState.NeedsAttention)
        assertEquals("El ACS no confirmó los SSID.", (submission as SubmissionState.NeedsAttention).message)
        assertTrue(events.any { it is RegisterSubscriptionUiEvent.Error })
        job.cancel()
    }

    @Test
    fun `network failure after submit reconciles with the server outcome instead of failing`() = runTest(testDispatcher) {
        val operationUseCase = mockk<OnuRegistrationOperationUseCase>()
        val onu = Onu("b", "olt", "1", "t", "VSOLVA74", "gpon", "p", "VSOL0031C0B6")
        val ready = OnuRegistrationOperation(
            id = "op-lost-response", serial = onu.sn, subscriptionId = null,
            phase = "READY_FOR_FORM", state = "READY_FOR_FORM", revision = 4,
        )
        coEvery { operationUseCase.active() } returns ready
        coEvery { operationUseCase.draft(ready.id) } returns mapOf("facadePhotoUrl" to "https://storage.example/photo.jpg")
        coEvery { operationUseCase.saveDraft(any(), any()) } returns Unit
        coEvery { operationUseCase.outcome(ready.id) } returns com.dscorp.ispadmin.domain.model.OnuRegistrationOutcome(
            operationId = ready.id, subscriptionId = 90, phase = "PROVISIONING", state = "RUNNING", outcome = "RUNNING",
        )
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(napBoxes = listOf(fiberNap()), onus = listOf(fiberOnu().copy(sn = onu.sn)))
        )
        coEvery { registerSubscriptionUseCase(any(), any(), facadePhotoFile = any()) } returns
            Result.failure(java.net.SocketTimeoutException("timeout"))
        val done = Subscription(subscriptionId = 90, tr069ProvisionStatus = "COMPLETE")
        coEvery { pollRegistrationProgressUseCase(90, any()) } returns Result.success(
            RegistrationProgress(90, "DONE", "Listo", true, tr069ProvisionStatus = "COMPLETE", subscription = done, outcome = "SUCCEEDED")
        )
        viewModel = viewModelWithPreauthorization(operationUseCase)
        viewModel.loadScreenData(null)
        advanceUntilIdle()
        fillValidFiberForm(NapBoxResponse(id = "n1", placeName = "P1", placeId = 1), onu)
        fillWifiFields()
        val events = mutableListOf<RegisterSubscriptionUiEvent>()
        val job = launch { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.saveSubscription(facadePhotoFile)
        advanceUntilIdle()

        coVerify(exactly = 1) { operationUseCase.outcome(ready.id) }
        coVerify(exactly = 1) { pollRegistrationProgressUseCase(90, any()) }
        assertTrue(viewModel.uiState.value.submission is SubmissionState.Completed)
        assertTrue(events.none { it is RegisterSubscriptionUiEvent.Error })
        job.cancel()
    }

    @Test
    fun `typing a dni that already has active subscriptions shows a non blocking warning`() = runTest(testDispatcher) {
        val actions = mockk<com.dscorp.ispadmin.domain.repository.SubscriptionActionsRepository>()
        coEvery { actions.checkDni("12345678") } returns com.dscorp.ispadmin.domain.model.DniCheck(
            totalSubscriptions = 3, activeSubscriptions = 2,
        )
        viewModel = RegisterSubscriptionComposeViewModel(
            getAvailableOnuListUseCase = getAvailableOnuListUseCase,
            getRegistrationCatalogUseCase = getRegistrationCatalogUseCase,
            refreshRegistrationCatalogUseCase = refreshRegistrationCatalogUseCase,
            getPlaceFromLocationUseCase = getPlaceFromLocationUseCase,
            registerSubscriptionUseCase = registerSubscriptionUseCase,
            getUserSessionUseCase = getUserSessionUseCase,
            getNearNapBoxesUseCase = getNearNapBoxesUseCase,
            installationOrderUseCase = installationOrderUseCase,
            observeOfflineRegistrationModeUseCase = observeOfflineRegistrationModeUseCase,
            retryTr069ProvisioningUseCase = retryTr069ProvisioningUseCase,
            pollRegistrationProgressUseCase = pollRegistrationProgressUseCase,
            observabilityClient = mockk(relaxed = true),
            mainImmediate = testDispatcher,
            subscriptionActionsRepository = actions,
        )

        viewModel.onIntent(RegisterSubscriptionIntent.DniChanged("1234567"))
        viewModel.onIntent(RegisterSubscriptionIntent.DniChanged("12345678"))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.dniWarning?.contains("2") == true)
        coVerify(exactly = 1) { actions.checkDni("12345678") }
        viewModel.onIntent(RegisterSubscriptionIntent.DniChanged("1234567"))
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.dniWarning)
    }

    @Test
    fun `registration already exists conflict is reconciled as an accepted registration`() = runTest(testDispatcher) {
        val operationUseCase = mockk<OnuRegistrationOperationUseCase>()
        val onu = Onu("b", "olt", "1", "t", "VSOLVA74", "gpon", "p", "VSOL0031C0B6")
        val ready = OnuRegistrationOperation(
            id = "op-conflict", serial = onu.sn, subscriptionId = null,
            phase = "READY_FOR_FORM", state = "READY_FOR_FORM", revision = 4,
        )
        coEvery { operationUseCase.active() } returns ready
        coEvery { operationUseCase.draft(ready.id) } returns mapOf("facadePhotoUrl" to "https://storage.example/photo.jpg")
        coEvery { operationUseCase.saveDraft(any(), any()) } returns Unit
        coEvery { operationUseCase.outcome(ready.id) } returns com.dscorp.ispadmin.domain.model.OnuRegistrationOutcome(
            operationId = ready.id, subscriptionId = 91, phase = "PROVISIONING", state = "SUCCEEDED", outcome = "SUCCEEDED",
        )
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(napBoxes = listOf(fiberNap()), onus = listOf(fiberOnu().copy(sn = onu.sn)))
        )
        coEvery { registerSubscriptionUseCase(any(), any(), facadePhotoFile = any()) } returns Result.failure(
            com.dscorp.ispadmin.domain.model.RegistrationConflictException("REGISTRATION_ALREADY_EXISTS", "ya procesada")
        )
        coEvery { pollRegistrationProgressUseCase(91, any()) } returns Result.success(
            RegistrationProgress(91, "DONE", "Listo", true, tr069ProvisionStatus = "COMPLETE",
                subscription = Subscription(subscriptionId = 91, tr069ProvisionStatus = "COMPLETE"), outcome = "SUCCEEDED")
        )
        viewModel = viewModelWithPreauthorization(operationUseCase)
        viewModel.loadScreenData(null)
        advanceUntilIdle()
        fillValidFiberForm(NapBoxResponse(id = "n1", placeName = "P1", placeId = 1), onu)
        fillWifiFields()

        viewModel.saveSubscription(facadePhotoFile)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.submission is SubmissionState.Completed)
    }

    @Test
    fun `wireless retries after an uncertain failure reuse the same clientRequestId`() = runTest(testDispatcher) {
        val handle = androidx.lifecycle.SavedStateHandle()
        viewModel = RegisterSubscriptionComposeViewModel(
            getAvailableOnuListUseCase = getAvailableOnuListUseCase,
            getRegistrationCatalogUseCase = getRegistrationCatalogUseCase,
            refreshRegistrationCatalogUseCase = refreshRegistrationCatalogUseCase,
            getPlaceFromLocationUseCase = getPlaceFromLocationUseCase,
            registerSubscriptionUseCase = registerSubscriptionUseCase,
            getUserSessionUseCase = getUserSessionUseCase,
            getNearNapBoxesUseCase = getNearNapBoxesUseCase,
            installationOrderUseCase = installationOrderUseCase,
            observeOfflineRegistrationModeUseCase = observeOfflineRegistrationModeUseCase,
            retryTr069ProvisioningUseCase = retryTr069ProvisioningUseCase,
            pollRegistrationProgressUseCase = pollRegistrationProgressUseCase,
            observabilityClient = mockk(relaxed = true),
            mainImmediate = testDispatcher,
            savedStateHandle = handle,
        )
        val nap = NapBoxResponse(id = "n1", placeName = "P1", placeId = 1)
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(napBoxes = listOf(fiberNap()), onus = listOf(fiberOnu()))
        )
        viewModel.loadScreenData(null)
        advanceUntilIdle()
        val sent = mutableListOf<Subscription>()
        coEvery { registerSubscriptionUseCase(capture(sent), any(), facadePhotoFile = any()) } returns
            Result.failure(java.net.SocketTimeoutException("timeout"))
        fillValidFiberForm(nap, onu)
        fillWifiFields()
        selectValidLocation()

        viewModel.saveSubscription(facadePhotoFile)
        advanceUntilIdle()
        viewModel.saveSubscription(facadePhotoFile)
        advanceUntilIdle()

        assertEquals(2, sent.size)
        assertNotNull(sent[0].clientRequestId)
        assertEquals(sent[0].clientRequestId, sent[1].clientRequestId)
        assertTrue(viewModel.uiState.value.submission is SubmissionState.Uncertain)
    }

    @Test
    fun `registration workflow is keyed by the operation id and closed with the outcome`() = runTest(testDispatcher) {
        val obs = mockk<com.dscorp.ispadmin.observability.ObservabilityClient>(relaxed = true)
        val operationUseCase = mockk<OnuRegistrationOperationUseCase>()
        val linked = OnuRegistrationOperation(
            id = "op-workflow", serial = "VSOL0031C0B6", subscriptionId = 85,
            phase = "PROVISIONING", state = "RUNNING", revision = 5,
        )
        coEvery { operationUseCase.active() } returns linked
        coEvery { pollRegistrationProgressUseCase(85, any()) } returns Result.success(
            RegistrationProgress(85, "DONE", "Listo", true, tr069ProvisionStatus = "COMPLETE",
                subscription = Subscription(subscriptionId = 85, tr069ProvisionStatus = "COMPLETE"), outcome = "SUCCEEDED")
        )
        viewModel = viewModelWithPreauthorization(operationUseCase, observabilityClient = obs)

        viewModel.loadScreenData(null)
        advanceUntilIdle()

        io.mockk.verify { obs.startWorkflow(any(), "registration", any(), "op-workflow") }
        io.mockk.verify { obs.endWorkflow(com.dscorp.ispadmin.observability.WorkflowStatus.SUCCESS, any(), any()) }
    }

    @Test
    fun `async preauthorization is followed until the form is ready`() = runTest(testDispatcher) {
        val onu = Onu("1", "olt-lab", "3", "type-1", "VSOLVA74", "gpon", "2", "VSOL0031C0B6")
        val operationUseCase = mockk<OnuRegistrationOperationUseCase>()
        val pending = OnuRegistrationOperation(
            id = "preauth-async", serial = onu.sn, subscriptionId = null,
            phase = "OLT_AUTHORIZATION", state = "PENDING", revision = 1,
        )
        val running = pending.copy(state = "RUNNING", revision = 2)
        val ready = pending.copy(phase = "READY_FOR_FORM", state = "READY_FOR_FORM", revision = 5)
        coEvery { operationUseCase.active() } returns null
        coEvery { operationUseCase.start(any()) } returns pending
        coEvery { operationUseCase.get(pending.id) } returnsMany listOf(running, ready)
        coEvery { operationUseCase.draft(any()) } returns null
        coEvery { operationUseCase.saveDraft(any(), any()) } returns Unit
        coEvery { getRegistrationCatalogUseCase() } returns Result.success(
            sampleCatalog(onus = listOf(fiberOnu().copy(sn = onu.sn, oltId = onu.olt_id, board = onu.board, port = onu.port, ponType = onu.pon_type, onuTypeName = onu.onu_type_name)))
        )
        viewModel = viewModelWithPreauthorization(operationUseCase)
        viewModel.loadScreenData(null)
        advanceUntilIdle()

        viewModel.onIntent(RegisterSubscriptionIntent.OnuSelected(onu))
        viewModel.onIntent(RegisterSubscriptionIntent.WizardContinueClicked)
        advanceUntilIdle()

        coVerify(exactly = 2) { operationUseCase.get(pending.id) }
        assertEquals(ready, viewModel.uiState.value.preauthorizationOperation)
        assertEquals(
            com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep.CLIENT_LOCATION,
            viewModel.uiState.value.wizardStep,
        )
    }
}
