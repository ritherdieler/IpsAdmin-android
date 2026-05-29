package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.repository.SubscriptionRegistrationQueryRepository

class GetPlaceListUseCase(
    private val registrationQueryRepository: SubscriptionRegistrationQueryRepository
) {
    suspend operator fun invoke(): Result<List<Place>> = runCatching {
        registrationQueryRepository.getPlaces()
    }
}
