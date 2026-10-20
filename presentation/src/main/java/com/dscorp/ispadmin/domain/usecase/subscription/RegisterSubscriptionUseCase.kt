package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.domain.repository.SubscriptionWriteRepository
import kotlinx.coroutines.CancellationException
import java.io.File
import java.util.UUID

class RegisterSubscriptionUseCase(
    private val subscriptionWriteRepository: SubscriptionWriteRepository,
    private val uuidGenerator: () -> String = { UUID.randomUUID().toString() }
) {
    suspend operator fun invoke(
        subscription: Subscription,
        orderId: Int?,
        facadePhotoFile: File? = null
    ): Result<RegisterSubscriptionResult> = try {
        // A preauthorized registration already carries its photo URL, so no file is uploaded.
        val photo = facadePhotoFile.takeIf { subscription.registrationOperationId.isNullOrBlank() }
        Result.success(registerOnline(subscription, orderId, photo))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    private suspend fun registerOnline(
        subscription: Subscription,
        orderId: Int?,
        facadePhotoFile: File?
    ): RegisterSubscriptionResult.Registered {
        val payload = subscription.copy(
            clientRequestId = subscription.registrationOperationId?.takeIf(String::isNotBlank)
                ?: subscription.clientRequestId?.takeIf(String::isNotBlank)
                ?: uuidGenerator(),
            installationOrderId = orderId
        )
        val registered = if (facadePhotoFile != null) {
            subscriptionWriteRepository.registerSubscriptionWithFacadePhoto(
                subscription = payload,
                facadePhotoFile = facadePhotoFile
            )
        } else {
            subscriptionWriteRepository.registerSubscription(payload)
        }
        return RegisterSubscriptionResult.Registered(registered)
    }
}
