package com.dscorp.ispadmin.presentation.ui.features.ippool.register

import com.dscorp.ispadmin.domain.model.IpPool
import com.dscorp.ispadmin.domain.model.NetworkDevice

sealed class IpPoolUiState(val error: String? = null) {

    class IpPoolRegister(val ipPool: IpPool) : IpPoolUiState()
    class FormDataReady(val hostDevices: List<NetworkDevice>, val ipPoolList: List<IpPool>) :
        IpPoolUiState()
}
