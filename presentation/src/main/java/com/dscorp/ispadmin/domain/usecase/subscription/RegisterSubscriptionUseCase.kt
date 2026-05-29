package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.data.repository.InstallationOrderRepository
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.domain.repository.SubscriptionWriteRepository

class RegisterSubscriptionUseCase(
    private val subscriptionWriteRepository: SubscriptionWriteRepository,
    private val installationOrderRepository: InstallationOrderRepository
) {
    suspend operator fun invoke(subscription: Subscription, orderId: Int?): Result<Subscription> =
        runCatching {
            orderId?.let { installationOrderRepository.closeInstallationOrder(it) }
            subscriptionWriteRepository.registerSubscription(subscription)
        }
}
