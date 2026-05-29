package com.dscorp.ispadmin.domain.repository

interface SubscriptionActionsRepository {
    suspend fun reactivateService(subscriptionId: Int, notes: String?)

    suspend fun rebootFiberOnu(subscriptionId: Int)

    suspend fun restoreInternetConnection(subscriptionId: Int, notes: String?)
}
