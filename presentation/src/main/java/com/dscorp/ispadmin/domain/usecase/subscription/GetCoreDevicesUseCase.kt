package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.domain.model.NetworkDevice
import com.dscorp.ispadmin.domain.repository.SubscriptionRegistrationQueryRepository

class GetCoreDevicesUseCase(
    private val registrationQueryRepository: SubscriptionRegistrationQueryRepository
) {
    suspend operator fun invoke(): Result<List<NetworkDevice>> = runCatching {
        registrationQueryRepository.getCoreDevices()
    }
}
