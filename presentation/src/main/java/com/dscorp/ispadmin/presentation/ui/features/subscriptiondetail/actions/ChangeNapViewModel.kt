package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.datasource.remote.NapMoveApi
import com.dscorp.ispadmin.data.datasource.remote.NapMoveRequest
import com.dscorp.ispadmin.data.datasource.remote.NapOptionsResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface ChangeNapGateway {
    suspend fun options(subscriptionId: Int): NapOptionsResponse
    /** Moves the ONU and returns "LV-15-1 · puerto 5". */
    suspend fun move(subscriptionId: Int, napId: Int, port: Int): String
}

class ApiChangeNapGateway(private val api: NapMoveApi) : ChangeNapGateway {
    override suspend fun options(subscriptionId: Int) = api.options(subscriptionId)
    override suspend fun move(subscriptionId: Int, napId: Int, port: Int): String {
        val moved = api.move(subscriptionId, NapMoveRequest(napId, port))
        return listOfNotNull(moved.napCode.takeIf { it.isNotBlank() }, moved.port?.let { "puerto $it" }).joinToString(" · ")
    }
}

data class ChangeNapUiState(
    val subscriptionId: Int = 0,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val model: ChangeNap? = null,
    val sort: NapSort = NapSort.NEAREST,
    val napId: Int? = null,
    val port: Int? = null,
    val done: String? = null,
) {
    val canSubmit: Boolean get() = napId != null && port != null && !saving
    val action: String
        get() = if (napId != null && port != null && model != null) "Mover a ${model.code(napId)} · puerto $port" else "Mover"
}

sealed interface ChangeNapIntent {
    data class Load(val subscriptionId: Int) : ChangeNapIntent
    data class SortSelected(val sort: NapSort) : ChangeNapIntent
    data class NapSelected(val napId: Int) : ChangeNapIntent
    data class PortSelected(val port: Int) : ChangeNapIntent
    data object Submit : ChangeNapIntent
}

class ChangeNapViewModel(private val gateway: ChangeNapGateway, private val mainImmediate: CoroutineDispatcher) : ViewModel() {
    private val _uiState = MutableStateFlow(ChangeNapUiState())
    val uiState: StateFlow<ChangeNapUiState> = _uiState

    fun onIntent(intent: ChangeNapIntent) {
        when (intent) {
            is ChangeNapIntent.Load -> load(intent.subscriptionId)
            is ChangeNapIntent.SortSelected -> _uiState.update { it.copy(sort = intent.sort) }
            is ChangeNapIntent.NapSelected -> _uiState.update { state ->
                val port = state.model?.firstFree(intent.napId) ?: return@update state
                state.copy(napId = intent.napId, port = port, error = null)
            }
            is ChangeNapIntent.PortSelected -> _uiState.update { state ->
                val napId = state.napId ?: return@update state
                if (state.model?.isFree(napId, intent.port) == true) state.copy(port = intent.port, error = null) else state
            }
            ChangeNapIntent.Submit -> submit()
        }
    }

    private fun load(subscriptionId: Int) {
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(subscriptionId = subscriptionId, loading = true, error = null) }
            try {
                val model = ChangeNap.from(gateway.options(subscriptionId))
                _uiState.update { it.copy(loading = false, model = model) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(loading = false, error = LOAD_ERROR) }
            }
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        val napId = state.napId ?: return
        val port = state.port ?: return
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(saving = true, error = null) }
            try {
                val label = gateway.move(state.subscriptionId, napId, port)
                _uiState.update { it.copy(saving = false, done = "Caja cambiada a $label") }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (taken: retrofit2.HttpException) {
                // 409: someone took the port meanwhile; reload so the grid shows it.
                val reason = if (taken.code() == 409) PORT_TAKEN else MOVE_ERROR
                _uiState.update { it.copy(saving = false, error = reason) }
                if (taken.code() == 409) load(state.subscriptionId)
            } catch (_: Exception) {
                _uiState.update { it.copy(saving = false, error = MOVE_ERROR) }
            }
        }
    }

    private companion object {
        const val LOAD_ERROR = "No se pudieron cargar las cajas. Revisa tu conexión y vuelve a intentar."
        const val PORT_TAKEN = "Ese puerto se ocupó hace un momento. Elige otro."
        const val MOVE_ERROR = "No se pudo mover la ONU. Revisa tu conexión y vuelve a intentar."
    }
}
