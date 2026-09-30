package com.dscorp.ispadmin.domain.model

data class RegistrationProgress(
    val subscriptionId: Int,
    val step: String,
    val message: String,
    val done: Boolean,
    val mikrotikProvisionStatus: String? = null,
    val oltProvisionStatus: String? = null,
    val tr069ProvisionStatus: String? = null,
    val tr069Message: String? = null,
    val provisioningCheckpoints: List<RegistrationProgressCheckpoint> = emptyList(),
    val subscription: Subscription? = null,
)

data class RegistrationProgressCheckpoint(
    val stage: String,
    val state: String,
    val attempts: Int,
    val failure: RegistrationProgressFailure? = null,
)

data class RegistrationProgressFailure(
    val code: String,
    val message: String,
    val retryable: Boolean = true,
    val technicalDetails: String? = null,
)
