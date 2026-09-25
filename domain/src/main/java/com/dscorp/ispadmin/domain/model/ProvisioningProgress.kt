package com.dscorp.ispadmin.domain.model

data class ProvisioningProgress(
    val operation: ProvisioningOperation,
    val canRetry: Boolean = false,
    val canCancel: Boolean = false,
    val canRetryCancellation: Boolean = false,
    val canStartAgain: Boolean = false,
)

data class ProvisioningOperation(
    val id: String,
    val subscriptionId: Int,
    val revision: Long,
    val state: String,
    val checkpoints: List<ProvisioningCheckpoint> = emptyList(),
)

data class ProvisioningCheckpoint(
    val stage: String,
    val state: String,
    val attempts: Int,
    val failure: ProvisioningFailure? = null,
)

data class ProvisioningFailure(val code: String, val message: String, val retryable: Boolean)

data class ProvisioningAction(val subscriptionId: Int, val operationId: String, val expectedRevision: Long)
