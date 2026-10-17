package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.domain.model.Payment
import com.dscorp.ispadmin.domain.model.PaymentMethodOption
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.CpeReading
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.FICHA_ZONE
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.FichaNetwork
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.FichaHeader
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.FichaPayments
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.FichaTab
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.FichaTickets
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.canRestore
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.restoreHint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the hub needs from the data layer; keeps the view model off the monolithic repository. */
interface SubscriptionHubGateway {
    suspend fun subscription(subscriptionId: Int): SubscriptionResponse
    suspend fun recentPayments(subscriptionId: Int, limit: Int): List<Payment>
    suspend fun paymentMethods(): List<PaymentMethodOption>
    suspend fun registerPayment(payment: Payment): Payment
    suspend fun restoreInternet(subscriptionId: Int): Result<Unit>
    fun currentUser(): User?
    suspend fun cpeReading(subscriptionId: Int): CpeReading = error("not wired")
    suspend fun mufaName(mufaId: Int): String? = null
    suspend fun rebootOnu(subscriptionId: Int): Result<Unit> = Result.failure(UnsupportedOperationException())
    suspend fun resendConfig(subscriptionId: Int): Result<Unit> = Result.failure(UnsupportedOperationException())
    suspend fun tickets(subscriptionId: Int): List<com.dscorp.ispadmin.data.response.AssistanceTicketResponse> = emptyList()
}

data class ChargeSheetState(
    val draft: ChargeDraft,
    val isSubmitting: Boolean = false,
    val error: String? = null,
) {
    val status: ServiceStatus get() = draft.status
}

data class SubscriptionHubUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val subscription: SubscriptionResponse? = null,
    val header: HubHeader? = null,
    val receipts: List<Payment> = emptyList(),
    val methods: List<PaymentMethodOption> = emptyList(),
    val charge: ChargeSheetState? = null,
    val ficha: FichaHeader? = null,
    val tab: FichaTab = FichaTab.RESUMEN,
    val payments: FichaPayments? = null,
    val network: FichaNetwork? = null,
    val tickets: FichaTickets? = null,
) {
    val unpaidReceipts: List<Payment> get() = receipts.filter { !it.paid }.sortedBy { it.billingDate }
}

sealed interface SubscriptionHubIntent {
    data class Load(val subscriptionId: Int) : SubscriptionHubIntent
    data object OpenCharge : SubscriptionHubIntent
    data object CloseCharge : SubscriptionHubIntent
    data class ReceiptToggled(val receiptId: Int) : SubscriptionHubIntent
    data class MethodSelected(val method: String) : SubscriptionHubIntent
    data class PayerNameChanged(val name: String) : SubscriptionHubIntent
    data object RestoreToggled : SubscriptionHubIntent
    data object SubmitCharge : SubscriptionHubIntent
    data class TabSelected(val tab: FichaTab) : SubscriptionHubIntent
    data object RestoreService : SubscriptionHubIntent
    data object RebootOnu : SubscriptionHubIntent
    data object ResendConfig : SubscriptionHubIntent
}

sealed interface SubscriptionHubEvent {
    data class Message(val text: String) : SubscriptionHubEvent
}

