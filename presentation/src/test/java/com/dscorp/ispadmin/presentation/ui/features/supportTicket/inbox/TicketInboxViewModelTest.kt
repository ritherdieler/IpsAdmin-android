package com.dscorp.ispadmin.presentation.ui.features.supportTicket.inbox

import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class TicketInboxViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val gateway = mockk<TicketInboxGateway>()
    private val now = Instant.parse("2026-10-14T16:00:00Z")
    private lateinit var viewModel: TicketInboxViewModel

    private fun ticket(id: Int, priority: String, assignedTo: String? = null, status: AssistanceTicketStatus = AssistanceTicketStatus.PENDING) =
        AssistanceTicketResponse(id = id, name = "ANA", category = "Otros", description = "", status = status, priority = priority,
            createdAt = Date.from(now.minusSeconds(id.toLong())), assignedTo = assignedTo, sheetImageUrl = null)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { gateway.counts() } returns mapOf("PENDING" to 41L, "ASSIGNED" to 2L)
        coEvery { gateway.pending() } returns listOf(ticket(1, "Baja"), ticket(2, "Alta"))
        coEvery { gateway.inProgress() } returns listOf(ticket(3, "Media", assignedTo = "Diego", status = AssistanceTicketStatus.ASSIGNED))
        viewModel = TicketInboxViewModel(gateway, mainImmediate = dispatcher, now = { now })
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `opening shows the pending tab with the Core counts`() = runTest(dispatcher) {
        viewModel.onIntent(TicketInboxIntent.Load)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TicketTab.PENDING, state.tab)
        assertEquals(listOf(2, 1), state.rows.map { it.id })
        assertEquals(mapOf(TicketTab.PENDING to 41L, TicketTab.IN_PROGRESS to 2L), state.counts)
        assertEquals("2 pendientes · prioridad alta primero", state.summary)
    }

    @Test
    fun `each tab loads once and chips filter it`() = runTest(dispatcher) {
        viewModel.onIntent(TicketInboxIntent.Load)
        advanceUntilIdle()

        viewModel.onIntent(TicketInboxIntent.TabSelected(TicketTab.IN_PROGRESS))
        advanceUntilIdle()
        viewModel.onIntent(TicketInboxIntent.TabSelected(TicketTab.PENDING))
        viewModel.onIntent(TicketInboxIntent.TabSelected(TicketTab.IN_PROGRESS))
        advanceUntilIdle()
        assertEquals(listOf(3), viewModel.uiState.value.rows.map { it.id })
        coVerify(exactly = 1) { gateway.inProgress() }

        viewModel.onIntent(TicketInboxIntent.TabSelected(TicketTab.PENDING))
        viewModel.onIntent(TicketInboxIntent.HighPriorityToggled)
        assertEquals(listOf(2), viewModel.uiState.value.rows.map { it.id })
        viewModel.onIntent(TicketInboxIntent.HighPriorityToggled)
        viewModel.onIntent(TicketInboxIntent.UnassignedToggled)
        assertEquals(listOf(2, 1), viewModel.uiState.value.rows.map { it.id })
    }

    @Test
    fun `refresh reloads the open tab and failing counts leave the tabs without numbers`() = runTest(dispatcher) {
        coEvery { gateway.counts() } throws IllegalStateException("500")
        viewModel.onIntent(TicketInboxIntent.Load)
        advanceUntilIdle()
        assertEquals(emptyMap<TicketTab, Long>(), viewModel.uiState.value.counts)

        viewModel.onIntent(TicketInboxIntent.Refresh)
        advanceUntilIdle()
        coVerify(exactly = 2) { gateway.pending() }
    }
}
