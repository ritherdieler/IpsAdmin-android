package com.dscorp.ispadmin.domain.usecase.service

import com.dscorp.ispadmin.domain.repository.SubscriptionActionsRepository

class DeferCutUseCase(
    private val subscriptionActionsRepository: SubscriptionActionsRepository
) {
    suspend operator fun invoke(subscriptionId: Int, untilDate: String, reason: String): Result<Unit> = runCatching {
        subscriptionActionsRepository.deferCut(subscriptionId, untilDate, reason)
    }
}
