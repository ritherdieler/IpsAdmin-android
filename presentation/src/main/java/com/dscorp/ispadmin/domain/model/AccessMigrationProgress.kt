package com.dscorp.ispadmin.domain.model

data class AccessMigrationProgress(
    val subscriptionId: Int,
    val stage: AccessMigrationStage? = null,
    val failureReason: String? = null,
    val quarantineUntil: String? = null,
    val pppoeUsername: String? = null,
    val attempt: Int? = null,
    val done: Boolean = false,
    val message: String? = null,
)
