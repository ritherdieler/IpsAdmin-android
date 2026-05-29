package com.dscorp.ispadmin.domain.usecase.service

import com.dscorp.ispadmin.domain.repository.SubscriptionActionsRepository

class RestoreInternetConnectionUseCase(
    private val subscriptionActionsRepository: SubscriptionActionsRepository
) {
    suspend operator fun invoke(
        subscriptionId: Int,
        notes: String? = null
    ): Result<Unit> = runCatching {
        subscriptionActionsRepository.restoreInternetConnection(subscriptionId, notes)
    }
}
