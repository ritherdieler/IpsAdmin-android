package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.repository.SubscriptionRegistrationQueryRepository

class GetAvailableOnuListUseCase(
    private val registrationQueryRepository: SubscriptionRegistrationQueryRepository
) {

    suspend operator fun invoke(): Result<List<Onu>> = runCatching {
        registrationQueryRepository.getUnconfirmedOnus()
    }
}
