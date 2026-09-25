package com.dscorp.ispadmin.presentation.ui.features.subscription.provisioning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.domain.model.ProvisioningAction
import com.dscorp.ispadmin.domain.model.ProvisioningProgress
import com.dscorp.ispadmin.domain.repository.ProvisioningRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ProvisioningUiState(
    val subscriptionId: Int? = null,
    val progress: ProvisioningProgress? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val confirmCancel: Boolean = false,
    val error: String? = null,
)

sealed interface ProvisioningIntent {
    data class Load(val subscriptionId: Int) : ProvisioningIntent
    data object Retry : ProvisioningIntent
    data object AskCancel : ProvisioningIntent
    data object ConfirmCancel : ProvisioningIntent
    data object DismissCancel : ProvisioningIntent
    data object Stop : ProvisioningIntent
}

class ProvisioningViewModel(
    private val repository: ProvisioningRepository,
    private val mainImmediate: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {
    private val state = MutableStateFlow(ProvisioningUiState())
    val uiState = state.asStateFlow()
    private var pollJob: Job? = null
    private var actionJob: Job? = null

    fun onIntent(intent: ProvisioningIntent) {
        when (intent) {
            is ProvisioningIntent.Load -> load(intent.subscriptionId)
            ProvisioningIntent.Retry -> submit(cancel = false)
            ProvisioningIntent.AskCancel -> if (state.value.progress?.let { it.canCancel || it.canRetryCancellation } == true) {
                state.update { it.copy(confirmCancel = true) }
            }
            ProvisioningIntent.ConfirmCancel -> if (state.value.confirmCancel) submit(cancel = true)
            ProvisioningIntent.DismissCancel -> state.update { it.copy(confirmCancel = false) }
            ProvisioningIntent.Stop -> stop()
        }
    }

    private fun stop() {
        pollJob?.cancel()
        actionJob?.cancel()
        pollJob = null
        actionJob = null
        state.update { it.copy(isLoading = false, isSubmitting = false, confirmCancel = false) }
    }

    private fun load(subscriptionId: Int) {
        if (actionJob?.isActive == true) return
        pollJob?.cancel()
        if (state.value.subscriptionId != subscriptionId) state.value = ProvisioningUiState(subscriptionId = subscriptionId)
        pollJob = viewModelScope.launch(mainImmediate) {
            do {
                state.update { it.copy(isLoading = true) }
                val result = repository.latest(subscriptionId)
                result.fold(
                    onSuccess = { progress -> state.update { it.copy(progress = progress, isLoading = false, error = null) } },
                    onFailure = { state.update { it.copy(isLoading = false, error = "No se pudo consultar el registro. Actualiza su estado.") } },
                )
                if (result.isFailure || !isActiveProgress(state.value.progress)) break
                delay(POLL_INTERVAL_MS)
            } while (true)
        }
    }

    private fun submit(cancel: Boolean) {
        if (actionJob?.isActive == true || state.value.error != null) return
        val progress = state.value.progress ?: return
        if (cancel && !progress.canCancel && !progress.canRetryCancellation) return
        if (!cancel && !progress.canRetry) return
        pollJob?.cancel()
        state.update { it.copy(isSubmitting = true, confirmCancel = false) }
        actionJob = viewModelScope.launch(mainImmediate) {
            val operation = progress.operation
            val action = ProvisioningAction(subscriptionId = operation.subscriptionId,
                operationId = operation.id, expectedRevision = operation.revision)
            val result = if (cancel) repository.cancel(action) else repository.retry(action)
            result.fold(
                onSuccess = { next -> state.update { it.copy(progress = next, isSubmitting = false, error = null) } },
                onFailure = { state.update { it.copy(isSubmitting = false, error = "No se confirmó la acción. Actualiza el estado antes de reintentar.") } },
            )
            actionJob = null
            if (result.isSuccess && isActiveProgress(state.value.progress)) load(operation.subscriptionId)
        }
    }

    private fun isActiveProgress(progress: ProvisioningProgress?): Boolean = progress?.operation?.state in
        setOf("PENDING", "RUNNING", "WAITING", "CANCEL_REQUESTED", "CANCELLING")

    companion object { private const val POLL_INTERVAL_MS = 3000L }
}
