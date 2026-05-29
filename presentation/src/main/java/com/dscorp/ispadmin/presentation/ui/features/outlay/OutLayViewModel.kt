package com.dscorp.ispadmin.presentation.ui.features.outlay

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.domain.model.Outlay
import com.dscorp.ispadmin.domain.usecase.outlay.RegisterOutlayUseCase
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
    object RegisterOutLay : OutlayIntent()
}

class OutLayViewModel(
    private val registerOutlayUseCase: RegisterOutlayUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(OutlayUiState())
    val uiState: StateFlow<OutlayUiState> get() = _uiState

    fun handleIntent(intent: OutlayIntent) {
        when (intent) {
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

    private fun registerOutLay() = viewModelScope.launch {
        if (!uiState.value.outlay.isValid()) {
            _uiState.update { it.copy(error = "Por favor complete todos los campos requeridos") }
            return@launch
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        registerOutlayUseCase(
            uiState.value.outlay,
            uiState.value.photoList.map { it.toString() }
        )
            .onSuccess {
                _uiState.update {
                    it.copy(
                        isSaved = true,
                        isLoading = false,
                        outlay = Outlay(), // Limpiar formulario
                        photoList = emptyList()
                    )
                }
            }
            .onFailure { exception ->
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
    val photoList: List<Uri> = emptyList(),
    val outlay: Outlay = Outlay()
)