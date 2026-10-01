package com.dscorp.ispadmin.domain.model

data class OnuRegistrationCancellationIntent(
    val operatorId: Long,
    val requestKey: String,
    val serial: String,
    val operationId: String? = null,
)
