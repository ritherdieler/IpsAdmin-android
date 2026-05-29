package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.repository.SubscriptionRegistrationQueryRepository

class GetNapBoxListUseCase(
    private val registrationQueryRepository: SubscriptionRegistrationQueryRepository
) {
    suspend operator fun invoke(): Result<List<NapBoxResponse>> = runCatching {
        registrationQueryRepository.getNapBoxes()
    }
}
