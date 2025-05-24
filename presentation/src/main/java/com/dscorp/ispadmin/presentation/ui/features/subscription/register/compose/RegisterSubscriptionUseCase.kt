package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose


import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.Subscription

class RegisterSubscriptionUseCase(private val repository: IRepository) {
    suspend operator fun invoke(subscription: Subscription): Result<Subscription> = runCatching {
        repository.registerSubscription(subscription)
    }
}
