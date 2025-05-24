package com.dscorp.ispadmin.presentation.ui.features.migration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.apirequestmodel.MigrationRequest
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MigrationViewModel(private val repository: IRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<MigrationUiState>(MigrationUiState.Empty)
    val uiState = _uiState.stateIn(viewModelScope, SharingStarted.Lazily, MigrationUiState.Empty)

    fun doMigration(migrationRequest: MigrationRequest) = viewModelScope.launch {
        if (migrationRequest.isValid().not()) {
            _uiState.value = MigrationUiState.Error(Exception("Datos incorrectos"))
            return@launch
        } else {
            try {
                _uiState.emit(MigrationUiState.Loading)
                val response =
                    repository.doMigration(migrationRequest)
                _uiState.emit(MigrationUiState.Success(response))
            } catch (e: Exception) {
                _uiState.emit(MigrationUiState.Error(e))
            }
        }

    }

    fun getMigrationFormData(subscriptionId: Int) = viewModelScope.launch {
        try {
            val subscription = async { repository.subscriptionById(subscriptionId) }.await()
            _uiState.emit(MigrationUiState.Loading)
            val unconfirmedOnus = async { repository.getUnconfirmedOnus() }.await()
            val plans = async { repository.getPlans() }.await()
                .filter { it.type == InstallationType.FIBER }
            _uiState.emit(MigrationUiState.FormDataReady(plans, unconfirmedOnus, subscription))
        } catch (e: Exception) {
            _uiState.emit(MigrationUiState.Error(e))
        }
    }

}

sealed class MigrationUiState {

    object Empty : MigrationUiState()
    object Loading : MigrationUiState()
    data class Success(val subscriptionResponse: SubscriptionResponse) : MigrationUiState()
    data class Error(val error: Exception) : MigrationUiState()
    data class FormDataReady(
        val plans: List<PlanResponse>,
        val unconfirmedOnus: List<Onu>,
        val subscription: SubscriptionResponse
    ) :
        MigrationUiState()
}
