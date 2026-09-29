package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.domain.connectivity.MikrotikReachabilityMonitor
import com.dscorp.ispadmin.domain.connectivity.NetworkConnectivityMonitor
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.domain.model.subscription.subscriptionClientIpAddressError
import com.dscorp.ispadmin.domain.repository.SubscriptionWriteRepository
import com.google.gson.Gson
import java.io.File
import java.util.UUID

class RegisterSubscriptionUseCase(
    private val subscriptionWriteRepository: SubscriptionWriteRepository,
    private val enqueuePendingSubscriptionUseCase: EnqueuePendingSubscriptionUseCase,
    private val connectivityMonitor: NetworkConnectivityMonitor,
    private val mikrotikReachabilityMonitor: MikrotikReachabilityMonitor,
    private val uuidGenerator: () -> String = { UUID.randomUUID().toString() },
    private val gson: Gson = Gson()
) {
    suspend operator fun invoke(
        subscription: Subscription,
        orderId: Int?,
        facadePhotoFile: File? = null
    ): Result<RegisterSubscriptionResult> = runCatching {
        if (!subscription.registrationOperationId.isNullOrBlank()) {
            registerOnline(subscription, orderId, null)
        } else if (canRegisterOnline()) {
            registerOnline(subscription, orderId, facadePhotoFile)
        } else {
            enqueueOffline(subscription, orderId, facadePhotoFile)
        }
    }

    private suspend fun canRegisterOnline(): Boolean {
        if (!connectivityMonitor.isConnected()) return false
        return mikrotikReachabilityMonitor.isMikrotikReachable()
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

    private suspend fun enqueueOffline(
        subscription: Subscription,
        orderId: Int?,
        facadePhotoFile: File?
    ): RegisterSubscriptionResult.QueuedOffline {
        val photo = facadePhotoFile
            ?: error("La foto de fachada es obligatoria para guardar la suscripción en modo offline")
        val clientIp = subscription.clientIpAddress?.trim().orEmpty()
        if (clientIp.isEmpty()) {
            error("La IP del cliente es obligatoria en modo offline")
        }
        val clientIpError = subscriptionClientIpAddressError(clientIp, required = true)
        if (clientIpError != null) {
            error(clientIpError)
        }
        val payload = subscription.copy(
            installationOrderId = orderId,
            ip = clientIp,
            clientIpAddress = clientIp
        )
        val pending = enqueuePendingSubscriptionUseCase(
            subscriptionJson = gson.toJson(payload),
            facadePhotoFile = photo,
            installationOrderId = orderId
        ).getOrThrow()
        return RegisterSubscriptionResult.QueuedOffline(pending)
    }
}
