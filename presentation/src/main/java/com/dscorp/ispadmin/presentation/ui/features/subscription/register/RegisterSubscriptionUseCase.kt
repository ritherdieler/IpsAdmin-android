package com.dscorp.ispadmin.presentation.ui.features.subscription.register


import com.dscorp.ispadmin.domain.model.Subscription
import com.example.data2.data.repository.IRepository

class RegisterSubscriptionUseCase(private val repository: IRepository) {
    suspend operator fun invoke(subscription: Subscription): Result<Subscription> = runCatching {
        repository.registerSubscription(subscription)
    }
}
