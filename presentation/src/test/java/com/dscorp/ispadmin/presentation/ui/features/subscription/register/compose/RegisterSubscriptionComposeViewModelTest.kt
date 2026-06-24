package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import com.dscorp.ispadmin.domain.model.InstallationOrder
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.usecase.InstallationOrderUseCase
import com.dscorp.ispadmin.domain.usecase.plan.GetPlanListUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetAvailableOnuListUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetCoreDevicesUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetNapBoxListUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetNearNapBoxesUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetPlaceFromLocationUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetPlaceListUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetUserSessionUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.RegisterSubscriptionUseCase
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionIntent
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionUiEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterSubscriptionComposeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getAvailableOnuListUseCase: GetAvailableOnuListUseCase
    private lateinit var getPlanListUseCase: GetPlanListUseCase
    private lateinit var getPlaceListUseCase: GetPlaceListUseCase
    private lateinit var getPlaceFromLocationUseCase: GetPlaceFromLocationUseCase
    private lateinit var getNapBoxListUseCase: GetNapBoxListUseCase
    private lateinit var registerSubscriptionUseCase: RegisterSubscriptionUseCase
    private lateinit var getUserSessionUseCase: GetUserSessionUseCase
    private lateinit var getCoreDevicesUseCase: GetCoreDevicesUseCase
    private lateinit var getNearNapBoxesUseCase: GetNearNapBoxesUseCase
    private lateinit var installationOrderUseCase: InstallationOrderUseCase

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

    private lateinit var facadePhotoFile: File

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        facadePhotoFile = File.createTempFile("facade_test", ".jpg")
        getAvailableOnuListUseCase = mockk()
        getPlanListUseCase = mockk()
        getPlaceListUseCase = mockk()
        getPlaceFromLocationUseCase = mockk()
        getNapBoxListUseCase = mockk()
        registerSubscriptionUseCase = mockk()
        getUserSessionUseCase = mockk()
        getCoreDevicesUseCase = mockk()
        getNearNapBoxesUseCase = mockk()
        installationOrderUseCase = mockk(relaxed = true)

        coEvery { getAvailableOnuListUseCase() } returns Result.success(emptyList())
        coEvery { getPlanListUseCase() } returns Result.success(listOf(samplePlan))
        coEvery { getPlaceListUseCase() } returns Result.success(emptyList())
        coEvery { getNapBoxListUseCase() } returns Result.success(emptyList())
        coEvery { getUserSessionUseCase() } returns Result.success(sampleUser)
        coEvery { getCoreDevicesUseCase() } returns Result.success(emptyList())

        viewModel = RegisterSubscriptionComposeViewModel(
            getAvailableOnuListUseCase = getAvailableOnuListUseCase,
            getPlanListUseCase = getPlanListUseCase,
            getPlaceListUseCase = getPlaceListUseCase,
            getPlaceFromLocationUseCase = getPlaceFromLocationUseCase,
            getNapBoxListUseCase = getNapBoxListUseCase,
            registerSubscriptionUseCase = registerSubscriptionUseCase,
            getUserSessionUseCase = getUserSessionUseCase,
            getCoreDevicesUseCase = getCoreDevicesUseCase,
            getNearNapBoxesUseCase = getNearNapBoxesUseCase,
            installationOrderUseCase = installationOrderUseCase,
            mainImmediate = testDispatcher
        )
    }

    @After
    fun tearDown() {
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
        assertEquals(sampleUser, viewModel.uiState.value.currentUser)
        assertEquals(samplePlan, viewModel.uiState.value.registerSubscriptionForm.selectedPlan)

        job.cancel()
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
        coEvery { getNapBoxListUseCase() } returns Result.success(
            listOf(NapBoxResponse(id = "1", placeName = "Lima", placeId = 5))
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
        coEvery { getAvailableOnuListUseCase() } returnsMany listOf(
            Result.success(emptyList()),
            Result.failure(Exception("onu fail"))
        )

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
        coEvery { getNapBoxListUseCase() } returns Result.success(listOf(nap))
        coEvery { getAvailableOnuListUseCase() } returns Result.success(listOf(onu))

        viewModel.loadScreenData(null)
        advanceUntilIdle()

        val registered = Subscription(subscriptionId = 1, firstName = "A", lastName = "B")
        coEvery {
            registerSubscriptionUseCase(any(), any(), facadePhotoFile = any())
        } answers {
            assertEquals(true, firstArg<Subscription>().autoCut)
            Result.success(registered)
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

        val events = mutableListOf<RegisterSubscriptionUiEvent>()
        val job = launch {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.saveSubscription(facadePhotoFile)
        advanceUntilIdle()

        assertEquals(1, events.size)
        val success = events[0] as RegisterSubscriptionUiEvent.Success
        assertEquals(registered, success.subscription)
        assertEquals(false, viewModel.uiState.value.isLoading)

        job.cancel()
    }

    @Test
    fun `saveSubscription emits error when register fails`() = runTest(testDispatcher) {
        val nap = NapBoxResponse(id = "n1", placeName = "P1", placeId = 1)
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        coEvery { getNapBoxListUseCase() } returns Result.success(listOf(nap))
        coEvery { getAvailableOnuListUseCase() } returns Result.success(listOf(onu))

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
    fun `saveSubscription ignores second call while first is in progress`() = runTest(testDispatcher) {
        val nap = NapBoxResponse(id = "n1", placeName = "P1", placeId = 1)
        val onu = Onu("b", "olt", "1", "t", "type", "pon", "p", "sn1")
        coEvery { getNapBoxListUseCase() } returns Result.success(listOf(nap))
        coEvery { getAvailableOnuListUseCase() } returns Result.success(listOf(onu))

        viewModel.loadScreenData(null)
        advanceUntilIdle()

        val registered = Subscription(subscriptionId = 1, firstName = "A", lastName = "B")
        coEvery { registerSubscriptionUseCase(any(), any(), facadePhotoFile = any()) } coAnswers {
            delay(100)
            Result.success(registered)
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

        viewModel.saveSubscription(facadePhotoFile)
        viewModel.saveSubscription(facadePhotoFile)
        advanceUntilIdle()

        coVerify(exactly = 1) { registerSubscriptionUseCase(any(), any(), facadePhotoFile = any()) }
    }
}
