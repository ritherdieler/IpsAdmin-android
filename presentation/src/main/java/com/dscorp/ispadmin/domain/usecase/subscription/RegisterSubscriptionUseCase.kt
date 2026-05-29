package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.data.repository.InstallationOrderRepository
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.domain.repository.SubscriptionWriteRepository
import java.io.File

class RegisterSubscriptionUseCase(
    private val subscriptionWriteRepository: SubscriptionWriteRepository,
    private val installationOrderRepository: InstallationOrderRepository
) {
    suspend operator fun invoke(
        subscription: Subscription,
        orderId: Int?,
        facadePhotoFile: File? = null
    ): Result<Subscription> = runCatching {
        orderId?.let { installationOrderRepository.closeInstallationOrder(it) }
        if (facadePhotoFile != null) {
            subscriptionWriteRepository.registerSubscriptionWithFacadePhoto(
                subscription = subscription,
                facadePhotoFile = facadePhotoFile
            )
        } else {
            subscriptionWriteRepository.registerSubscription(subscription)
        }
    }
}
