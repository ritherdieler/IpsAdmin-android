package com.dscorp.ispadmin.presentation.ui.features.outlay

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.Outlay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class OutlayIntent {
    data class RegisterOutLay(val outLay: Outlay) : OutlayIntent()
    data class TakeImage(val uri: Uri) : OutlayIntent()
    data class RemoveImage(val index: Int) : OutlayIntent()
}

class OutLayViewModel(
    private val repository: IRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(OutlayUiState())
    val uiState: StateFlow<OutlayUiState> get() = _uiState

    fun handleIntent(intent: OutlayIntent) {
        when (intent) {
            is OutlayIntent.RegisterOutLay -> {

            }

            is OutlayIntent.TakeImage -> {
                _uiState.update {
                    it.copy(photoList = uiState.value.photoList.toMutableList().apply {
                        add(intent.uri)
                    })
                }
            }

            is OutlayIntent.RemoveImage -> {
                _uiState.update {
                    it.copy(photoList = uiState.value.photoList.toMutableList().apply {
                        removeAt(intent.index)
                    })
                }
            }
        }
    }

    fun registerOutLay(outLay: Outlay) = viewModelScope.launch {

        if (outLay.isValid().not()) {
            _uiState.value = OutlayUiState(error = "Datos incompletos")
            return@launch
        }

        try {
            _uiState.value = OutlayUiState(isLoading = true)
            repository.saveOutLay(
                outLay.apply { responsibleId = repository.getUserSession()!!.id }
            )
            _uiState.value = OutlayUiState(isSaved = true, isLoading = false)
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.value = OutlayUiState(error = e.message, isLoading = false)
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
    val photoList: List<Uri> = emptyList()
)