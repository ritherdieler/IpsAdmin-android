package com.dscorp.ispadmin.domain.model

data class OnuRegistrationOutcome(
    val operationId: String,
    val subscriptionId: Int?,
    val phase: String,
    val state: String,
    val outcome: String,
)
