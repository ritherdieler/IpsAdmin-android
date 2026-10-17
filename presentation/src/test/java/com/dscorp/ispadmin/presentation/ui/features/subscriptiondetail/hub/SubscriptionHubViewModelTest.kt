package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub

import com.dscorp.ispadmin.domain.model.Payment
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionHubViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val gateway = mockk<SubscriptionHubGateway>()
    private lateinit var viewModel: SubscriptionHubViewModel
    private val cashier = User(id = 7, name = "Rosa", type = User.UserType.SECRETARY)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { gateway.subscription(1842) } returns subscription()
        coEvery { gateway.recentPayments(1842, any()) } returns listOf(receipt(2, 200), receipt(1, 100), receipt(0, 50, paid = true))
        coEvery { gateway.paymentMethods() } returns METHODS
        every { gateway.currentUser() } returns cashier
        viewModel = SubscriptionHubViewModel(gateway, mainImmediate = dispatcher)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `load shows the header and the unpaid receipts`() = runTest(dispatcher) {
        viewModel.onIntent(SubscriptionHubIntent.Load(1842))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Jorge Ramos Villanueva", state.header?.name)
        assertEquals(listOf(1, 2), state.unpaidReceipts.map { it.id })
        assertFalse(state.isLoading)
    }

    @Test
    fun `opening the ficha to charge shows the charge sheet once it loads`() = runTest(dispatcher) {
        viewModel.onIntent(SubscriptionHubIntent.Load(1842, openCharge = true))
        advanceUntilIdle()

        assertEquals(setOf(1, 2), viewModel.uiState.value.charge?.draft?.selectedIds)
    }

    @Test
    fun `load failure keeps a retryable error`() = runTest(dispatcher) {
        coEvery { gateway.subscription(1842) } throws IllegalStateException("timeout")

        viewModel.onIntent(SubscriptionHubIntent.Load(1842))
        advanceUntilIdle()

        assertEquals("No se pudo cargar la suscripción. Revisa tu conexión y vuelve a intentar.", viewModel.uiState.value.error)
    }

    @Test
    fun `charging registers each receipt oldest first and restores the service`() = runTest(dispatcher) {
        val registered = mutableListOf<Payment>()
        coEvery { gateway.registerPayment(capture(registered)) } answers { firstArg() }
        coEvery { gateway.restoreInternet(1842) } returns Result.success(Unit)
        val events = mutableListOf<SubscriptionHubEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events += it } }
        viewModel.onIntent(SubscriptionHubIntent.Load(1842))
        advanceUntilIdle()

        viewModel.onIntent(SubscriptionHubIntent.OpenCharge)
        viewModel.onIntent(SubscriptionHubIntent.PayerNameChanged("Jorge Ramos V."))
        viewModel.onIntent(SubscriptionHubIntent.SubmitCharge)
        advanceUntilIdle()

        assertEquals(listOf(1, 2), registered.map { it.id })
        registered.forEach {
            assertEquals("Yape", it.method)
            assertEquals("Jorge Ramos V.", it.electronicPayerName)
            assertEquals(7, it.responsibleId)
        }
        coVerify(exactly = 1) { gateway.restoreInternet(1842) }
        assertNull(viewModel.uiState.value.charge)
        assertEquals(listOf(SubscriptionHubEvent.Message("Cobro registrado · S/ 100.00 · servicio restablecido")), events)
    }

    @Test
    fun `a failed receipt stops the charge and keeps the sheet open with the error`() = runTest(dispatcher) {
        val first = slot<Payment>()
        coEvery { gateway.registerPayment(capture(first)) } throws IllegalStateException("500")
        viewModel.onIntent(SubscriptionHubIntent.Load(1842))
        advanceUntilIdle()

        viewModel.onIntent(SubscriptionHubIntent.OpenCharge)
        viewModel.onIntent(SubscriptionHubIntent.MethodSelected("Efectivo"))
        viewModel.onIntent(SubscriptionHubIntent.SubmitCharge)
        advanceUntilIdle()

        coVerify(exactly = 1) { gateway.registerPayment(any()) }
        coVerify(exactly = 0) { gateway.restoreInternet(any()) }
        val charge = viewModel.uiState.value.charge
        assertEquals("No se pudo registrar el cobro. Revisa tu conexión y vuelve a intentar.", charge?.error)
        assertFalse(charge?.isSubmitting ?: true)
    }

    @Test
    fun `charge without restore leaves the service as it is`() = runTest(dispatcher) {
        coEvery { gateway.registerPayment(any()) } answers { firstArg() }
        viewModel.onIntent(SubscriptionHubIntent.Load(1842))
        advanceUntilIdle()

        viewModel.onIntent(SubscriptionHubIntent.OpenCharge)
        viewModel.onIntent(SubscriptionHubIntent.MethodSelected("Efectivo"))
        viewModel.onIntent(SubscriptionHubIntent.RestoreToggled)
        viewModel.onIntent(SubscriptionHubIntent.SubmitCharge)
        advanceUntilIdle()

        coVerify(exactly = 0) { gateway.restoreInternet(any()) }
        assertTrue(viewModel.uiState.value.charge == null)
    }

    @Test
    fun `cut status is read from the subscription`() = runTest(dispatcher) {
        viewModel.onIntent(SubscriptionHubIntent.Load(1842))
        advanceUntilIdle()
        viewModel.onIntent(SubscriptionHubIntent.OpenCharge)

        assertEquals(ServiceStatus.CUT_OFF, viewModel.uiState.value.charge?.status)
    }
}
