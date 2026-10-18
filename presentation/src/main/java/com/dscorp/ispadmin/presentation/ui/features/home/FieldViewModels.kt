package com.dscorp.ispadmin.presentation.ui.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.datasource.remote.InstallationOrderApi
import com.dscorp.ispadmin.data.model.InstallationOrderDto
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.repository.PendingSubscriptionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

/** Reads for the technician and sales homes, on endpoints the Core already has. */
interface FieldGateway {
    fun currentUser(): User?
    suspend fun openTickets(): List<AssistanceTicketResponse>
    suspend fun closedTickets(): List<AssistanceTicketResponse>
    suspend fun technicianOrders(technicianId: Int): List<InstallationOrderDto>
    suspend fun sellerOrders(sellerId: Int): List<InstallationOrderDto>
    suspend fun pendingUploads(): Int
}

data class FieldUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val initials: String = "",
    val today: TechnicianToday? = null,
    val agenda: SalesAgenda? = null,
    val filter: AgendaFilter = AgendaFilter.ALL,
)

sealed interface FieldIntent {
    data object Load : FieldIntent
    data object Refresh : FieldIntent
    data class Filter(val filter: AgendaFilter) : FieldIntent
}

private fun initialsOf(user: User?): String =
    listOfNotNull(user?.name, user?.lastName).mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }.joinToString("")

/** Shared load/refresh/error handling for the two field homes. */
abstract class FieldViewModel(
    protected val gateway: FieldGateway,
    private val mainImmediate: CoroutineDispatcher,
    protected val now: () -> Instant,
    private val loadError: String,
) : ViewModel() {
    protected val state = MutableStateFlow(FieldUiState())
    val uiState: StateFlow<FieldUiState> = state

    open fun onIntent(intent: FieldIntent) {
        when (intent) {
            FieldIntent.Load -> if (!loaded() && !state.value.loading) refresh()
            FieldIntent.Refresh -> refresh()
            is FieldIntent.Filter -> Unit
        }
    }

    protected abstract fun loaded(): Boolean
    protected abstract suspend fun load(user: User)

    private fun refresh() {
        viewModelScope.launch(mainImmediate) {
            val user = gateway.currentUser()
            state.update { it.copy(loading = true, initials = initialsOf(user)) }
            try {
                load(user ?: error("No session"))
                state.update { it.copy(loading = false, error = null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state.update { it.copy(loading = false, error = loadError) }
            }
        }
    }
}

class TechnicianTodayViewModel(gateway: FieldGateway, mainImmediate: CoroutineDispatcher, now: () -> Instant) :
    FieldViewModel(gateway, mainImmediate, now, "No se pudo cargar tu día. Revisa tu conexión y vuelve a intentar.") {

    override fun loaded() = state.value.today != null

    override suspend fun load(user: User) {
        val today = TechnicianToday.from(
            user, gateway.openTickets(), gateway.closedTickets(), gateway.technicianOrders(user.id ?: 0), gateway.pendingUploads(), now(),
        )
        state.update { it.copy(today = today) }
    }
}

class SalesAgendaViewModel(gateway: FieldGateway, mainImmediate: CoroutineDispatcher, now: () -> Instant) :
    FieldViewModel(gateway, mainImmediate, now, "No se pudieron cargar las instalaciones. Revisa tu conexión y vuelve a intentar.") {
    private var orders: List<InstallationOrderDto> = emptyList()
    private var tickets: List<AssistanceTicketResponse> = emptyList()

    override fun onIntent(intent: FieldIntent) {
        if (intent is FieldIntent.Filter) {
            state.update { it.copy(filter = intent.filter) }
            publish()
        } else {
            super.onIntent(intent)
        }
    }

    override fun loaded() = state.value.agenda != null

    override suspend fun load(user: User) {
        orders = gateway.sellerOrders(user.id ?: 0)
        tickets = gateway.openTickets() + gateway.closedTickets()
        publish()
    }

    private fun publish() {
        state.update { it.copy(agenda = SalesAgenda.from(orders, tickets, now(), it.filter)) }
    }
}

class RepositoryFieldGateway(
    private val repository: IRepository,
    private val orders: InstallationOrderApi,
    private val pending: PendingSubscriptionRepository,
) : FieldGateway {
    override fun currentUser(): User? = repository.getUserSession()

    override suspend fun openTickets(): List<AssistanceTicketResponse> = OPEN.flatMap { repository.getTicketsByStatus(it) }

    override suspend fun closedTickets(): List<AssistanceTicketResponse> = repository.getTicketsByStatus(AssistanceTicketStatus.CLOSED)

    override suspend fun technicianOrders(technicianId: Int): List<InstallationOrderDto> =
        orders.getInstallationOrdersByTechnicianPaginated(technicianId, 0, PAGE).content

    override suspend fun sellerOrders(sellerId: Int): List<InstallationOrderDto> =
        orders.getInstallationOrdersBySellerPaginated(sellerId, 0, PAGE).content

    override suspend fun pendingUploads(): Int = pending.observePending().first().size

    private companion object {
        // The Core pages orders newest first; one page covers the current agenda.
        const val PAGE = 50
        val OPEN = listOf(AssistanceTicketStatus.PENDING, AssistanceTicketStatus.REOPEN, AssistanceTicketStatus.ASSIGNED, AssistanceTicketStatus.IN_PROGRESS)
    }
}
