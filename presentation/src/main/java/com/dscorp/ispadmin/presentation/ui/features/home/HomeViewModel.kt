package com.dscorp.ispadmin.presentation.ui.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.datasource.remote.HomeApi
import com.dscorp.ispadmin.data.datasource.remote.HomeSummaryResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

interface HomeGateway {
    suspend fun summary(): HomeSummaryResponse
}

data class HomeUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val home: SecretaryHome? = null,
    val admin: AdminHome? = null,
)

sealed interface HomeIntent {
    data object Load : HomeIntent
    data object Refresh : HomeIntent
}

class HomeViewModel(
    private val gateway: HomeGateway,
    private val mainImmediate: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Load -> if (_uiState.value.home == null && !_uiState.value.loading) refresh()
            HomeIntent.Refresh -> refresh()
        }
    }

    private fun refresh() {
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(loading = true) }
            try {
                val summary = gateway.summary()
                _uiState.update { it.copy(loading = false, error = null, home = SecretaryHome.from(summary), admin = AdminHome.from(summary)) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(loading = false, error = LOAD_ERROR) }
            }
        }
    }

    private companion object {
        const val LOAD_ERROR = "No se pudo actualizar el inicio. Revisa tu conexión y vuelve a intentar."
    }
}

class ApiHomeGateway(private val api: HomeApi) : HomeGateway {
    override suspend fun summary(): HomeSummaryResponse = api.summary()
}
