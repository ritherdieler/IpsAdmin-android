package com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail

import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.model.User.UserType
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.CpeReading
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.InfoRow
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class TicketDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val gateway = mockk<TicketDetailGateway>()
    private val now = Instant.parse("2026-10-14T15:42:00Z")
    private val admin = User(id = 1, name = "Sergio", type = UserType.ADMIN)
    private lateinit var viewModel: TicketDetailViewModel

    private val pending = AssistanceTicketResponse(
        id = 3609, name = "PEDRO", phone = "964818302", category = "Sin Conexión a Internet", description = "Sin internet",
        status = AssistanceTicketStatus.PENDING, priority = "Alta", createdAt = Date.from(now.minusSeconds(7200)),
        sheetImageUrl = null, subscriptionId = 1842,
    )
    private val events = mutableListOf<TicketDetailEvent>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { gateway.currentUser() } returns admin
        coEvery { gateway.ticket(3609) } returns pending
        coEvery { gateway.subscription(1842) } returns subscription(status = ServiceStatus.CUT_OFF, pendingInvoices = 1, totalDebt = 50.0)
        coEvery { gateway.ticketsBySubscription(1842) } returns listOf(pending, pending.copy(id = 3471, status = AssistanceTicketStatus.CLOSED))
        viewModel = TicketDetailViewModel(gateway, mainImmediate = dispatcher, now = { now })
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun kotlinx.coroutines.test.TestScope.collectEvents() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events += it } }
    }

    @Test
    fun `load shows the ticket, the subscriber and the previous tickets`() = runTest(dispatcher) {
        viewModel.onIntent(TicketDetailIntent.Load(3609))
        advanceUntilIdle()

        val detail = viewModel.uiState.value.detail
        assertEquals("Ticket #3609", detail?.title)
        assertEquals("Cobrar S/ 50.00", detail?.debtNotice?.chargeLabel)
        assertEquals(listOf(3471), detail?.previous?.map { it.id })
        assertEquals(1842, viewModel.uiState.value.subscriptionId)
    }

    @Test
    fun `a failing subscriber lookup still shows the ticket`() = runTest(dispatcher) {
        coEvery { gateway.subscription(1842) } throws IllegalStateException("500")
        coEvery { gateway.ticketsBySubscription(1842) } throws IllegalStateException("500")

        viewModel.onIntent(TicketDetailIntent.Load(3609))
        advanceUntilIdle()

        assertEquals("Ticket #3609", viewModel.uiState.value.detail?.title)
        assertNull(viewModel.uiState.value.detail?.subscriber)
    }

    @Test
    fun `taking, assigning, rescheduling and cancelling report and reload`() = runTest(dispatcher) {
        collectEvents()
        coEvery { gateway.take(3609, 1) } returns Unit
        coEvery { gateway.technicians() } returns listOf(User(id = 7, name = "Marco", lastName = "Salinas", type = UserType.TECHNICIAN))
        coEvery { gateway.assign(3609, 7) } returns Unit
        coEvery { gateway.reschedule(3609, 1_800_000_000_000) } returns Unit
        coEvery { gateway.cancel(3609, 1) } returns Unit
        viewModel.onIntent(TicketDetailIntent.Load(3609))
        advanceUntilIdle()

        viewModel.onIntent(TicketDetailIntent.Take)
        advanceUntilIdle()
        viewModel.onIntent(TicketDetailIntent.OpenAssign)
        advanceUntilIdle()
        assertEquals(listOf("Marco Salinas"), viewModel.uiState.value.technicians?.map { it.name })
        viewModel.onIntent(TicketDetailIntent.Assign(7))
        advanceUntilIdle()
        viewModel.onIntent(TicketDetailIntent.Reschedule(1_800_000_000_000))
        advanceUntilIdle()
        viewModel.onIntent(TicketDetailIntent.Cancel)
        advanceUntilIdle()

        assertEquals(
            listOf("Ticket tomado", "Ticket asignado a Marco Salinas", "Ticket reagendado", "Ticket cancelado"),
            events.filterIsInstance<TicketDetailEvent.Message>().map { it.text },
        )
        assertNull(viewModel.uiState.value.technicians)
        coVerify(exactly = 5) { gateway.ticket(3609) }
    }

    @Test
    fun `a taken ticket reads the ONU, and says so when there is no reading`() = runTest(dispatcher) {
        coEvery { gateway.ticket(3609) } returns pending.copy(status = AssistanceTicketStatus.ASSIGNED, assignedTo = "Diego", assignedAt = Date.from(now))
        coEvery { gateway.cpeReading(1842) } returns CpeReading(online = true, rxDbm = -24.9, lastInform = now.minusSeconds(120), model = null)

        viewModel.onIntent(TicketDetailIntent.Load(3609))
        advanceUntilIdle()

        assertEquals(
            listOf(
                InfoRow("ONU", "", badge = StatusBadge("En línea", BadgeTone.SUCCESS)),
                InfoRow("Potencia", "−24.9 dBm · aceptable"),
                InfoRow("Último reporte", "hace 2 min"),
            ),
            viewModel.uiState.value.diagnosis?.rows,
        )

        coEvery { gateway.cpeReading(1842) } throws IllegalStateException("500")
        viewModel.onIntent(TicketDetailIntent.Load(3609))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.diagnosis?.unavailable == true)
    }
}
