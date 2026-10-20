package com.dscorp.ispadmin.presentation.ui.features.home

import com.dscorp.ispadmin.data.model.InstallationOrderStatus
import com.dscorp.ispadmin.data.model.UserDto
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus
import com.dscorp.ispadmin.domain.model.User
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FieldViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val gateway = mockk<FieldGateway>()
    private val now = { NOW.atZone(LIMA).toInstant() }
    private val tech = User(id = 7, name = "Marco", lastName = "Salinas", type = User.UserType.TECHNICIAN)
    private val seller = User(id = 3, name = "Karla", lastName = "Vega", type = User.UserType.SALES)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { gateway.openTickets() } returns listOf(
            ticket(3612, category = "Instalación de Internet", status = AssistanceTicketStatus.ASSIGNED, assignedTo = "Marco Salinas", scheduled = NOW),
            ticket(3598, category = "Instalación de Internet"),
        )
        coEvery { gateway.closedTickets() } returns emptyList()
        coEvery { gateway.technicianOrders(7) } returns listOf(order(6, technician = UserDto(id = 7), scheduled = NOW.plusHours(4)))
        coEvery { gateway.sellerOrders(3) } returns listOf(order(7, status = InstallationOrderStatus.SOLICITADO))
        coEvery { gateway.pendingUploads() } returns 0
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `technician today loads my jobs`() = runTest(dispatcher) {
        every { gateway.currentUser() } returns tech
        val viewModel = TechnicianTodayViewModel(gateway, dispatcher, now)

        viewModel.onIntent(FieldIntent.Load)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("mié 14 oct · 2 trabajos", state.today?.subtitle)
        assertEquals("MS", state.initials)
        assertNull(state.today?.uploads)
        assertFalse(state.loading)
    }

    @Test
    fun `technician today keeps a readable error`() = runTest(dispatcher) {
        every { gateway.currentUser() } returns tech
        coEvery { gateway.openTickets() } throws IllegalStateException("HTTP 500")
        val viewModel = TechnicianTodayViewModel(gateway, dispatcher, now)

        viewModel.onIntent(FieldIntent.Load)
        advanceUntilIdle()

        assertEquals("No se pudo cargar tu día. Revisa tu conexión y vuelve a intentar.", viewModel.uiState.value.error)
    }

    @Test
    fun `sales agenda loads my orders with installation tickets and filters them`() = runTest(dispatcher) {
        every { gateway.currentUser() } returns seller
        val viewModel = SalesAgendaViewModel(gateway, dispatcher, now)

        viewModel.onIntent(FieldIntent.Load)
        advanceUntilIdle()
        assertEquals(listOf("Ticket #3612", "Orden #7", "Ticket #3598"), viewModel.uiState.value.agenda?.items?.map { it.reference })

        viewModel.onIntent(FieldIntent.Filter(AgendaFilter.UNASSIGNED))
        assertEquals(listOf("Orden #7", "Ticket #3598"), viewModel.uiState.value.agenda?.items?.map { it.reference })
        assertEquals(AgendaFilter.UNASSIGNED, viewModel.uiState.value.filter)
    }
}
