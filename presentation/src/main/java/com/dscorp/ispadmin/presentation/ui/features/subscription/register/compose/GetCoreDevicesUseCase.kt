package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.NetworkDevice

class GetCoreDevicesUseCase(private val repository: IRepository) {
    suspend operator fun invoke(): Result<List<NetworkDevice>> = runCatching {
        repository.getCoreDevices()
    }
}
