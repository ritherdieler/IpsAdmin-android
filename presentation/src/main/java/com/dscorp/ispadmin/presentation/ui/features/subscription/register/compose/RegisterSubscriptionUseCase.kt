package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose


import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.data.repository.InstallationOrderRepository
import com.dscorp.ispadmin.domain.model.Subscription

class RegisterSubscriptionUseCase(
    private val repository: IRepository,
    private val installationOrderRepository: InstallationOrderRepository
) {
    suspend operator fun invoke(subscription: Subscription, orderId: Int?): Result<Subscription> =
        runCatching {
            orderId?.let { installationOrderRepository.closeInstallationOrder(it) }
            repository.registerSubscription(subscription)
        }
}
