package com.dscorp.ispadmin.domain.repository

interface SubscriptionActionsRepository {
    suspend fun reactivateService(subscriptionId: Int, notes: String?)

    suspend fun rebootFiberOnu(subscriptionId: Int)

    suspend fun retryTr069Provisioning(subscriptionId: Int): com.dscorp.ispadmin.domain.model.Subscription

    suspend fun getRegistrationProgress(subscriptionId: Int): com.dscorp.ispadmin.domain.model.RegistrationProgress

    suspend fun restoreInternetConnection(subscriptionId: Int, notes: String?)
}
