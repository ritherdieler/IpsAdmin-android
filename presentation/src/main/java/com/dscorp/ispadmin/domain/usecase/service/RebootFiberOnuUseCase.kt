package com.dscorp.ispadmin.domain.usecase.service

import com.dscorp.ispadmin.domain.repository.SubscriptionActionsRepository

class RebootFiberOnuUseCase(
    private val subscriptionActionsRepository: SubscriptionActionsRepository
) {
    suspend operator fun invoke(subscriptionId: Int): Result<Unit> = runCatching {
        subscriptionActionsRepository.rebootFiberOnu(subscriptionId)
    }
}
