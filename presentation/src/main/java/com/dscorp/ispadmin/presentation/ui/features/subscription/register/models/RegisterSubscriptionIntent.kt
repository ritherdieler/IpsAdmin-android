package com.dscorp.ispadmin.presentation.ui.features.subscription.register.models

import com.dscorp.ispadmin.domain.model.EquipmentCondition
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.NetworkDevice
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.PlanResponse
import java.io.File

sealed interface RegisterSubscriptionIntent {
    data class FirstNameChanged(val value: String) : RegisterSubscriptionIntent
    data class LastNameChanged(val value: String) : RegisterSubscriptionIntent
    data class DniChanged(val value: String) : RegisterSubscriptionIntent
    data class AddressChanged(val value: String) : RegisterSubscriptionIntent
    data class PhoneChanged(val value: String) : RegisterSubscriptionIntent
    data class PlanSelected(val value: PlanResponse) : RegisterSubscriptionIntent
    data class PlaceSelected(val value: Place) : RegisterSubscriptionIntent
    data class OnuSelected(val value: Onu) : RegisterSubscriptionIntent
    data class NapBoxSelected(val value: NapBoxResponse) : RegisterSubscriptionIntent
    data class HostDeviceSelected(val device: NetworkDevice) : RegisterSubscriptionIntent
    data object PlaceSelectionCleared : RegisterSubscriptionIntent
    data object NapBoxSelectionCleared : RegisterSubscriptionIntent
    data class InstallationTypeSelected(val type: InstallationType) : RegisterSubscriptionIntent
    data object RefreshOnuList : RegisterSubscriptionIntent
    data class NoteChanged(val value: String) : RegisterSubscriptionIntent
    data class EquipmentConditionChanged(val value: EquipmentCondition) : RegisterSubscriptionIntent
    data class ClientIpAddressChanged(val value: String) : RegisterSubscriptionIntent
    data class OnVlanChanged(val vlan: String) : RegisterSubscriptionIntent
    data class TvCpeKindSelected(val kind: TvCpeKind) : RegisterSubscriptionIntent
    data class WifiSsid24Changed(val value: String) : RegisterSubscriptionIntent
    data class WifiPassword24Changed(val value: String) : RegisterSubscriptionIntent
    data class WifiSsid5Changed(val value: String) : RegisterSubscriptionIntent
    data class WifiPassword5Changed(val value: String) : RegisterSubscriptionIntent
    data class UseDifferentWifiNamesChanged(val enabled: Boolean) : RegisterSubscriptionIntent
    data class RegisterClick(val facadePhotoFile: File? = null) : RegisterSubscriptionIntent
    data class RetryTr069(val subscriptionId: Int) : RegisterSubscriptionIntent
    data object UseCurrentLocationClicked : RegisterSubscriptionIntent
    data object ChooseManualLocationClicked : RegisterSubscriptionIntent
    data object DismissManualLocationMap : RegisterSubscriptionIntent
    data class LocationCoordinatesSelected(
        val latitude: Double,
        val longitude: Double,
    ) : RegisterSubscriptionIntent
    data object WizardContinueClicked : RegisterSubscriptionIntent
    data object WizardBackClicked : RegisterSubscriptionIntent
}
