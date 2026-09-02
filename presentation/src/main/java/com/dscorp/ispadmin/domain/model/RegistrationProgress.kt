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
    val subscription: Subscription? = null,
)
