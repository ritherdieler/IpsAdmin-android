package com.dscorp.ispadmin.presentation.ui.features.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.data.datasource.remote.PlanApi
import com.dscorp.ispadmin.data.datasource.remote.PlanCountResponse
import com.dscorp.ispadmin.data.datasource.remote.PlanSaveRequest
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.planLabel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response

interface PlanCatalogGateway {
    suspend fun plans(): List<PlanCountResponse>
    suspend fun update(plan: PlanSaveRequest)
    suspend fun create(plan: PlanSaveRequest)
    suspend fun setActive(planId: Int, active: Boolean)
}

class ApiPlanCatalogGateway(private val api: PlanApi) : PlanCatalogGateway {
    override suspend fun plans() = api.allWithCounts()
    override suspend fun update(plan: PlanSaveRequest) = api.update(plan).orThrow()
    override suspend fun create(plan: PlanSaveRequest) = api.create(plan).orThrow()
    override suspend fun setActive(planId: Int, active: Boolean) =
        (if (active) api.activate(planId) else api.deactivate(planId)).orThrow()

    private fun Response<Unit>.orThrow() {
        if (!isSuccessful) throw HttpException(this)
    }
}

data class PlanCatalogUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val catalog: PlanCatalog? = null,
    val retiredTab: Boolean = false,
    val editor: PlanEditor? = null,
    val saving: Boolean = false,
    val editorError: String? = null,
    val message: String? = null,
)

sealed interface PlanCatalogIntent {
    data object Load : PlanCatalogIntent
    data class TabSelected(val retired: Boolean) : PlanCatalogIntent
    data class Edit(val planId: Int) : PlanCatalogIntent
    data object New : PlanCatalogIntent
    data class EditorChanged(val change: (PlanEditor) -> PlanEditor) : PlanCatalogIntent
    data object CloseEditor : PlanCatalogIntent
    data object Save : PlanCatalogIntent
    data object Retire : PlanCatalogIntent
    data object MessageShown : PlanCatalogIntent
}

/** a-mas-03/04: plan catalog with its editor. */
class PlanCatalogViewModel(private val gateway: PlanCatalogGateway, private val mainImmediate: CoroutineDispatcher) : ViewModel() {
    private val _uiState = MutableStateFlow(PlanCatalogUiState())
    val uiState: StateFlow<PlanCatalogUiState> = _uiState

    fun onIntent(intent: PlanCatalogIntent) {
        when (intent) {
            PlanCatalogIntent.Load -> load()
            is PlanCatalogIntent.TabSelected -> _uiState.update { it.copy(retiredTab = intent.retired) }
            is PlanCatalogIntent.Edit -> _uiState.update { state ->
                val plan = state.catalog?.plan(intent.planId) ?: return@update state
                state.copy(editor = PlanEditor.edit(plan), editorError = null)
            }
            PlanCatalogIntent.New -> _uiState.update { it.copy(editor = PlanEditor.new(), editorError = null) }
            is PlanCatalogIntent.EditorChanged -> _uiState.update { state ->
                state.copy(editor = state.editor?.let(intent.change), editorError = null)
            }
            PlanCatalogIntent.CloseEditor -> _uiState.update { if (it.saving) it else it.copy(editor = null) }
            PlanCatalogIntent.Save -> save()
            PlanCatalogIntent.Retire -> _uiState.value.editor?.let { editor ->
                _uiState.update { it.copy(editor = editor.copy(available = false)) }
                save()
            }
            PlanCatalogIntent.MessageShown -> _uiState.update { it.copy(message = null) }
        }
    }

    private fun load() {
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val catalog = PlanCatalog.from(gateway.plans())
                _uiState.update { it.copy(loading = false, catalog = catalog) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(loading = false, error = LOAD_ERROR) }
            }
        }
    }

    private fun save() {
        val state = _uiState.value
        val editor = state.editor ?: return
        if (state.saving || editor.blockingReason != null || !editor.changed) return
        viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(saving = true, editorError = null) }
            try {
                when {
                    editor.isNew -> gateway.create(editor.toRequest())
                    else -> {
                        if (editor.commercialChanged) gateway.update(editor.toRequest())
                        if (editor.availabilityChanged) gateway.setActive(editor.id!!, editor.available)
                    }
                }
                val name = planLabel(editor.name.trim())
                val message = when {
                    editor.isNew -> "Plan $name creado"
                    editor.availabilityChanged && !editor.available && !editor.commercialChanged -> "Plan $name retirado"
                    else -> "Plan $name guardado"
                }
                _uiState.update { it.copy(saving = false, editor = null, message = message) }
                load()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(saving = false, editorError = SAVE_ERROR) }
            }
        }
    }

    private companion object {
        const val LOAD_ERROR = "No se pudieron cargar los planes. Revisa tu conexión y vuelve a intentar."
        const val SAVE_ERROR = "No se pudo guardar el plan. Revisa tu conexión y vuelve a intentar."
    }
}
