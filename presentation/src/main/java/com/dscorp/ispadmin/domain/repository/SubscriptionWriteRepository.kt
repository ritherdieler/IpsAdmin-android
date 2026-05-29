package com.dscorp.ispadmin.domain.repository

import com.dscorp.ispadmin.domain.model.Subscription
import java.io.File

interface SubscriptionWriteRepository {
    suspend fun registerSubscription(subscription: Subscription): Subscription

    suspend fun registerSubscriptionWithFacadePhoto(
        subscription: Subscription,
        facadePhotoFile: File
    ): Subscription
}
