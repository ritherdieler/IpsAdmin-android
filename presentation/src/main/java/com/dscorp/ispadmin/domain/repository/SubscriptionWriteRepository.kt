package com.dscorp.ispadmin.domain.repository

import com.dscorp.ispadmin.domain.model.Subscription

interface SubscriptionWriteRepository {
    suspend fun registerSubscription(subscription: Subscription): Subscription
}
