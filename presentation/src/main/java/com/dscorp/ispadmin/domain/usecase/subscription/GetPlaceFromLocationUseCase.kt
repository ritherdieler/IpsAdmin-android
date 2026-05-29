package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.repository.SubscriptionRegistrationQueryRepository

class GetPlaceFromLocationUseCase(
    val registrationQueryRepository: SubscriptionRegistrationQueryRepository
) {

    suspend operator fun invoke(latitude: Double, longitude: Double): Result<Place> =
        runCatching {
            registrationQueryRepository.getPlaceFromLocation(latitude, longitude)
        }
}
