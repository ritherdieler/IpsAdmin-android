package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.repository.SubscriptionRegistrationQueryRepository

class GetNearNapBoxesUseCase(
    val registrationQueryRepository: SubscriptionRegistrationQueryRepository
) {

    suspend operator fun invoke(latitude: Double, longitude: Double): Result<List<NapBoxResponse>> =
        runCatching {
            registrationQueryRepository.getNapBoxesOrderedByLocation(latitude, longitude)
        }
}
