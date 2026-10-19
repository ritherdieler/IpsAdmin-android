package com.dscorp.ispadmin.presentation.ui.features.oltadministrator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.datasource.remote.OltToolsApi
import com.dscorp.ispadmin.data.datasource.remote.OnuLiveStatusResponse
import com.dscorp.ispadmin.data.datasource.remote.OnuLookupResponse
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.badge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.toTitleCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.util.Locale

interface OltToolsGateway {
    /** Null when the OLT does not know the serial. */
    suspend fun lookup(sn: String): OnuLookupResponse?
    suspend fun liveStatus(externalId: String): OnuLiveStatusResponse
    suspend fun delete(externalId: String)
}

class ApiOltToolsGateway(private val api: OltToolsApi) : OltToolsGateway {
    override suspend fun lookup(sn: String): OnuLookupResponse? {
        val response = api.lookup(sn)
        return when {
            response.isSuccessful -> response.body()
            response.code() == NOT_FOUND -> null
            else -> throw HttpException(response)
        }
    }
    override suspend fun liveStatus(externalId: String) = api.liveStatus(externalId)
    override suspend fun delete(externalId: String) {
        val response = api.delete(externalId)
        if (!response.isSuccessful) throw HttpException(response)
    }

    private companion object {
        const val NOT_FOUND = 404
    }
}

/** a-mas-08: one ONU as the OLT has it, with what deleting it would affect. */
data class OnuCard(
    val sn: String,
    val externalId: String,
    val model: String?,
    val rows: List<Pair<String, String>>,
    val deleteWarning: String,
    val linkedWarning: String?,
) {
    companion object {
        fun from(lookup: OnuLookupResponse, live: OnuLiveStatusResponse?): OnuCard {
            val place = listOfNotNull(lookup.oltName, lookup.slot).joinToString(" ")
            val linked = lookup.subscription
            val linkedName = linked?.name?.toTitleCase()
            val linkedStatus = linked?.status?.let { runCatching { ServiceStatus.valueOf(it) }.getOrNull() }
            return OnuCard(
                sn = lookup.sn,
                externalId = lookup.externalId,
                model = lookup.model,
                rows = listOfNotNull(
                    listOfNotNull(lookup.oltName, lookup.slot, lookup.onuId?.let { "ONT $it" }).joinToString(" · ").takeIf { it.isNotBlank() }?.let { "Ubicación" to it },
                    "Estado" to if (linked == null) "Sin suscripción vinculada"
                    else listOfNotNull("Suscripción ${linked.id}", linkedName, linkedStatus?.badge()?.text).joinToString(" · "),
                    live?.runState?.let { "Señal" to if (it.equals("online", ignoreCase = true)) "En línea" else "Sin señal" },
                    live?.onuRxDbm?.let { "Potencia" to String.format(Locale.US, "%.1f dBm", it).replace("-", "−") },
                ),
                deleteWarning = "Se borra su configuración en ${place.ifBlank { "la OLT" }}. Si alguien la usa, pierde el servicio. No se puede deshacer.",
                linkedWarning = linked?.takeIf { linkedStatus != ServiceStatus.CANCELLED }
                    ?.let { "La usa la suscripción ${it.id} ($linkedName), que perderá el servicio." },
            )
        }
    }
}

data class OltToolsUiState(
    val query: String = "",
    val searching: Boolean = false,
    val deleting: Boolean = false,
    val error: String? = null,
    val card: OnuCard? = null,
    val message: String? = null,
)

sealed interface OltToolsIntent {
    data class QueryChanged(val query: String) : OltToolsIntent
    data object Search : OltToolsIntent
    data object Delete : OltToolsIntent
    data object MessageShown : OltToolsIntent
}

class OltToolsViewModel(private val gateway: OltToolsGateway, private val mainImmediate: CoroutineDispatcher) : ViewModel() {
    private val _uiState = MutableStateFlow(OltToolsUiState())
    val uiState: StateFlow<OltToolsUiState> = _uiState

    fun onIntent(intent: OltToolsIntent) {
        when (intent) {
            is OltToolsIntent.QueryChanged -> _uiState.update { it.copy(query = intent.query.uppercase(Locale.ROOT), error = null) }
            OltToolsIntent.Search -> search()
            OltToolsIntent.Delete -> delete()
            OltToolsIntent.MessageShown -> _uiState.update { it.copy(message = null) }
        }
    }

    private fun search() {
        val sn = _uiState.value.query.trim()
        if (sn.isEmpty() || _uiState.value.searching) return
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(searching = true, error = null, card = null) }
            try {
                val lookup = gateway.lookup(sn)
                if (lookup == null) {
                    _uiState.update { it.copy(searching = false, error = NOT_FOUND) }
                    return@launch
                }
                // The live reading comes from the Gateway and may be off; the card still shows.
                val live = try {
                    gateway.liveStatus(lookup.externalId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
                _uiState.update { it.copy(searching = false, card = OnuCard.from(lookup, live)) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(searching = false, error = SEARCH_ERROR) }
            }
        }
    }

    private fun delete() {
        val card = _uiState.value.card ?: return
        if (_uiState.value.deleting) return
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(deleting = true, error = null) }
            try {
                gateway.delete(card.externalId)
                _uiState.update { it.copy(deleting = false, card = null, message = "ONU ${card.sn} eliminada de la OLT") }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(deleting = false, error = DELETE_ERROR) }
            }
        }
    }

    private companion object {
        const val NOT_FOUND = "No hay una ONU con ese serial en la OLT."
        const val SEARCH_ERROR = "No se pudo consultar la OLT. Revisa tu conexión y vuelve a intentar."
        const val DELETE_ERROR = "No se pudo eliminar la ONU. La OLT no confirmó el cambio."
    }
}
