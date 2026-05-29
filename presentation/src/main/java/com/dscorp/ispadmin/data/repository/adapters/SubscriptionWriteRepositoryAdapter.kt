package com.dscorp.ispadmin.data.repository.adapters

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.domain.repository.SubscriptionWriteRepository

class SubscriptionWriteRepositoryAdapter(
    private val repository: IRepository
) : SubscriptionWriteRepository {

    override suspend fun registerSubscription(subscription: Subscription): Subscription =
        repository.registerSubscription(subscription)
}
