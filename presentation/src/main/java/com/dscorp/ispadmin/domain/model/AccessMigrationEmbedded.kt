package com.dscorp.ispadmin.domain.model

data class AccessMigrationEmbedded(
    val subscriptionId: Int? = null,
    val stage: String? = null,
    val failureReason: String? = null,
    val quarantineUntil: String? = null,
    val pppoeUsername: String? = null,
    val attempt: Int? = null,
    val done: Boolean? = null,
    val message: String? = null,
)
