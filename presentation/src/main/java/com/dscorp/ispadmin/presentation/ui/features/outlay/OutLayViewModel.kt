package com.dscorp.ispadmin.presentation.ui.features.outlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.domain.model.Outlay
import com.example.data2.data.repository.IRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OutLayViewModel(
    private val repository: IRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<OutlayUiState>(OutlayUiState.Idle)
    val uiState: StateFlow<OutlayUiState> get() = _uiState

    fun registerOutLay(outLay: Outlay) = viewModelScope.launch {

        if (outLay.isValid().not()) {
            _uiState.value = OutlayUiState.Error
            return@launch
        }

        try {
            _uiState.value = OutlayUiState.Loading
            repository.saveOutLay(
                outLay.apply { responsibleId = repository.getUserSession()!!.id }
            )
            _uiState.value = OutlayUiState.Saved
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.value = OutlayUiState.Error
        }
    }

    fun updateState(state: OutlayUiState) {
        _uiState.value = state
    }

}


sealed class OutlayUiState {
    object Idle : OutlayUiState()
    object Loading : OutlayUiState()
    object Saved : OutlayUiState()
    object Error : OutlayUiState()
}