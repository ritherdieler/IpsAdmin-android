package com.dscorp.ispadmin.data.remote

data class SubscriptionSyncApiResponse(
    val status: Int? = null,
    val error: String? = null,
    val errorCode: String? = null,
    val message: String? = null,
    val data: SubscriptionSyncData? = null
)

data class SubscriptionSyncData(
    val id: Int? = null,
    val alreadyRegistered: Boolean? = null,
    val provisioningPending: Boolean? = null,
    val tr069ProvisionStatus: String? = null,
)

data class RegistrationProgressDto(
    val subscriptionId: Int? = null,
    val step: String? = null,
    val message: String? = null,
    val done: Boolean? = null,
    val mikrotikProvisionStatus: String? = null,
    val oltProvisionStatus: String? = null,
    val tr069ProvisionStatus: String? = null,
    val tr069Message: String? = null,
)
