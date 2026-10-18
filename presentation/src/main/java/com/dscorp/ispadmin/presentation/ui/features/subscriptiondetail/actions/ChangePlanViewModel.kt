package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.apirequestmodel.UpdateSubscriptionPlanBody
import com.dscorp.ispadmin.data.datasource.remote.PlanApi
import com.dscorp.ispadmin.data.datasource.remote.PlanCountResponse
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface ChangePlanGateway {
    suspend fun subscription(subscriptionId: Int): SubscriptionResponse
    suspend fun plans(): List<PlanCountResponse>
    suspend fun changePlan(subscriptionId: Int, planId: Int)
}

class ApiChangePlanGateway(private val repository: IRepository, private val planApi: PlanApi) : ChangePlanGateway {
    override suspend fun subscription(subscriptionId: Int) = repository.subscriptionById(subscriptionId)
    override suspend fun plans() = planApi.allWithCounts()
    override suspend fun changePlan(subscriptionId: Int, planId: Int) {
        repository.updateSubscriptionPlan(UpdateSubscriptionPlanBody(planId = planId.toString(), subscriptionId = subscriptionId))
    }
}

data class ChangePlanUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val model: ChangePlan? = null,
    val selectedId: Int? = null,
    /** Message for the ficha once the plan changed. */
    val done: String? = null,
) {
    val change: PlanChange? get() = selectedId?.let { model?.change(it) }
}

sealed interface ChangePlanIntent {
    data class Load(val subscriptionId: Int) : ChangePlanIntent
    data class Select(val planId: Int) : ChangePlanIntent
    data object Submit : ChangePlanIntent
}

class ChangePlanViewModel(private val gateway: ChangePlanGateway, private val mainImmediate: CoroutineDispatcher) : ViewModel() {
    private val _uiState = MutableStateFlow(ChangePlanUiState())
    val uiState: StateFlow<ChangePlanUiState> = _uiState

    fun onIntent(intent: ChangePlanIntent) {
        when (intent) {
            is ChangePlanIntent.Load -> load(intent.subscriptionId)
            is ChangePlanIntent.Select -> _uiState.update { it.copy(selectedId = intent.planId, error = null) }
            ChangePlanIntent.Submit -> submit()
        }
    }

    private fun load(subscriptionId: Int) {
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val model = ChangePlan.from(gateway.subscription(subscriptionId), gateway.plans())
                _uiState.update { it.copy(loading = false, model = model, selectedId = model.options.firstOrNull { o -> o.current }?.id) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(loading = false, error = LOAD_ERROR) }
            }
        }
    }

    private fun submit() {
        val state = _uiState.value
        val model = state.model ?: return
        val planId = state.selectedId ?: return
        if (state.change == null || state.saving) return
        val name = model.options.first { it.id == planId }.name
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(saving = true, error = null) }
            try {
                gateway.changePlan(model.subscriptionId, planId)
                _uiState.update { it.copy(saving = false, done = "Plan cambiado a $name") }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(saving = false, error = SAVE_ERROR) }
            }
        }
    }

    private companion object {
        const val LOAD_ERROR = "No se pudieron cargar los planes. Revisa tu conexión y vuelve a intentar."
        const val SAVE_ERROR = "No se pudo cambiar el plan. Revisa tu conexión y vuelve a intentar."
    }
}
