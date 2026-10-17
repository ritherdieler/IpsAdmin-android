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
        viewModel = SubscriptionHubViewModel(gateway, mainImmediate = dispatcher, today = { today }, now = { java.time.Instant.parse("2026-10-14T15:00:00Z") })
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
        assertEquals("S/ 50.00", viewModel.uiState.value.payments?.pending?.amount)
        assertEquals(listOf("Set 2026"), viewModel.uiState.value.payments?.receipts?.map { it.title })
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

    @Test
    fun `network tab loads the equipment reading once and survives a failing reading`() = runTest(dispatcher) {
        coEvery { gateway.subscription(1842) } returns subscription().copy(
            fiberOnuSn = "HWTC9A3F21B0",
            napBox = com.dscorp.ispadmin.domain.model.NapBoxResponse(code = "LV-15-3", mufaId = 4, placeName = "La Villa", placeId = 1),
        )
        coEvery { gateway.cpeReading(1842) } throws IllegalStateException("500")
        coEvery { gateway.mufaName(4) } returns "Antena Claro (96 hilos)"
        viewModel.onIntent(SubscriptionHubIntent.Load(1842))
        advanceUntilIdle()

        viewModel.onIntent(SubscriptionHubIntent.TabSelected(FichaTab.RED))
        advanceUntilIdle()
        viewModel.onIntent(SubscriptionHubIntent.TabSelected(FichaTab.RESUMEN))
        viewModel.onIntent(SubscriptionHubIntent.TabSelected(FichaTab.RED))
        advanceUntilIdle()

        val network = viewModel.uiState.value.network
        assertEquals(true, network?.equipmentUnavailable)
        assertEquals("Antena Claro (96 hilos)", network?.fiber?.firstOrNull { it.label == "Mufa" }?.value)
        coVerify(exactly = 1) { gateway.cpeReading(1842) }
    }

    @Test
    fun `equipment actions report their result`() = runTest(dispatcher) {
        coEvery { gateway.rebootOnu(1842) } returns Result.success(Unit)
        coEvery { gateway.resendConfig(1842) } returns Result.failure(IllegalStateException("ACS"))
        val events = mutableListOf<SubscriptionHubEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events += it } }
        viewModel.onIntent(SubscriptionHubIntent.Load(1842))
        advanceUntilIdle()

        viewModel.onIntent(SubscriptionHubIntent.RebootOnu)
        advanceUntilIdle()
        viewModel.onIntent(SubscriptionHubIntent.ResendConfig)
        advanceUntilIdle()

        assertEquals(
            listOf(
                SubscriptionHubEvent.Message("Reinicio de ONU enviado"),
                SubscriptionHubEvent.Message("No se pudo reenviar la configuración"),
            ),
            events,
        )
    }

    @Test
    fun `tickets load with the ficha and a failure does not break it`() = runTest(dispatcher) {
        coEvery { gateway.tickets(1842) } returns listOf(
            com.dscorp.ispadmin.data.response.AssistanceTicketResponse(
                id = 3609, name = "JORGE", category = "Sin Conexión a Internet", description = "",
                status = com.dscorp.ispadmin.data.response.AssistanceTicketStatus.PENDING, priority = "Alta", sheetImageUrl = null,
            )
        )
        viewModel.onIntent(SubscriptionHubIntent.Load(1842))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.tickets?.openCount)

        coEvery { gateway.tickets(1842) } throws IllegalStateException("404")
        viewModel.onIntent(SubscriptionHubIntent.Load(1842))
        advanceUntilIdle()
        assertEquals(null, viewModel.uiState.value.tickets)
        assertEquals("Cortado desde 15 set", viewModel.uiState.value.ficha?.badges?.first()?.text)
    }

    @Test
    fun `reloading while on the network tab reads the equipment again`() = runTest(dispatcher) {
        coEvery { gateway.cpeReading(1842) } returns CpeReading(online = true, rxDbm = -20.0, lastInform = null, model = null)
        viewModel.onIntent(SubscriptionHubIntent.Load(1842))
        advanceUntilIdle()
        viewModel.onIntent(SubscriptionHubIntent.TabSelected(FichaTab.RED))
        advanceUntilIdle()

        viewModel.onIntent(SubscriptionHubIntent.Load(1842))
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.network == null)
        coVerify(exactly = 2) { gateway.cpeReading(1842) }
    }

    @Test
    fun `history loads on its tab, filters by chip and survives a failing log`() = runTest(dispatcher) {
        coEvery { gateway.history(1842) } returns listOf(HistoryLog(java.time.Instant.parse("2026-09-15T11:00:00Z").toEpochMilli(), "CUT_FOR_DEBT"))
        viewModel.onIntent(SubscriptionHubIntent.Load(1842))
        advanceUntilIdle()

        viewModel.onIntent(SubscriptionHubIntent.TabSelected(FichaTab.HISTORIAL))
        advanceUntilIdle()
        assertEquals(listOf("Corte automático por deuda"), viewModel.uiState.value.historyMonths.flatMap { it.items }.map { it.title })

        viewModel.onIntent(SubscriptionHubIntent.HistoryFilterSelected(HistoryFilter.PAYMENTS))
        assertEquals(HistoryFilter.PAYMENTS, viewModel.uiState.value.historyFilter)
        assertEquals(emptyList<HistoryMonth>(), viewModel.uiState.value.historyMonths)

        coEvery { gateway.history(1842) } throws IllegalStateException("500")
        viewModel.onIntent(SubscriptionHubIntent.Load(1842))
        advanceUntilIdle()
        assertEquals(HistoryFilter.PAYMENTS, viewModel.uiState.value.historyFilter)
        assertEquals(false, viewModel.uiState.value.history == null)
    }
}
