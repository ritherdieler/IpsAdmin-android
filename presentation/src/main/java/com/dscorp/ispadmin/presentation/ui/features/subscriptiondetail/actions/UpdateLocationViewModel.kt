package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.datasource.remote.CoverageResponse
import com.dscorp.ispadmin.data.datasource.remote.LocationApi
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.distanceLabel
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.napDistanceMeters
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.toTitleCase
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

interface UpdateLocationGateway {
    suspend fun subscription(subscriptionId: Int): SubscriptionResponse
    suspend fun coverage(latitude: Double, longitude: Double, assignedPlace: String?): CoverageResponse
    suspend fun save(subscriptionId: Int, latitude: Double, longitude: Double)
}

class ApiUpdateLocationGateway(private val repository: IRepository, private val api: LocationApi) : UpdateLocationGateway {
    override suspend fun subscription(subscriptionId: Int) = repository.subscriptionById(subscriptionId)
    override suspend fun coverage(latitude: Double, longitude: Double, assignedPlace: String?) = api.coverage(latitude, longitude, assignedPlace)
    override suspend fun save(subscriptionId: Int, latitude: Double, longitude: Double) {
        repository.updateSubscriptionLocation(subscriptionId, latitude, longitude)
    }
}

data class PlaceCheck(val text: String, val inside: Boolean)

data class UpdateLocationUiState(
    val subscriptionId: Int = 0,
    val loaded: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    /** Shown only after «Mi ubicación»: how precise the GPS fix was. */
    val accuracy: String? = null,
    val placeCheck: PlaceCheck? = null,
    val napNote: String? = null,
    val reference: String? = null,
    val nap: NapBoxResponse? = null,
    val placeName: String? = null,
    val saving: Boolean = false,
    val error: String? = null,
    val done: String? = null,
) {
    val coordinates: String get() = String.format(Locale.US, "%.4f, %.4f", latitude, longitude)
}

sealed interface UpdateLocationIntent {
    data class Load(val subscriptionId: Int) : UpdateLocationIntent
    data class CenterMoved(val latitude: Double, val longitude: Double) : UpdateLocationIntent
    data class MyLocation(val latitude: Double, val longitude: Double, val accuracy: Float?) : UpdateLocationIntent
    data object Submit : UpdateLocationIntent
}

/** a-sus-11 «Actualizar ubicación»: the map center is the new point. */
class UpdateLocationViewModel(private val gateway: UpdateLocationGateway, private val mainImmediate: CoroutineDispatcher) : ViewModel() {
    private val _uiState = MutableStateFlow(UpdateLocationUiState())
    val uiState: StateFlow<UpdateLocationUiState> = _uiState
    private var checkJob: Job? = null

    fun onIntent(intent: UpdateLocationIntent) {
        when (intent) {
            is UpdateLocationIntent.Load -> load(intent.subscriptionId)
            is UpdateLocationIntent.CenterMoved -> {
                // The map settling on the point already shown (e.g. after «Mi ubicación») keeps its GPS precision.
                val state = _uiState.value
                if (abs(state.latitude - intent.latitude) < SAME_POINT && abs(state.longitude - intent.longitude) < SAME_POINT) return
                moveTo(intent.latitude, intent.longitude, accuracy = null, debounce = true)
            }
            is UpdateLocationIntent.MyLocation -> moveTo(intent.latitude, intent.longitude, accuracy = intent.accuracy, debounce = false)
            UpdateLocationIntent.Submit -> submit()
        }
    }

    private fun load(subscriptionId: Int) {
        viewModelScope.launch(mainImmediate) {
            try {
                val subscription = gateway.subscription(subscriptionId)
                val location = subscription.location
                _uiState.update {
                    it.copy(
                        subscriptionId = subscriptionId,
                        loaded = true,
                        latitude = location?.latitude ?: subscription.place?.latitude?.toDouble() ?: 0.0,
                        longitude = location?.longitude ?: subscription.place?.longitude?.toDouble() ?: 0.0,
                        nap = subscription.napBox,
                        placeName = subscription.place?.name,
                        reference = subscription.address?.takeIf { address -> address.isNotBlank() },
                        error = null,
                    ).withNapNote()
                }
                check()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(error = LOAD_ERROR) }
            }
        }
    }

    private fun moveTo(latitude: Double, longitude: Double, accuracy: Float?, debounce: Boolean) {
        _uiState.update {
            it.copy(
                latitude = latitude,
                longitude = longitude,
                accuracy = accuracy?.let { meters -> "Precisión ±${meters.roundToInt()} m" } ?: if (debounce) null else it.accuracy,
            ).withNapNote()
        }
        checkJob?.cancel()
        checkJob = viewModelScope.launch(mainImmediate) {
            if (debounce) delay(SETTLE_MS)
            check()
        }
    }

    private suspend fun check() {
        val state = _uiState.value
        val place = state.placeName?.toTitleCase()
        val check = try {
            val coverage = gateway.coverage(state.latitude, state.longitude, state.placeName)
            val detected = coverage.detectedPlaceName?.toTitleCase()
            when {
                coverage.matchesAssignedPlace == true -> PlaceCheck("Dentro de ${place ?: detected}", inside = true)
                place != null && detected != null -> PlaceCheck("Fuera de $place · cae en $detected", inside = false)
                place != null -> PlaceCheck("Fuera de $place", inside = false)
                else -> null
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        _uiState.update { it.copy(placeCheck = check) }
    }

    private fun submit() {
        val state = _uiState.value
        if (!state.loaded || state.saving) return
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(saving = true, error = null) }
            try {
                gateway.save(state.subscriptionId, state.latitude, state.longitude)
                _uiState.update { it.copy(saving = false, done = "Ubicación actualizada") }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(saving = false, error = SAVE_ERROR) }
            }
        }
    }

    private fun UpdateLocationUiState.withNapNote(): UpdateLocationUiState {
        val box = nap ?: return copy(napNote = null)
        val meters = napDistanceMeters(LatLng(latitude, longitude), box) ?: return copy(napNote = null)
        return copy(napNote = "La caja NAP actual (${box.code}) queda a ${distanceLabel(meters)}.")
    }

    private companion object {
        const val SETTLE_MS = 500L
        const val SAME_POINT = 1e-6
        const val LOAD_ERROR = "No se pudo cargar la suscripción. Revisa tu conexión y vuelve a intentar."
        const val SAVE_ERROR = "No se pudo guardar la ubicación. Revisa tu conexión y vuelve a intentar."
    }
}
