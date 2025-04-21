package com.dscorp.ispadmin.presentation.ui.features.networkdevice.networkdevicelist

import com.dscorp.ispadmin.domain.model.NetworkDeviceResponse

/**
 * Created by Sergio Carrillo Diestra on 19/12/2022.
 * scarrillo.peruapps@gmail.com
 * Peru Apps
 * Huacho, Peru.
 *
 **/
sealed class NetworkDeviceListResponse {
    class OnNetworkDeviceListFound(val networkDeviceList: List<NetworkDeviceResponse>) :
        NetworkDeviceListResponse()

    class OnError(val error: Exception) : NetworkDeviceListResponse()
}
