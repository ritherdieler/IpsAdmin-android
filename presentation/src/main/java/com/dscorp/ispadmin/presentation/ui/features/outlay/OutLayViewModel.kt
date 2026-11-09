package com.dscorp.ispadmin.presentation.ui.features.outlay

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.domain.model.Outlay
import com.dscorp.ispadmin.domain.model.OutlaySaveResult
import com.dscorp.ispadmin.domain.model.OutlayType
import com.dscorp.ispadmin.domain.repository.OutlayRepository
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.formatSoles
import kotlinx.coroutines.CancellationException
import com.dscorp.ispadmin.domain.usecase.outlay.RegisterOutlayUseCase
import com.dscorp.ispadmin.observability.ObsBreadcrumbCategory
import com.dscorp.ispadmin.observability.ObservabilityClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class OutlayIntent {
    data class UpdateAmount(val amount: String) : OutlayIntent()
    data class UpdateDescription(val description: String) : OutlayIntent()
    data class UpdateDocumentCode(val documentCode: String) : OutlayIntent()
    data class UpdateCategory(val category: String) : OutlayIntent()
    data class UpdateCostCenter(val costCenter: String) : OutlayIntent()
    data class TakeImage(val uri: Uri) : OutlayIntent()
    data class RemoveImage(val index: Int) : OutlayIntent()
    data class UpdateType(val type: OutlayType) : OutlayIntent()
    object LoadLimit : OutlayIntent()
    object RegisterOutLay : OutlayIntent()
}

class OutLayViewModel(
    private val registerOutlayUseCase: RegisterOutlayUseCase,
    private val outlayRepository: OutlayRepository,
    private val summaryGateway: OutlaySummaryGateway,
    private val observabilityClient: ObservabilityClient
) : ViewModel() {

    private companion object {
        const val OBS_FEATURE = "outlay"
        const val OBS_SCREEN = "register_outlay"
    }

    private val _uiState = MutableStateFlow(OutlayUiState(canChooseType = outlayRepository.canRegisterFixedOutlay()))
    val uiState: StateFlow<OutlayUiState> get() = _uiState

    fun handleIntent(intent: OutlayIntent) {
        when (intent) {
            is OutlayIntent.UpdateType -> updateType(intent.type)
            OutlayIntent.LoadLimit -> loadLimit()

            is OutlayIntent.UpdateAmount -> {
                _uiState.update { currentState ->
                    val updatedOutlay = currentState.outlay.copy(
                        amount = intent.amount
                    )
                    currentState.copy(outlay = updatedOutlay)
                }
            }

            is OutlayIntent.UpdateDescription -> {
                _uiState.update { currentState ->
                    val updatedOutlay = currentState.outlay.copy(
                        description = intent.description
                    )
                    currentState.copy(outlay = updatedOutlay)
                }
            }

            is OutlayIntent.UpdateDocumentCode -> {
                _uiState.update { currentState ->
                    val updatedOutlay = currentState.outlay.copy(
                        document_code = intent.documentCode
                    )
                    currentState.copy(outlay = updatedOutlay)
                }
            }

            is OutlayIntent.UpdateCategory -> {
                _uiState.update { currentState ->
                    val updatedOutlay = currentState.outlay.copy(
                        category = intent.category
                    )
                    currentState.copy(outlay = updatedOutlay)
                }
            }

            is OutlayIntent.UpdateCostCenter -> {
                _uiState.update { currentState ->
                    val updatedOutlay = currentState.outlay.copy(
                        cost_center = intent.costCenter
                    )
                    currentState.copy(outlay = updatedOutlay)
                }
            }

            is OutlayIntent.TakeImage -> {

                if (uiState.value.photoList.isEmpty()) {
                    _uiState.update {
                        it.copy(photoList = uiState.value.photoList.toMutableList().apply {
                            add(intent.uri)
                        }, error = null)
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            error = "Solo puedes subir una imagen",
                            isLoading = false,
                            isSaved = false
                        )
                    }
                }
            }

            is OutlayIntent.RemoveImage -> {
                _uiState.update {
                    it.copy(photoList = uiState.value.photoList.toMutableList().apply {
                        removeAt(intent.index)
                    })
                }
            }

            is OutlayIntent.RegisterOutLay -> registerOutLay()
        }
    }

    private fun updateType(type: OutlayType) {
        val state = _uiState.value
        if (type == state.type || (type == OutlayType.FIXED && !state.canChooseType)) return
        val categories = if (type == OutlayType.FIXED) OutlayConstants.FIXED_CATEGORIES else OutlayConstants.CATEGORIES
        _uiState.update {
            it.copy(
                outlay = it.outlay.copy(
                    type = type,
                    category = it.outlay.category?.takeIf { category -> category in categories },
                ),
                // Fixed outlays carry no receipt.
                photoList = if (type == OutlayType.FIXED) emptyList() else it.photoList,
                error = null,
            )
        }
    }

    // The summary only helps the user avoid a rejection; if it cannot be read, the Core still enforces the cap.
    private fun loadLimit() {
        viewModelScope.launch {
            val summary = try {
                summaryGateway.summary()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            _uiState.update { it.copy(summary = summary) }
        }
    }

    private fun registerOutLay() = viewModelScope.launch {
        if (!uiState.value.canSubmit) {
            _uiState.update { it.copy(error = "Por favor complete todos los campos requeridos") }
            return@launch
        }

        observabilityClient.addBreadcrumb(
            category = ObsBreadcrumbCategory.USER_ACTION,
            message = "$OBS_FEATURE.register",
            data = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "category" to uiState.value.outlay.category)
        )
        _uiState.update { it.copy(isLoading = true, error = null) }

        registerOutlayUseCase(
            uiState.value.outlay,
            uiState.value.photoList.map { it.toString() }
        )
            .onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isSaved = true,
                        saved = result,
                        isLoading = false,
                        outlay = Outlay(type = it.outlay.type), // Limpiar formulario, conservando el tipo
                        photoList = emptyList()
                    )
                }
            }
            .onFailure { exception ->
                observabilityClient.reportError(
                    throwable = exception,
                    message = "Fallo al registrar egreso",
                    tags = mapOf("feature" to OBS_FEATURE, "screen" to OBS_SCREEN, "action" to "register")
                )
                _uiState.update {
                    it.copy(
                        error = exception.message ?: "Error desconocido",
                        isLoading = false
                    )
                }
            }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSaved() {
        _uiState.update { it.copy(isSaved = false) }
    }

}


