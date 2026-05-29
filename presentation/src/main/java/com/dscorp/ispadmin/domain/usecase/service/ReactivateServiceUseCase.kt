package com.dscorp.ispadmin.domain.usecase.service

import com.dscorp.ispadmin.domain.repository.SubscriptionActionsRepository

class ReactivateServiceUseCase(
    private val subscriptionActionsRepository: SubscriptionActionsRepository
) {
    suspend operator fun invoke(
        subscriptionId: Int,
        notes: String? = null
    ): Result<Unit> = runCatching {
        subscriptionActionsRepository.reactivateService(subscriptionId, notes)
    }
}
