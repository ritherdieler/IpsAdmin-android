package com.dscorp.ispadmin.presentation.ui.features.networkmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.datasource.remote.NapDetailResponse
import com.dscorp.ispadmin.data.datasource.remote.NapMapResponse
import com.dscorp.ispadmin.data.datasource.remote.NetworkMapApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface NetworkMapGateway {
    suspend fun map(placeId: Int?): NapMapResponse
    suspend fun detail(napId: Int): NapDetailResponse
}

class ApiNetworkMapGateway(private val api: NetworkMapApi) : NetworkMapGateway {
    override suspend fun map(placeId: Int?) = api.map(placeId)
    override suspend fun detail(napId: Int) = api.detail(napId)
}

enum class MapLayer(val label: String) { NAPS("Cajas NAP"), MUFAS("Mufas") }

data class NetworkMapUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val map: NetworkMap? = null,
    val layers: Set<MapLayer> = MapLayer.entries.toSet(),
    val selectedId: Int? = null,
    val selected: NapDetail? = null,
)

sealed interface NetworkMapIntent {
    data object Load : NetworkMapIntent
    data class PlaceSelected(val placeId: Int) : NetworkMapIntent
    data class LayerToggled(val layer: MapLayer) : NetworkMapIntent
    data class PinSelected(val napId: Int) : NetworkMapIntent
    data object CardClosed : NetworkMapIntent
}

/** a-mas-05 «Mapa de red». */
class NetworkMapViewModel(private val gateway: NetworkMapGateway, private val mainImmediate: CoroutineDispatcher) : ViewModel() {
    private val _uiState = MutableStateFlow(NetworkMapUiState())
    val uiState: StateFlow<NetworkMapUiState> = _uiState
    private var cardJob: Job? = null

    fun onIntent(intent: NetworkMapIntent) {
        when (intent) {
            NetworkMapIntent.Load -> if (_uiState.value.map == null) load(null)
            is NetworkMapIntent.PlaceSelected -> load(intent.placeId)
            is NetworkMapIntent.LayerToggled -> _uiState.update {
                it.copy(layers = if (intent.layer in it.layers) it.layers - intent.layer else it.layers + intent.layer)
            }
            is NetworkMapIntent.PinSelected -> select(intent.napId)
            NetworkMapIntent.CardClosed -> _uiState.update { it.copy(selectedId = null, selected = null) }
        }
    }

    private fun load(placeId: Int?) {
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(loading = true, error = null, selectedId = null, selected = null) }
            try {
                val map = NetworkMap.from(gateway.map(placeId))
                _uiState.update { it.copy(loading = false, map = map) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(loading = false, error = LOAD_ERROR) }
            }
        }
    }

    private fun select(napId: Int) {
        _uiState.update { it.copy(selectedId = napId, selected = null) }
        cardJob?.cancel()
        cardJob = viewModelScope.launch(mainImmediate) {
            val detail = try {
                NapDetail.from(gateway.detail(napId))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            _uiState.update { if (it.selectedId == napId) it.copy(selected = detail) else it }
        }
    }

    private companion object {
        const val LOAD_ERROR = "No se pudo cargar el mapa. Revisa tu conexión y vuelve a intentar."
    }
}

data class NapDetailUiState(val loading: Boolean = false, val error: String? = null, val detail: NapDetail? = null)

/** a-mas-06 «Caja NAP». */
class NapDetailViewModel(private val gateway: NetworkMapGateway, private val mainImmediate: CoroutineDispatcher) : ViewModel() {
    private val _uiState = MutableStateFlow(NapDetailUiState())
    val uiState: StateFlow<NapDetailUiState> = _uiState

    fun load(napId: Int) {
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val detail = NapDetail.from(gateway.detail(napId))
                _uiState.update { it.copy(loading = false, detail = detail) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(loading = false, error = "No se pudo cargar la caja. Revisa tu conexión y vuelve a intentar.") }
            }
        }
    }
}
