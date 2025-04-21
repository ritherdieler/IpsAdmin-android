package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import com.dscorp.ispadmin.domain.model.NetworkDevice
import com.example.data2.data.repository.IRepository

class GetCoreDevicesUseCase(private val repository: IRepository) {
    suspend operator fun invoke(): Result<List<NetworkDevice>> = runCatching {
        repository.getCoreDevices()
    }
}
