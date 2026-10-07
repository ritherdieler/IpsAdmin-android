package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import com.dscorp.ispadmin.domain.model.CatalogCoreDevice
import com.dscorp.ispadmin.domain.model.CatalogPlan
import com.dscorp.ispadmin.domain.model.CustomerDetail
import com.dscorp.ispadmin.domain.model.CustomerServiceSummary
import com.dscorp.ispadmin.domain.model.RegistrationCatalog
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.repository.SubscriptionActionsRepository
import com.dscorp.ispadmin.domain.usecase.InstallationOrderUseCase
import com.dscorp.ispadmin.domain.usecase.catalog.GetRegistrationCatalogUseCase
import com.dscorp.ispadmin.domain.usecase.catalog.RefreshRegistrationCatalogUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetAvailableOnuListUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetNearNapBoxesUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetPlaceFromLocationUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.GetUserSessionUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.PollRegistrationProgressUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.RegisterSubscriptionUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.RetryTr069ProvisioningUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.ObserveOfflineRegistrationModeUseCase
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionIntent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterExistingCustomerViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val actions = mockk<SubscriptionActionsRepository>()

    private val customer = CustomerDetail(
        id = 40,
        firstName = "Ana",
        lastName = "Perez",
        dni = "45678912",
        subscriptions = listOf(CustomerServiceSummary(id = 10, address = "Casa", phone = "111")),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads the existing customer and keeps address and phone empty`() = runTest(dispatcher) {
        coEvery { actions.getCustomer(40) } returns customer
        val viewModel = viewModel()

        viewModel.loadScreenData(installationOrderId = null, customerId = 40)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val form = state.registerSubscriptionForm
        assertEquals("ANA", form.firstName)
        assertEquals("PEREZ", form.lastName)
        assertEquals("45678912", form.dni)
        assertEquals("", form.address)
        assertEquals("", form.phone)
        assertTrue(state.identityLocked)
        assertEquals(40, state.customerId)
        assertEquals(2, state.customerServiceNumber)
    }

    @Test
    fun `locked identity ignores name edits and the draft keeps the customer id`() = runTest(dispatcher) {
        coEvery { actions.getCustomer(40) } returns customer
        val viewModel = viewModel()
        viewModel.loadScreenData(installationOrderId = null, customerId = 40)
        advanceUntilIdle()

        viewModel.onIntent(RegisterSubscriptionIntent.FirstNameChanged("OTRA"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("ANA", state.registerSubscriptionForm.firstName)
        assertEquals(40, viewModel.buildRegistrationDraft(state)["customerId"])
    }

    private fun viewModel(): RegisterSubscriptionComposeViewModel {
        val refresh = mockk<RefreshRegistrationCatalogUseCase>()
        val getCatalog = mockk<GetRegistrationCatalogUseCase>()
        coEvery { refresh() } returns Result.success(Unit)
        coEvery { getCatalog() } returns Result.success(
            RegistrationCatalog(
                plans = listOf(CatalogPlan("p1", "Plan", 10.0, "100", "100", "FIBER")),
                places = emptyList(),
                napBoxes = emptyList(),
                onus = emptyList(),
                coreDevices = listOf(CatalogCoreDevice(id = 10, name = "Core-A", disabled = false)),
            )
        )
        val onus = mockk<GetAvailableOnuListUseCase>()
        coEvery { onus() } returns Result.success(emptyList())
        val session = mockk<GetUserSessionUseCase>()
        coEvery { session() } returns Result.success(User(id = 1, name = "T", lastName = "U"))
        val offline = mockk<ObserveOfflineRegistrationModeUseCase>()
        every { offline() } returns Result.success(flowOf(false))
        return RegisterSubscriptionComposeViewModel(
            getAvailableOnuListUseCase = onus,
            getRegistrationCatalogUseCase = getCatalog,
            refreshRegistrationCatalogUseCase = refresh,
            getPlaceFromLocationUseCase = mockk(relaxed = true),
            registerSubscriptionUseCase = mockk(relaxed = true),
            getUserSessionUseCase = session,
            getNearNapBoxesUseCase = mockk(relaxed = true),
            installationOrderUseCase = mockk(relaxed = true),
            observeOfflineRegistrationModeUseCase = offline,
            retryTr069ProvisioningUseCase = mockk(relaxed = true),
            pollRegistrationProgressUseCase = mockk(relaxed = true),
            observabilityClient = mockk(relaxed = true),
            mainImmediate = dispatcher,
            subscriptionActionsRepository = actions,
        )
    }
}
