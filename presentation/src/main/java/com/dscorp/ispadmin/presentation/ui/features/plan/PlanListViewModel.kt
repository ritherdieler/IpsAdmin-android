package com.dscorp.ispadmin.presentation.ui.features.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.Plan
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.usecase.plan.GetPlanListUseCase
import com.dscorp.ispadmin.domain.usecase.plan.UpdatePlanUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlanListUIState(
    val isLoading: Boolean = false,
    val plans: List<PlanResponse> = emptyList(),
    val filteredPlans: List<PlanResponse> = emptyList(),
    val error: String? = null,
    val isSuccess: Boolean = false,
    val form: PlanEditForm = PlanEditForm(),
    val showEditDialog: Boolean = false,
    val selectedPlan: PlanResponse? = null,
    val editedName: String = "",
    val editedPrice: String = "",
    val editedDownloadSpeed: String = "",
    val editedUploadSpeed: String = "",
    val selectedType: InstallationType? = null,
    val isFilterExpanded: Boolean = false
)

data class PlanEditForm(
    val selectedPlan: PlanResponse? = null,
    val touched: Boolean = false
) {
    val isValid: Boolean get() = selectedPlan != null
}

class PlanListViewModel(
    private val getPlanListUseCase: GetPlanListUseCase,
    private val updatePlanUseCase: UpdatePlanUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanListUIState())
    val uiState: StateFlow<PlanListUIState> = _uiState.asStateFlow()

    init {
        loadPlans()
    }

    fun loadPlans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            getPlanListUseCase().fold(
                onSuccess = { plans ->
                    _uiState.update { currentState -> 
                        val filteredPlans = currentState.selectedType?.let { type ->
                            plans.filter { it.type == type }
                        } ?: plans
                        
                        currentState.copy(
                            isLoading = false,
                            plans = plans,
                            filteredPlans = filteredPlans
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Error al cargar los planes"
                        )
                    }
                }
            )
        }
    }

    fun updateSelectedPlan(plan: PlanResponse?) {
        _uiState.update { currentState ->
            currentState.copy(
                form = currentState.form.copy(
                    selectedPlan = plan,
                    touched = true
                )
            )
        }
    }

    fun updatePlan(plan: Plan) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            if (plan.id == null) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "El plan seleccionado no es válido"
                    )
                }
                return@launch
            }

            updatePlanUseCase(plan).fold(
                onSuccess = { updatedPlan ->
                    _uiState.update { currentState ->
                        val updatedPlans = currentState.plans.map {
                            if (it.id == updatedPlan.id) updatedPlan else it
                        }
                        val filteredPlans = currentState.selectedType?.let { type ->
                            updatedPlans.filter { it.type == type }
                        } ?: updatedPlans
                        currentState.copy(
                            isLoading = false,
                            plans = updatedPlans,
                            filteredPlans = filteredPlans,
                            isSuccess = true,
                            showEditDialog = false,
                            selectedPlan = null,
                            editedName = "",
                            editedPrice = "",
                            editedDownloadSpeed = "",
                            editedUploadSpeed = ""
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Error al actualizar el plan"
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(isSuccess = false) }
    }

    fun showEditDialog(plan: PlanResponse) {
        _uiState.update { 
            it.copy(
                showEditDialog = true,
                selectedPlan = plan,
                editedName = plan.name ?: "",
                editedPrice = plan.price?.toString() ?: "0.0",
                editedDownloadSpeed = plan.downloadSpeed ?: "",
                editedUploadSpeed = plan.uploadSpeed ?: ""
            )
        }
    }

    fun hideEditDialog() {
        _uiState.update { 
            it.copy(
                showEditDialog = false,
                selectedPlan = null,
                editedName = "",
                editedPrice = "",
                editedDownloadSpeed = "",
                editedUploadSpeed = ""
            )
        }
    }

    fun updateEditedName(name: String) {
        _uiState.update { it.copy(editedName = name) }
    }

    fun updateEditedPrice(price: String) {
        _uiState.update { it.copy(editedPrice = price) }
    }

    fun updateEditedDownloadSpeed(speed: String) {
        _uiState.update { it.copy(editedDownloadSpeed = speed) }
    }

    fun updateEditedUploadSpeed(speed: String) {
        _uiState.update { it.copy(editedUploadSpeed = speed) }
    }

    fun updateSelectedType(type: InstallationType?) {
        _uiState.update { currentState ->
            val filteredPlans = type?.let { selectedType ->
                currentState.plans.filter { it.type == selectedType }
            } ?: currentState.plans
            
            currentState.copy(
                selectedType = type,
                filteredPlans = filteredPlans
            )
        }
    }

    fun toggleFilterExpanded() {
        _uiState.update { it.copy(isFilterExpanded = !it.isFilterExpanded) }
    }
} 