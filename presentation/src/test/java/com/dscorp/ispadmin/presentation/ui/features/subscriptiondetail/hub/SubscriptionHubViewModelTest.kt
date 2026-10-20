package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub

import com.dscorp.ispadmin.domain.model.Payment
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
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
        every { gateway.lastMethod() } returns null
        every { gateway.rememberMethod(any()) } returns Unit
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
    fun `charging sends one batch and shows the receipt`() = runTest(dispatcher) {
        val batch = slot<ChargeBatch>()
        coEvery { gateway.registerCharge(capture(batch)) } returns listOf(receipt(1, 100, paid = true), receipt(2, 200, paid = true))
        coEvery { gateway.restoreInternet(1842) } returns Result.success(Unit)
        every { gateway.rememberMethod(any()) } returns Unit
        viewModel.onIntent(SubscriptionHubIntent.Load(1842))
        advanceUntilIdle()

        viewModel.onIntent(SubscriptionHubIntent.OpenCharge)
        viewModel.onIntent(SubscriptionHubIntent.PayerNameChanged("Jorge Ramos V."))
        viewModel.onIntent(SubscriptionHubIntent.SubmitCharge)
        advanceUntilIdle()

        assertEquals(ChargeBatch(listOf(1, 2), "Yape", 0.0, null, "Jorge Ramos V.", responsibleId = 7), batch.captured)
        coVerify(exactly = 1) { gateway.restoreInternet(1842) }
        verify { gateway.rememberMethod("Yape") }
        assertNull(viewModel.uiState.value.charge)
        val done = viewModel.uiState.value.chargeReceipt!!
        assertEquals("S/ 100.00", done.amount)
        assertEquals("Yape · Jorge Ramos V.", done.method)
        assertEquals(true, done.restored)
    }

    @Test
    fun `a failed receipt stops the charge and keeps the sheet open with the error`() = runTest(dispatcher) {
        coEvery { gateway.registerCharge(any()) } throws IllegalStateException("500")
        viewModel.onIntent(SubscriptionHubIntent.Load(1842))
        advanceUntilIdle()

        viewModel.onIntent(SubscriptionHubIntent.OpenCharge)
        viewModel.onIntent(SubscriptionHubIntent.MethodSelected("Efectivo"))
        viewModel.onIntent(SubscriptionHubIntent.SubmitCharge)
        advanceUntilIdle()

        coVerify(exactly = 1) { gateway.registerCharge(any()) }
        coVerify(exactly = 0) { gateway.restoreInternet(any()) }
        val charge = viewModel.uiState.value.charge
        assertEquals("No se pudo registrar el cobro. Revisa tu conexión y vuelve a intentar.", charge?.error)
        assertFalse(charge?.isSubmitting ?: true)
    }

    @Test
    fun `charge without restore leaves the service as it is`() = runTest(dispatcher) {
        coEvery { gateway.registerCharge(any()) } returns emptyList()
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
    fun `the current Core sends no debt totals, so the loaded receipts give the debt`() = runTest(dispatcher) {
        coEvery { gateway.subscription(1842) } returns subscription(status = ServiceStatus.ACTIVE, pendingInvoices = 0, totalDebt = 0.0)

        viewModel.onIntent(SubscriptionHubIntent.Load(1842))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("S/ 100.00", state.header?.debtAmount)
        assertEquals(true, state.header?.canCharge)
        assertEquals("S/ 100.00", state.ficha?.debt?.amount)
        assertEquals("Cobrar S/ 100.00", state.payments?.pending?.chargeLabel)
    }

    @Test
    fun `the current Core cut flag offers restore in the charge sheet`() = runTest(dispatcher) {
        coEvery { gateway.subscription(1842) } returns subscription(status = ServiceStatus.ACTIVE).copy(isServiceCutOff = true)
        viewModel.onIntent(SubscriptionHubIntent.Load(1842))
        advanceUntilIdle()

        viewModel.onIntent(SubscriptionHubIntent.OpenCharge)

        assertEquals(true, viewModel.uiState.value.charge?.draft?.canRestore)
        assertEquals(false, viewModel.uiState.value.charge?.draft?.restore)
    }

    @Test
    fun `cut status is read from the subscription`() = runTest(dispatcher) {
        viewModel.onIntent(SubscriptionHubIntent.Load(1842))
        advanceUntilIdle()
        viewModel.onIntent(SubscriptionHubIntent.OpenCharge)

        assertEquals(ServiceStatus.CUT_OFF, viewModel.uiState.value.charge?.status)
    }
}
