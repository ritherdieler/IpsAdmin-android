package com.dscorp.ispadmin.domain.repository

import com.dscorp.ispadmin.domain.model.OnuRegistrationOperation
import com.dscorp.ispadmin.domain.model.OnuRegistrationCleanupReport
import java.io.File

data class OnuRegistrationTarget(
    val oltId: String,
    val ponType: String,
    val board: String,
    val port: String,
    val onuType: String,
    val vlan: Int,
    val zone: String = "Zone 1",
    val onuMode: String = "Routing",
    val customProfile: String = "Generic_1",
)

data class StartOnuRegistrationRequest(
    val requestKey: String,
    val serial: String,
    val target: OnuRegistrationTarget,
)

interface OnuRegistrationOperationRepository {
    suspend fun active(): OnuRegistrationOperation?
    suspend fun start(request: StartOnuRegistrationRequest): OnuRegistrationOperation
    suspend fun draft(operationId: String): Map<String, Any?>?
    suspend fun saveDraft(operationId: String, draft: Map<String, Any?>)
    suspend fun uploadPhoto(operationId: String, file: File): String
    suspend fun retry(operationId: String, expectedRevision: Long): OnuRegistrationOperation
    suspend fun cancel(operationId: String, expectedRevision: Long): OnuRegistrationOperation
    suspend fun cleanupCancelled(operationId: String): OnuRegistrationCleanupReport
    suspend fun get(operationId: String): OnuRegistrationOperation
}
