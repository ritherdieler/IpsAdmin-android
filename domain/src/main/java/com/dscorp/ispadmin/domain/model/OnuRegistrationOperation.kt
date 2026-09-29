package com.dscorp.ispadmin.domain.model

data class OnuRegistrationOperation(
    val id: String,
    val serial: String,
    val subscriptionId: Int?,
    val phase: String,
    val state: String,
    val revision: Long,
    val operatorId: Long? = null,
    val operatorUsername: String? = null,
    val checkpoints: List<OnuRegistrationCheckpoint> = emptyList(),
    val operationFailure: OnuRegistrationFailure? = null,
)

data class OnuRegistrationCheckpoint(
    val stage: String,
    val state: String,
    val attempts: Int,
    val failure: OnuRegistrationFailure? = null,
)

data class OnuRegistrationFailure(
    val code: String,
    val message: String,
    val retryable: Boolean,
    val technicalDetails: String? = null,
)

fun OnuRegistrationOperation.canOpenRegistrationForm(): Boolean =
    phase == "READY_FOR_FORM" && state == "READY_FOR_FORM"

fun OnuRegistrationOperation.canManuallyRetry(): Boolean = when (phase) {
    "OLT_AUTHORIZATION" -> state in setOf("PENDING", "RUNNING", "WAITING", "FAILED")
    "WAITING_FOR_ACS" -> state in setOf("WAITING", "FAILED")
    else -> false
}
