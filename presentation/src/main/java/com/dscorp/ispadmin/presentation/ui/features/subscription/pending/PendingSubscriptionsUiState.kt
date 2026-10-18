package com.dscorp.ispadmin.presentation.ui.features.subscription.pending

import com.dscorp.ispadmin.domain.model.PendingSubscriptionStatus

data class PendingSubscriptionsUiState(
    val items: List<PendingSubscriptionListItem> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val errorMessage: String? = null,
    val failedCount: Int = 0,
    val lastError: String? = null
)

sealed interface PendingSubscriptionsUiEvent {
    data class ShowSuccessSnackbar(val message: String) : PendingSubscriptionsUiEvent
}

data class PendingSubscriptionListItem(
    val localId: String,
    val clientName: String,
    val dni: String,
    val createdAt: Long,
    val status: PendingSubscriptionStatus,
    val detail: String = "",
    val error: String? = null,
) {
    val statusLabel: String
        get() = when (status) {
            PendingSubscriptionStatus.PENDING -> "Esperando señal"
            PendingSubscriptionStatus.SYNCING -> "Enviando…"
            PendingSubscriptionStatus.FAILED, PendingSubscriptionStatus.CONFLICT -> "No se pudo enviar"
        }

    val failed: Boolean get() = status == PendingSubscriptionStatus.FAILED || status == PendingSubscriptionStatus.CONFLICT
}

sealed interface PendingSubscriptionsIntent {
    data object Load : PendingSubscriptionsIntent
    data object Sync : PendingSubscriptionsIntent
    data class Discard(val localId: String) : PendingSubscriptionsIntent
    data object ClearError : PendingSubscriptionsIntent
}
