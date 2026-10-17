package com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.ageLabel
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.powerLabel
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.toTitleCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.CpeReading
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.InfoRow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

/** What the ticket detail needs from the data layer. */
interface TicketDetailGateway {
    suspend fun ticket(ticketId: Int): AssistanceTicketResponse
    suspend fun subscription(subscriptionId: Int): SubscriptionResponse
    suspend fun ticketsBySubscription(subscriptionId: Int): List<AssistanceTicketResponse>
    suspend fun cpeReading(subscriptionId: Int): CpeReading
    suspend fun technicians(): List<User>
    suspend fun take(ticketId: Int, userId: Int)
    suspend fun assign(ticketId: Int, userId: Int)
    suspend fun reschedule(ticketId: Int, scheduledAt: Long)
    suspend fun cancel(ticketId: Int, userId: Int)
    suspend fun rebootOnu(subscriptionId: Int): Result<Unit>
    suspend fun resendConfig(subscriptionId: Int): Result<Unit>
    fun currentUser(): User?
}

data class TechnicianOption(val id: Int, val name: String)
data class TicketDiagnosis(val rows: List<InfoRow>, val unavailable: Boolean)

data class TicketDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val detail: TicketDetail? = null,
    val subscriptionId: Int? = null,
    val diagnosis: TicketDiagnosis? = null,
    val technicians: List<TechnicianOption>? = null,
    val busy: Boolean = false,
)

sealed interface TicketDetailIntent {
    data class Load(val ticketId: Int) : TicketDetailIntent
    data object Take : TicketDetailIntent
    data object OpenAssign : TicketDetailIntent
    data object CloseAssign : TicketDetailIntent
    data class Assign(val userId: Int) : TicketDetailIntent
    data class Reschedule(val scheduledAt: Long) : TicketDetailIntent
    data object Cancel : TicketDetailIntent
    data object RebootOnu : TicketDetailIntent
    data object ResendConfig : TicketDetailIntent
}

sealed interface TicketDetailEvent {
    data class Message(val text: String) : TicketDetailEvent
}

class TicketDetailViewModel(
    private val gateway: TicketDetailGateway,
    private val mainImmediate: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val now: () -> Instant = { Instant.now() },
) : ViewModel() {
    private val _uiState = MutableStateFlow(TicketDetailUiState())
    val uiState: StateFlow<TicketDetailUiState> = _uiState
    private val _events = MutableSharedFlow<TicketDetailEvent>()
    val events: SharedFlow<TicketDetailEvent> = _events

    private var ticketId: Int? = null

    fun onIntent(intent: TicketDetailIntent) {
        when (intent) {
            is TicketDetailIntent.Load -> load(intent.ticketId)
            TicketDetailIntent.Take -> act("Ticket tomado", "No se pudo tomar el ticket") { id, user -> gateway.take(id, user) }
            TicketDetailIntent.OpenAssign -> openAssign()
            TicketDetailIntent.CloseAssign -> _uiState.update { it.copy(technicians = null) }
            is TicketDetailIntent.Assign -> {
                val name = _uiState.value.technicians?.firstOrNull { it.id == intent.userId }?.name
                _uiState.update { it.copy(technicians = null) }
                act(listOfNotNull("Ticket asignado", name?.let { "a $it" }).joinToString(" "), "No se pudo asignar el ticket") { id, _ ->
                    gateway.assign(id, intent.userId)
                }
            }
            is TicketDetailIntent.Reschedule -> act("Ticket reagendado", "No se pudo reagendar el ticket") { id, _ ->
                gateway.reschedule(id, intent.scheduledAt)
            }
            TicketDetailIntent.Cancel -> act("Ticket cancelado", "No se pudo cancelar el ticket") { id, user -> gateway.cancel(id, user) }
            TicketDetailIntent.RebootOnu -> equipment("Reinicio de ONU enviado", "No se pudo reiniciar la ONU") { gateway.rebootOnu(it) }
            TicketDetailIntent.ResendConfig -> equipment("Configuración reenviada al equipo", "No se pudo reenviar la configuración") { gateway.resendConfig(it) }
        }
    }

    private fun load(id: Int) {
        ticketId = id
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(isLoading = it.detail == null, error = null) }
            val ticket = try {
                gateway.ticket(id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, error = LOAD_ERROR) }
                return@launch
            }
            val subscriptionId = ticket.subscriptionId
            val subscription = subscriptionId?.let { optional { gateway.subscription(it) } }
            val previous = subscriptionId?.let { optional { gateway.ticketsBySubscription(it) } }.orEmpty()
            val user = gateway.currentUser() ?: User()
            val detail = TicketDetail.from(ticket, subscription, previous, user, now())
            val taken = ticket.status in TAKEN
            val fiber = subscription?.installationType?.let { it != InstallationType.WIRELESS } == true
            val diagnosis = if (taken && fiber && subscriptionId != null) {
                val reading = optional { gateway.cpeReading(subscriptionId) }
                TicketDiagnosis(reading?.let { diagnosisRows(it, now()) }.orEmpty(), unavailable = reading == null)
            } else null
            _uiState.update {
                it.copy(isLoading = false, detail = detail, subscriptionId = subscriptionId, diagnosis = diagnosis)
            }
        }
    }

    private fun openAssign() {
        viewModelScope.launch(mainImmediate) {
            val technicians = optional { gateway.technicians() }
            if (technicians == null) {
                _events.emit(TicketDetailEvent.Message("No se pudieron cargar los técnicos"))
                return@launch
            }
            _uiState.update { state ->
                state.copy(technicians = technicians.mapNotNull { tech ->
                    tech.id?.let { id -> TechnicianOption(id, "${tech.name} ${tech.lastName}".toTitleCase()) }
                })
            }
        }
    }

    private fun act(done: String, failed: String, call: suspend (ticketId: Int, userId: Int) -> Unit) {
        val id = ticketId ?: return
        val userId = gateway.currentUser()?.id ?: return
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(busy = true) }
            val ok = optional { call(id, userId) } != null
            _uiState.update { it.copy(busy = false) }
            _events.emit(TicketDetailEvent.Message(if (ok) done else failed))
            if (ok) load(id)
        }
    }

    private fun equipment(done: String, failed: String, call: suspend (Int) -> Result<Unit>) {
        val subscriptionId = _uiState.value.subscriptionId ?: return
        viewModelScope.launch(mainImmediate) {
            _events.emit(TicketDetailEvent.Message(if (call(subscriptionId).isSuccess) done else failed))
        }
    }

    private suspend fun <T> optional(block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val LOAD_ERROR = "No se pudo cargar el ticket. Revisa tu conexión y vuelve a intentar."
        val TAKEN = setOf(AssistanceTicketStatus.ASSIGNED, AssistanceTicketStatus.IN_PROGRESS)
    }
}

/** Quick diagnosis of a taken ticket (a-tic-03): ONU state, optical power and last report. */
internal fun diagnosisRows(reading: CpeReading, now: Instant): List<InfoRow> = listOfNotNull(
    InfoRow("ONU", "", badge = if (reading.online) StatusBadge("En línea", BadgeTone.SUCCESS) else StatusBadge("Sin conexión", BadgeTone.ERROR)),
    reading.rxDbm?.let { InfoRow("Potencia", powerLabel(it)) },
    reading.lastInform?.let { InfoRow("Último reporte", ageLabel(it, now)) },
)
