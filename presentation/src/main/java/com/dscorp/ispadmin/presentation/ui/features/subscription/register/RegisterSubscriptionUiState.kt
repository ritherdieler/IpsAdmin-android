package com.dscorp.ispadmin.presentation.ui.features.subscription.register

import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.NetworkDevice
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.domain.model.User

/**
 * Created by Sergio Carrillo Diestra on 13/12/2022.
 * scarrillo.peruapps@gmail.com
 * Peru Apps
 * Huacho, Peru.
 *
 **/
sealed class RegisterSubscriptionUiState {
    class FormDataFound(
        val networkDevices: List<NetworkDevice>,
        val places: List<Place>,
        val technicians: List<User>,
        val napBoxes: List<NapBoxResponse>,
        val hostNetworkDevices: List<NetworkDevice>,
        val unconfirmedOnus: List<Onu>
    ) : RegisterSubscriptionUiState()

    class OnOnuDataFound(val onus: List<Onu>) : RegisterSubscriptionUiState()

    class FiberDevicesFound(val devices: List<NetworkDevice>) : RegisterSubscriptionUiState()

    class WirelessDevicesFound(val devices: List<NetworkDevice>) : RegisterSubscriptionUiState()

    class PlansFound(val plans: List<PlanResponse>) : RegisterSubscriptionUiState()

    class RegisterSubscriptionSuccess(val subscription: Subscription) :
        RegisterSubscriptionUiState()

    class ShimmerVisibility(val showShimmer: Boolean) : RegisterSubscriptionUiState()
    class CouponIsValid(val isValid: Boolean) : RegisterSubscriptionUiState()
    class RefreshingOnus(val isRefreshing: Boolean) : RegisterSubscriptionUiState()
}
