package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.accessmigration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.domain.model.AccessMigrationProgress
import com.dscorp.ispadmin.domain.model.AccessMigrationStage
import com.dscorp.ispadmin.domain.model.AccessMode
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.repository.AccessMigrationRepository
import com.dscorp.ispadmin.domain.usecase.subscription.PollAccessMigrationUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccessMigrationViewModel(
    private val repository: AccessMigrationRepository,
    private val pollAccessMigrationUseCase: PollAccessMigrationUseCase,
    private val mainImmediate: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccessMigrationUiState())
    val uiState: StateFlow<AccessMigrationUiState> = _uiState.asStateFlow()

    private var migrateJob: Job? = null

    fun onIntent(intent: AccessMigrationIntent) {
        when (intent) {
            is AccessMigrationIntent.Bind -> bind(intent)
            AccessMigrationIntent.StartMigration -> startMigration()
            AccessMigrationIntent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun bind(intent: AccessMigrationIntent.Bind) {
        val stage = AccessMigrationStage.parse(intent.migrationStage)
        val (label, value) = resolveNetworkAccess(
            accessMode = intent.accessMode,
            ip = intent.ip,
            pppoeUsername = intent.pppoeUsername,
        )
        _uiState.value = AccessMigrationUiState(
            subscriptionId = intent.subscriptionId,
            accessLabel = label,
            accessValue = value,
            canMigrate = canMigrateAccess(
                accessMode = intent.accessMode,
                installationType = intent.installationType,
                stage = stage,
            ),
            stage = stage,
            stageMessage = stage?.toUserMessage(),
        )
    }

    private fun startMigration() {
        if (migrateJob?.isActive == true) return
        val subscriptionId = _uiState.value.subscriptionId ?: return
        migrateJob = viewModelScope.launch(mainImmediate) {
            _uiState.update { it.copy(isMigrating = true, errorMessage = null) }
            val started = runCatching { repository.startMigration(subscriptionId) }
            val initial = started.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isMigrating = false,
                        errorMessage = error.message ?: "No se pudo iniciar la migración a PPPoE",
                    )
                }
                return@launch
            }
            applyProgress(initial, keepMigrating = true)
            if (initial.done || initial.stage?.isTerminal() == true) {
                return@launch
            }
            val polled = pollAccessMigrationUseCase(subscriptionId) { progress ->
                applyProgress(progress, keepMigrating = true)
            }
            polled.fold(
                onSuccess = { progress -> applyProgress(progress, keepMigrating = false) },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isMigrating = false,
                            errorMessage = error.message ?: "No se pudo completar la migración a PPPoE",
                        )
                    }
                },
            )
        }
    }

    private fun applyProgress(progress: AccessMigrationProgress, keepMigrating: Boolean) {
        val username = progress.pppoeUsername?.trim()?.takeIf { it.isNotEmpty() }
        val terminal = progress.done || progress.stage?.isTerminal() == true
        val switchedToPppoe = progress.stage == AccessMigrationStage.DONE || username != null
        val error = when (progress.stage) {
            AccessMigrationStage.FAILED_STRANDED ->
                progress.failureReason?.takeIf { it.isNotBlank() }
                    ?: STRANDED_FALLBACK
            AccessMigrationStage.FAILED_REVERTED ->
                progress.failureReason?.takeIf { it.isNotBlank() }
                    ?: REVERTED_FALLBACK
            else -> null
        }
        _uiState.update { current ->
            current.copy(
                isMigrating = keepMigrating && !terminal,
                stage = progress.stage ?: current.stage,
                stageMessage = progress.message?.takeIf { it.isNotBlank() }
                    ?: progress.stage?.toUserMessage()
                    ?: current.stageMessage,
                failureReason = progress.failureReason,
                quarantineUntil = progress.quarantineUntil,
                errorMessage = error,
                canMigrate = canMigrateAccess(
                    accessMode = if (switchedToPppoe) AccessMode.PPPOE_DYNAMIC.name else AccessMode.STATIC_IP.name,
                    installationType = InstallationType.FIBER,
                    stage = progress.stage ?: current.stage,
                ),
                accessLabel = if (switchedToPppoe) {
                    AccessMigrationUiState.PPPOE_LABEL
                } else {
                    current.accessLabel
                },
                accessValue = username ?: current.accessValue,
            )
        }
    }

    companion object {
        const val STRANDED_FALLBACK = "La migración falló y el CPE no responde"
        const val REVERTED_FALLBACK = "La migración falló y se revirtió"
    }
}

internal fun resolveNetworkAccess(
    accessMode: String?,
    ip: String?,
    pppoeUsername: String?,
): Pair<String, String?> {
    val mode = AccessMode.parse(accessMode)
    if (mode?.usesPppoe() == true) {
        val user = pppoeUsername?.trim()?.takeIf { it.isNotEmpty() }
            ?: AccessMigrationUiState.PPPOE_UNASSIGNED
        return AccessMigrationUiState.PPPOE_LABEL to user
    }
    return AccessMigrationUiState.IP_LABEL to ip?.trim()?.takeIf { it.isNotEmpty() }
}

internal fun canMigrateAccess(
    accessMode: String?,
    installationType: InstallationType?,
    stage: AccessMigrationStage?,
): Boolean {
    if (installationType != InstallationType.FIBER) return false
    if (AccessMode.parse(accessMode)?.usesPppoe() == true) return false
    return stage == null ||
        stage == AccessMigrationStage.ELIGIBLE ||
        stage == AccessMigrationStage.FAILED_REVERTED
}

internal fun AccessMigrationStage.toUserMessage(): String = when (this) {
    AccessMigrationStage.ELIGIBLE -> "Elegible para migrar a PPPoE"
    AccessMigrationStage.OLT_READY -> "Service-port VLAN 100 verificado"
    AccessMigrationStage.SECRET_READY -> "Secret PPPoE creado"
    AccessMigrationStage.CPE_APPLIED -> "WAN PPPoE aplicada en el CPE"
    AccessMigrationStage.VERIFIED -> "Sesión PPPoE verificada"
    AccessMigrationStage.QUEUE_CLEARED -> "Cola por IP eliminada"
    AccessMigrationStage.QUARANTINE -> "En cuarentena"
    AccessMigrationStage.DONE -> "Migración completada"
    AccessMigrationStage.FAILED_REVERTED -> "Falló y se revirtió"
    AccessMigrationStage.FAILED_STRANDED -> "Falló y el CPE no responde"
}
