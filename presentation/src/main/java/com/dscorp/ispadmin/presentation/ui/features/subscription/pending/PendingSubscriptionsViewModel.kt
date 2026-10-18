package com.dscorp.ispadmin.presentation.ui.features.subscription.pending

import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.toTitleCase
import com.dscorp.ispadmin.domain.model.PendingSubscriptionStatus
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.domain.model.PendingSubscription
import com.dscorp.ispadmin.domain.usecase.subscription.ObservePendingSubscriptionsUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.PendingSubscriptionSyncResult
import com.dscorp.ispadmin.domain.usecase.subscription.SyncPendingSubscriptionsUseCase
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PendingSubscriptionsViewModel(
    private val observePendingSubscriptionsUseCase: ObservePendingSubscriptionsUseCase,
    private val syncPendingSubscriptionsUseCase: SyncPendingSubscriptionsUseCase,
    private val mainImmediate: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val gson: Gson = Gson(),
    private val discardPendingSubscriptionUseCase: com.dscorp.ispadmin.domain.usecase.subscription.DiscardPendingSubscriptionUseCase? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PendingSubscriptionsUiState())
    val uiState: StateFlow<PendingSubscriptionsUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<PendingSubscriptionsUiEvent>(
        extraBufferCapacity = 16
    )
    val uiEvent: SharedFlow<PendingSubscriptionsUiEvent> = _uiEvent.asSharedFlow()

    private var observeJob: Job? = null
    private var syncJob: Job? = null

    fun onIntent(intent: PendingSubscriptionsIntent) {
        when (intent) {
            PendingSubscriptionsIntent.Load -> load()
            PendingSubscriptionsIntent.Sync -> sync()
            is PendingSubscriptionsIntent.Discard -> discard(intent.localId)
            PendingSubscriptionsIntent.ClearError -> _uiState.update {
                it.copy(errorMessage = null, lastError = null, failedCount = 0)
            }
        }
    }

    private fun load() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            observePendingSubscriptionsUseCase().fold(
                onSuccess = { pendingFlow ->
                    pendingFlow.collect { pending ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                items = pending.map { item -> item.toListItem() }
                            )
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No se pudieron cargar las suscripciones pendientes"
                        )
                    }
                }
            )
        }
    }

    private fun discard(localId: String) {
        val useCase = discardPendingSubscriptionUseCase ?: return
        viewModelScope.launch(mainImmediate) {
            useCase(localId).onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "No se pudo descartar el registro") }
            }
        }
    }

    private fun sync() {
        if (syncJob?.isActive == true) return
        syncJob = viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(isSyncing = true, errorMessage = null) }
            syncPendingSubscriptionsUseCase().fold(
                onSuccess = { result -> emitSyncFeedback(result) },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            errorMessage = error.message ?: "No se pudieron sincronizar las suscripciones",
                            failedCount = 0,
                            lastError = error.message
                        )
                    }
                }
            )
        }
    }

    private suspend fun emitSyncFeedback(result: PendingSubscriptionSyncResult) {
        _uiState.update {
            it.copy(
                isSyncing = false,
                failedCount = result.failedCount,
                lastError = result.lastError,
                errorMessage = result.lastError.takeIf { result.failedCount > 0 }
            )
        }
        val message = when {
            result.syncedCount > 0 ->
                "Sincronización completada exitosamente (${result.syncedCount} suscripciones enviadas)"
            result.failedCount == 0 ->
                "No hay suscripciones pendientes por sincronizar"
            else -> null
        }
        if (message != null) {
            _uiEvent.emit(PendingSubscriptionsUiEvent.ShowSuccessSnackbar(message))
        }
    }

    private fun PendingSubscription.toListItem(): PendingSubscriptionListItem {
        val snapshot = runCatching {
            gson.fromJson(subscriptionJson, PendingClientSnapshot::class.java)
        }.getOrNull()
        val clientName = listOfNotNull(snapshot?.firstName, snapshot?.lastName)
            .joinToString(" ")
            .toTitleCase()
            .ifBlank { "Sin nombre" }
        val dni = snapshot?.dni?.takeIf { it.isNotBlank() }
        val type = when (snapshot?.installationType) {
            "WIRELESS" -> "Inalámbrico"
            "FIBER" -> "Fibra"
            "ONLY_TV_FIBER" -> "Solo TV"
            else -> null
        }
        val ip = (snapshot?.ip ?: snapshot?.clientIpAddress)?.takeIf { it.isNotBlank() }
        return PendingSubscriptionListItem(
            localId = localId,
            clientName = clientName,
            dni = dni ?: "—",
            createdAt = createdAt,
            status = status,
            detail = listOfNotNull(type, ip?.let { "IP $it" }, dni?.let { "DNI $it" }).joinToString(" · "),
            error = lastError?.takeIf { status == PendingSubscriptionStatus.FAILED || status == PendingSubscriptionStatus.CONFLICT },
        )
    }
}

private data class PendingClientSnapshot(
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("lastName") val lastName: String? = null,
    @SerializedName("dni") val dni: String? = null,
    @SerializedName("installationType") val installationType: String? = null,
    @SerializedName("ip") val ip: String? = null,
    @SerializedName("clientIpAddress") val clientIpAddress: String? = null,
)