class SubscriptionHubViewModel(
    private val gateway: SubscriptionHubGateway,
    private val mainImmediate: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val today: () -> java.time.LocalDate = { java.time.LocalDate.now(FICHA_ZONE) },
    private val now: () -> java.time.Instant = { java.time.Instant.now() },
) : ViewModel() {

    private companion object {
        const val RECENT_RECEIPTS = 12
        const val LOAD_ERROR = "No se pudo cargar la suscripción. Revisa tu conexión y vuelve a intentar."
        const val CHARGE_ERROR = "No se pudo registrar el cobro. Revisa tu conexión y vuelve a intentar."
        const val SESSION_ERROR = "Tu sesión expiró. Vuelve a ingresar para cobrar."
    }

    private val _uiState = MutableStateFlow(SubscriptionHubUiState())
    val uiState: StateFlow<SubscriptionHubUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SubscriptionHubEvent>()
    val events: SharedFlow<SubscriptionHubEvent> = _events.asSharedFlow()

    fun onIntent(intent: SubscriptionHubIntent) {
        when (intent) {
            is SubscriptionHubIntent.Load -> load(intent.subscriptionId)
            SubscriptionHubIntent.OpenCharge -> openCharge()
            SubscriptionHubIntent.CloseCharge -> _uiState.update { it.copy(charge = null) }
            is SubscriptionHubIntent.ReceiptToggled -> updateDraft { it.toggle(intent.receiptId) }
            is SubscriptionHubIntent.MethodSelected -> updateDraft { it.copy(method = intent.method) }
            is SubscriptionHubIntent.PayerNameChanged -> updateDraft { it.copy(payerName = intent.name) }
            SubscriptionHubIntent.RestoreToggled -> updateDraft { it.copy(restore = !it.restore) }
            SubscriptionHubIntent.SubmitCharge -> submitCharge()
            is SubscriptionHubIntent.TabSelected -> {
                _uiState.update { it.copy(tab = intent.tab) }
                if (intent.tab == FichaTab.RED && _uiState.value.network == null) loadNetwork()
            }
            SubscriptionHubIntent.RestoreService -> restoreService()
            SubscriptionHubIntent.RebootOnu -> equipmentAction("Reinicio de ONU enviado", "No se pudo reiniciar la ONU") {
                gateway.rebootOnu(it)
            }
            SubscriptionHubIntent.ResendConfig -> equipmentAction("Configuración reenviada al equipo", "No se pudo reenviar la configuración") {
                gateway.resendConfig(it)
            }
        }
    }

    private fun load(subscriptionId: Int) {
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val subscription = gateway.subscription(subscriptionId)
                val receipts = gateway.recentPayments(subscriptionId, RECENT_RECEIPTS)
                val methods = loadMethods()
                val tickets = loadTickets(subscriptionId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        subscription = subscription,
                        header = HubHeader.from(subscription),
                        ficha = FichaHeader.from(subscription, receipts, today()),
                        payments = FichaPayments.from(receipts, subscription.totalDebt, today()),
                        tickets = tickets,
                        network = null,
                        receipts = receipts,
                        methods = methods,
                    )
                }
                if (_uiState.value.tab == FichaTab.RED) loadNetwork()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, error = LOAD_ERROR) }
            }
        }
    }

    /** Tickets are a side panel of the ficha: if they fail the rest still loads. */
    private suspend fun loadTickets(subscriptionId: Int): FichaTickets? = try {
        FichaTickets.from(gateway.tickets(subscriptionId))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    /** Without the catalog the sheet still works with no method preselected. */
    private suspend fun loadMethods(): List<PaymentMethodOption> = try {
        gateway.paymentMethods()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        emptyList()
    }

    /** «Red» tab: the equipment reading may fail (Core health off); the rest of the tab still shows. */
    private fun loadNetwork() {
        val subscription = _uiState.value.subscription ?: return
        viewModelScope.launch(mainImmediate) {
            val reading = try {
                gateway.cpeReading(subscription.id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            val mufa = subscription.napBox?.mufaId?.let { mufaId ->
                try {
                    gateway.mufaName(mufaId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
            }
            _uiState.update {
                it.copy(network = FichaNetwork.from(subscription, reading, cpeFailed = reading == null, mufaName = mufa, now = now()))
            }
        }
    }

    private fun equipmentAction(done: String, failed: String, call: suspend (Int) -> Result<Unit>) {
        val subscriptionId = _uiState.value.subscription?.id ?: return
        viewModelScope.launch(mainImmediate) {
            _events.emit(SubscriptionHubEvent.Message(if (call(subscriptionId).isSuccess) done else failed))
        }
    }

    /** «Restablecer» quick action: only when the cut subscription keeps fewer than two pending receipts. */
    private fun restoreService() {
        val subscription = _uiState.value.subscription ?: return
        viewModelScope.launch(mainImmediate) {
            val pending = subscription.pendingInvoiceQuantity
            if (!canRestore(subscription.serviceStatus, pending)) {
                restoreHint(subscription.serviceStatus, pending)?.let { _events.emit(SubscriptionHubEvent.Message(it)) }
                return@launch
            }
            val restored = gateway.restoreInternet(subscription.id).isSuccess
            _events.emit(SubscriptionHubEvent.Message(if (restored) "Servicio restablecido" else "No se pudo restablecer el servicio"))
            if (restored) load(subscription.id)
        }
    }

    private fun openCharge() {
        val state = _uiState.value
        val subscription = state.subscription ?: return
        val draft = ChargeDraft.start(state.receipts, state.methods, subscription.serviceStatus)
        _uiState.update { it.copy(charge = ChargeSheetState(draft)) }
    }

    private fun updateDraft(change: (ChargeDraft) -> ChargeDraft) {
        _uiState.update { state ->
            state.copy(charge = state.charge?.let { it.copy(draft = change(it.draft), error = null) })
        }
    }

    private fun submitCharge() {
        val charge = _uiState.value.charge ?: return
        val subscription = _uiState.value.subscription ?: return
        if (charge.draft.blockingReason != null || charge.isSubmitting) return
        val user = gateway.currentUser()
        if (user?.id == null) {
            _uiState.update { it.copy(charge = charge.copy(error = SESSION_ERROR)) }
            return
        }
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(charge = charge.copy(isSubmitting = true, error = null)) }
            try {
                charge.draft.selectedReceipts.forEach { receipt -> gateway.registerPayment(receipt.toCharge(charge.draft, user)) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(charge = charge.copy(isSubmitting = false, error = CHARGE_ERROR)) }
                return@launch
            }
            val restoreNote = restoreIfAsked(charge.draft, subscription.id)
            _uiState.update { it.copy(charge = null) }
            _events.emit(SubscriptionHubEvent.Message("Cobro registrado · ${formatSoles(charge.draft.total)}$restoreNote"))
            load(subscription.id)
        }
    }

    private suspend fun restoreIfAsked(draft: ChargeDraft, subscriptionId: Int): String {
        if (!draft.restore || !draft.canRestore) return ""
        return if (gateway.restoreInternet(subscriptionId).isSuccess) " · servicio restablecido"
        else " · no se pudo restablecer el servicio"
    }

    private fun Payment.toCharge(draft: ChargeDraft, user: User) = Payment(
        id = id,
        method = draft.method,
        responsibleId = requireNotNull(user.id) { "Charging needs a signed-in user" },
        responsibleName = user.name,
        electronicPayerName = draft.payerName.trim().takeIf { draft.requiresPayerName && it.isNotEmpty() },
    )
}
