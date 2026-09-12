package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.accessmigration

import com.dscorp.ispadmin.domain.model.AccessMigrationStage
import com.dscorp.ispadmin.domain.model.InstallationType

data class AccessMigrationUiState(
    val subscriptionId: Int? = null,
    val accessLabel: String = IP_LABEL,
    val accessValue: String? = null,
    val canMigrate: Boolean = false,
    val isMigrating: Boolean = false,
    val stage: AccessMigrationStage? = null,
    val stageMessage: String? = null,
    val failureReason: String? = null,
    val quarantineUntil: String? = null,
    val errorMessage: String? = null,
) {
    companion object {
        const val IP_LABEL = "Dirección IP"
        const val PPPOE_LABEL = "Usuario PPPoE"
        const val IP_UNASSIGNED = "No asignada"
        const val PPPOE_UNASSIGNED = "No asignado"
    }
}

sealed interface AccessMigrationIntent {
    data class Bind(
        val subscriptionId: Int,
        val accessMode: String? = null,
        val ip: String? = null,
        val pppoeUsername: String? = null,
        val migrationStage: String? = null,
        val installationType: InstallationType? = null,
    ) : AccessMigrationIntent

    data object StartMigration : AccessMigrationIntent

    data object ClearError : AccessMigrationIntent
}
