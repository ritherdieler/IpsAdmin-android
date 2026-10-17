package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha

import com.dscorp.ispadmin.domain.model.Payment
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.METHODS
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.SubscriptionHubEvent
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.SubscriptionHubGateway
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.SubscriptionHubIntent
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.SubscriptionHubViewModel
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.subscription
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class FichaViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val gateway = mockk<SubscriptionHubGateway>()
    private lateinit var viewModel: SubscriptionHubViewModel
    private val today = LocalDate.of(2026, 10, 14)

    private fun unpaid(id: Int, month: Int) = Payment(
        id = id, amountToPay = 50.0, subscriptionId = 1842, dueDate = "2026-%02d-15T00:00:00".format(month),
        billingDate = LocalDate.of(2026, month, 1).atStartOfDay(FICHA_ZONE).toInstant().toEpochMilli(),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { gateway.subscription(1842) } returns subscription(pendingInvoices = 1, totalDebt = 50.0).copy(lastCutOffDate = "2026-09-15")
        coEvery { gateway.recentPayments(1842, any()) } returns listOf(unpaid(9, 9))
        coEvery { gateway.paymentMethods() } returns METHODS
        every { gateway.currentUser() } returns null
        viewModel = SubscriptionHubViewModel(gateway, mainImmediate = dispatcher, today = { today })
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `load builds the ficha header from the subscription and its receipts`() = runTest(dispatcher) {
        viewModel.onIntent(SubscriptionHubIntent.Load(1842))
        advanceUntilIdle()

        val ficha = viewModel.uiState.value.ficha
        assertEquals("Cortado desde 15 set", ficha?.badges?.first()?.text)
        assertEquals(DebtCard("S/ 50.00", "set 2026"), ficha?.debt)
        assertEquals(FichaTab.RESUMEN, viewModel.uiState.value.tab)
    }

    @Test
    fun `tabs switch without reloading`() = runTest(dispatcher) {
        viewModel.onIntent(SubscriptionHubIntent.Load(1842))
        advanceUntilIdle()

        viewModel.onIntent(SubscriptionHubIntent.TabSelected(FichaTab.PAGOS))

        assertEquals(FichaTab.PAGOS, viewModel.uiState.value.tab)
        coVerify(exactly = 1) { gateway.subscription(1842) }
    }

    @Test
    fun `restore calls the Core and reloads when it is allowed`() = runTest(dispatcher) {
        coEvery { gateway.restoreInternet(1842) } returns Result.success(Unit)
        val events = mutableListOf<SubscriptionHubEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events += it } }
        viewModel.onIntent(SubscriptionHubIntent.Load(1842))
        advanceUntilIdle()

        viewModel.onIntent(SubscriptionHubIntent.RestoreService)
        advanceUntilIdle()

        coVerify(exactly = 1) { gateway.restoreInternet(1842) }
        assertEquals(listOf(SubscriptionHubEvent.Message("Servicio restablecido")), events)
        coVerify(exactly = 2) { gateway.subscription(1842) }
    }

    @Test
    fun `restore explains what to charge when there is too much debt`() = runTest(dispatcher) {
        coEvery { gateway.subscription(1842) } returns subscription(status = ServiceStatus.CUT_OFF, pendingInvoices = 2, totalDebt = 100.0)
        val events = mutableListOf<SubscriptionHubEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events += it } }
        viewModel.onIntent(SubscriptionHubIntent.Load(1842))
        advanceUntilIdle()

        viewModel.onIntent(SubscriptionHubIntent.RestoreService)
        advanceUntilIdle()

        coVerify(exactly = 0) { gateway.restoreInternet(any()) }
        assertEquals(
            listOf(SubscriptionHubEvent.Message("Para restablecer debe quedar con menos de 2 recibos pendientes: cobra al menos 1 mes.")),
            events,
        )
    }
}
