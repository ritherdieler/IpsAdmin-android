package com.dscorp.ispadmin.presentation.ui.features.supportTicket.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

interface TicketInboxGateway {
    suspend fun pending(): List<AssistanceTicketResponse>
    suspend fun inProgress(): List<AssistanceTicketResponse>
    suspend fun closedThisMonth(): List<AssistanceTicketResponse>
    suspend fun counts(): Map<String, Long>
}

data class TicketInboxUiState(
    val tab: TicketTab = TicketTab.PENDING,
    val tickets: Map<TicketTab, List<AssistanceTicketResponse>> = emptyMap(),
    val filters: TicketFilters = TicketFilters(),
    val counts: Map<TicketTab, Long> = emptyMap(),
    val loading: Boolean = false,
    val error: String? = null,
    val rows: List<TicketInboxRow> = emptyList(),
    val summary: String = "",
)

sealed interface TicketInboxIntent {
    data object Load : TicketInboxIntent
    data object Refresh : TicketInboxIntent
    data class TabSelected(val tab: TicketTab) : TicketInboxIntent
    data object HighPriorityToggled : TicketInboxIntent
    data object UnassignedToggled : TicketInboxIntent
    data class CategorySelected(val category: String?) : TicketInboxIntent
    data class PlaceSelected(val place: String?) : TicketInboxIntent
}

class TicketInboxViewModel(
    private val gateway: TicketInboxGateway,
    private val mainImmediate: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val now: () -> Instant = { Instant.now() },
) : ViewModel() {
    private val _uiState = MutableStateFlow(TicketInboxUiState())
    val uiState: StateFlow<TicketInboxUiState> = _uiState
    fun onIntent(intent: TicketInboxIntent) {
        when (intent) {
            TicketInboxIntent.Load -> if (_uiState.value.tickets.isEmpty()) refresh()
            TicketInboxIntent.Refresh -> refresh()
            is TicketInboxIntent.TabSelected -> {
                update { it.copy(tab = intent.tab, filters = TicketFilters()) }
                if (intent.tab !in _uiState.value.tickets) loadTab(intent.tab)
            }
            TicketInboxIntent.HighPriorityToggled -> update { it.copy(filters = it.filters.copy(highPriority = !it.filters.highPriority)) }
            TicketInboxIntent.UnassignedToggled -> update { it.copy(filters = it.filters.copy(unassigned = !it.filters.unassigned)) }
            is TicketInboxIntent.CategorySelected -> update { it.copy(filters = it.filters.copy(category = intent.category)) }
            is TicketInboxIntent.PlaceSelected -> update { it.copy(filters = it.filters.copy(place = intent.place)) }
        }
    }

    private fun refresh() {
        val tab = _uiState.value.tab
        update { it.copy(tickets = emptyMap()) }
        loadTab(tab)
        viewModelScope.launch(mainImmediate) {
            val counts = try {
                tabCounts(gateway.counts())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emptyMap()
            }
            update { it.copy(counts = counts) }
        }
    }

    private fun loadTab(tab: TicketTab) {
        viewModelScope.launch(mainImmediate) {
            update { it.copy(loading = true, error = null) }
            val tickets = try {
                when (tab) {
                    TicketTab.PENDING -> gateway.pending()
                    TicketTab.IN_PROGRESS -> gateway.inProgress()
                    TicketTab.CLOSED -> gateway.closedThisMonth()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                update { it.copy(loading = false, error = "No se pudieron cargar los tickets. Revisa tu conexión y vuelve a intentar.") }
                return@launch
            }
            update { it.copy(loading = false, tickets = it.tickets + (tab to tickets)) }
        }
    }

    /** Applies a change and recomputes the visible rows and the summary. */
    private fun update(change: (TicketInboxUiState) -> TicketInboxUiState) {
        _uiState.update { current ->
            val next = change(current)
            val rows = ticketRows(next.tickets[next.tab].orEmpty(), next.filters, now())
            next.copy(rows = rows, summary = inboxSummary(next.tab, rows.size))
        }
    }
}

/** Ticket list over the existing repository calls; «En curso» joins assigned and in-progress tickets. */
class RepositoryTicketInboxGateway(
    private val repository: com.dscorp.ispadmin.data.repository.IRepository,
    private val fichaApi: com.dscorp.ispadmin.data.datasource.remote.FichaApi,
) : TicketInboxGateway {
    override suspend fun pending(): List<AssistanceTicketResponse> =
        repository.getTicketsByStatus(com.dscorp.ispadmin.data.response.AssistanceTicketStatus.PENDING) +
            optional { repository.getTicketsByStatus(com.dscorp.ispadmin.data.response.AssistanceTicketStatus.REOPEN) }

    override suspend fun inProgress(): List<AssistanceTicketResponse> =
        repository.getTicketsByStatus(com.dscorp.ispadmin.data.response.AssistanceTicketStatus.ASSIGNED) +
            optional { repository.getTicketsByStatus(com.dscorp.ispadmin.data.response.AssistanceTicketStatus.IN_PROGRESS) }

    override suspend fun closedThisMonth(): List<AssistanceTicketResponse> {
        val zone = com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.FICHA_ZONE
        val month = java.time.YearMonth.now(zone)
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.atEndOfMonth().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return repository.getTicketsByDateRange(com.dscorp.ispadmin.data.response.AssistanceTicketStatus.CLOSED, start, end)
    }

    override suspend fun counts(): Map<String, Long> = fichaApi.ticketCounts().byStatus.orEmpty()

    /** A secondary status that fails (e.g. an older Core without it) leaves the main list intact. */
    private suspend fun optional(block: suspend () -> List<AssistanceTicketResponse>): List<AssistanceTicketResponse> = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        emptyList()
    }
}