data class OutlayUiState(
    val error: String? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    /** The Core answer to the last saved outlay (its id and whether the team was notified). */
    val saved: OutlaySaveResult? = null,
    val photoList: List<Uri> = emptyList(),
    val outlay: Outlay = Outlay(),
    /** Only administrators and accountants may register FIXED outlays. */
    val canChooseType: Boolean = false,
    /** Null while loading or on a Core without the endpoint; the Core then enforces the cap alone. */
    val summary: OutlaySummary? = null,
) {
    val type: OutlayType get() = outlay.type

    /** Set only for a VARIABLE outlay whose amount does not fit in what is left of the month. */
    val amountError: String?
        get() {
            val summary = summary ?: return null
            val amount = outlay.amount?.toDoubleOrNull() ?: return null
            if (type != OutlayType.VARIABLE || amount <= summary.available) return null
            return if (summary.available <= 0.0) {
                "Se alcanzó el límite mensual de egresos variables. Pide a un administrador que lo registre."
            } else {
                "Supera lo disponible por ${formatSoles(amount - summary.available)}. " +
                    "Ingresa ${formatSoles(summary.available)} o menos, o pide a un administrador que lo registre."
            }
        }

    /** Why the register button is off, shown above it; null when it can be pressed. */
    val disabledReason: String?
        get() = when {
            amountError != null -> "El monto supera lo disponible del mes"
            outlay.amount?.toDoubleOrNull()?.let { it > 0 } != true -> "Falta el monto"
            outlay.description.isNullOrEmpty() -> "Falta la descripción"
            outlay.category.isNullOrEmpty() -> "Falta la categoría"
            else -> null
        }

    val canSubmit: Boolean get() = disabledReason == null && !isLoading
}
