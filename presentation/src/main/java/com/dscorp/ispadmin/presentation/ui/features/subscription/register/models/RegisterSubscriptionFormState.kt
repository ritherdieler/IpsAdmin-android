package com.dscorp.ispadmin.presentation.ui.features.subscription.register.models

import android.net.Uri
import com.dscorp.ispadmin.domain.model.EquipmentCondition
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.NetworkDevice
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.subscription.subscriptionAddressError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionClientIpAddressError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionDniError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionFacadePhotoError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionFirstNameError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionHostDeviceError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionLastNameError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionLocationError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionNapBoxError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionNoteError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionOnuError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionPhoneError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionPlaceError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionPlanError
import com.dscorp.ispadmin.domain.model.subscription.derivedWifiSsid5
import com.dscorp.ispadmin.domain.model.subscription.subscriptionWifiPasswordError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionWifiSsid24Error
import com.dscorp.ispadmin.domain.model.subscription.subscriptionWifiSsidError
import com.google.android.gms.maps.model.LatLng

data class RegisterSubscriptionFormState(
    val firstName: String = "",
    val firstNameError: String? = null,
    val lastName: String = "",
    val lastNameError: String? = null,
    val dni: String = "",
    val dniError: String? = null,
    val address: String = "",
    val addressError: String? = null,
    val phone: String = "",
    val phoneError: String? = null,
    val price: String = "",
    val priceError: String? = null,
    val subscriptionDate: Long = 0,
    val planList: List<PlanResponse> = emptyList(),
    val selectedPlan: PlanResponse? = null,
    val planError: String? = null,
    val placeList: List<Place> = emptyList(),
    val selectedPlace: Place? = null,
    val placeError: String? = null,
    val coreDeviceList: List<NetworkDevice> = emptyList(),
    val selectedHostDevice: NetworkDevice? = null,
    val hostDeviceError: String? = null,
    val location: LatLng? = null,
    val locationError: String? = null,
    val locationCaptureMethod: LocationCaptureMethod = LocationCaptureMethod.NOT_SELECTED,
    val cpeDevice: NetworkDevice? = null,
    val napBoxError: String? = null,
    val napBoxList: List<NapBoxResponse> = emptyList(),
    val selectedNapBox: NapBoxResponse? = null,
    val onuList: List<Onu> = emptyList(),
    val selectedOnu: Onu? = null,
    val onuError: String? = null,
    val coupon: String = "",
    val note: String = "",
    val noteError: String? = null,
    val facadePhotoUri: Uri? = null,
    val facadePhotoError: String? = null,
    val installationType: InstallationType = InstallationType.FIBER,
    val equipmentCondition: EquipmentCondition = EquipmentCondition.LOAN,
    val clientIpAddress: String = "",
    val clientIpAddressError: String? = null,
    val requiresClientIpAddress: Boolean = false,
    val vlan: String = DEFAULT_REGISTRATION_VLAN,
    val wifiSsid24: String = "",
    val wifiSsid24Error: String? = null,
    val wifiPassword24: String = "",
    val wifiPassword24Error: String? = null,
    val wifiSsid5: String = "",
    val wifiSsid5Error: String? = null,
    val wifiPassword5: String = "",
    val wifiPassword5Error: String? = null,
    val useDifferentWifiNames: Boolean = false,
    val tvCpeKind: TvCpeKind? = null,
    val tvCpeKindError: String? = null,
) {
    fun requiresNapBox(): Boolean {
        return installationType == InstallationType.FIBER ||
            installationType == InstallationType.ONLY_TV_FIBER
    }

    fun requiresTvCpeKind(): Boolean {
        return installationType == InstallationType.ONLY_TV_FIBER
    }

    fun requiresOnu(): Boolean {
        return installationType == InstallationType.FIBER ||
            (installationType == InstallationType.ONLY_TV_FIBER && tvCpeKind == TvCpeKind.ONU)
    }

    fun requiresWifiConfig(): Boolean {
        return installationType == InstallationType.FIBER
    }

    fun resolvedWifiSsid5(): String =
        if (useDifferentWifiNames) wifiSsid5.trim() else derivedWifiSsid5(wifiSsid24)

    fun activeCoreDevices(): List<NetworkDevice> = coreDeviceList.filter { !it.disabled }

    fun shouldShowHostDeviceSelector(): Boolean = activeCoreDevices().size > 1

    fun validate(field: FormFieldKey): String? {
        return when (field) {
            FormFieldKey.FIRST_NAME -> subscriptionFirstNameError(firstName)
            FormFieldKey.LAST_NAME -> subscriptionLastNameError(lastName)
            FormFieldKey.DNI -> subscriptionDniError(dni)
            FormFieldKey.ADDRESS -> subscriptionAddressError(address)
            FormFieldKey.PHONE -> subscriptionPhoneError(phone)
            FormFieldKey.PLAN -> subscriptionPlanError(selectedPlan, planList)
            FormFieldKey.PLACE -> subscriptionPlaceError(selectedPlace)
            FormFieldKey.ONU -> subscriptionOnuError(requiresOnu(), selectedOnu, onuList)
            FormFieldKey.NAP_BOX -> subscriptionNapBoxError(
                requiresNapBox(),
                selectedNapBox,
                napBoxList
            )
            FormFieldKey.FACADE_PHOTO -> subscriptionFacadePhotoError(facadePhotoUri != null)
            FormFieldKey.NOTE -> subscriptionNoteError(note)
            FormFieldKey.HOST_DEVICE -> subscriptionHostDeviceError(
                selectedHostDevice,
                coreDeviceList
            )
            FormFieldKey.EQUIPMENT_CONDITION -> null
            FormFieldKey.CLIENT_IP_ADDRESS -> subscriptionClientIpAddressError(
                clientIpAddress,
                requiresClientIpAddress
            )
            FormFieldKey.WIFI_SSID_24 -> subscriptionWifiSsid24Error(
                wifiSsid24,
                requiresWifiConfig(),
                useDifferentWifiNames
            )
            FormFieldKey.WIFI_PASSWORD_24 ->
                subscriptionWifiPasswordError(wifiPassword24, requiresWifiConfig())
            FormFieldKey.WIFI_SSID_5 -> subscriptionWifiSsidError(
                wifiSsid5,
                requiresWifiConfig() && useDifferentWifiNames
            )
            FormFieldKey.WIFI_PASSWORD_5 -> null
            FormFieldKey.LOCATION ->
                subscriptionLocationError(location?.latitude, location?.longitude)
            FormFieldKey.TV_CPE_KIND ->
                if (requiresTvCpeKind() && tvCpeKind == null) {
                    "Seleccione ONU o receptor óptico CATV"
                } else {
                    null
                }
        }
    }

    fun blockingFields(): List<FormFieldKey> {
        val extra = buildList {
            if (requiresClientIpAddress) add(FormFieldKey.CLIENT_IP_ADDRESS)
            if (requiresWifiConfig() && useDifferentWifiNames) add(FormFieldKey.WIFI_SSID_5)
            if (requiresTvCpeKind()) add(FormFieldKey.TV_CPE_KIND)
        }
        return FormFieldKey.blockingForSubmit + extra
    }

    fun validated(vararg fields: FormFieldKey): RegisterSubscriptionFormState {
        val fieldsToValidate =
            if (fields.isEmpty()) blockingFields() else fields.asList()
        return fieldsToValidate.fold(this) { form, field ->
            form.withFieldError(field, form.validate(field))
        }
    }

    fun isValid(): Boolean {
        val form = validated()
        return blockingFields().all { form.validate(it) == null }
    }
}

private fun RegisterSubscriptionFormState.withFieldError(
    field: FormFieldKey,
    message: String?
): RegisterSubscriptionFormState {
    return when (field) {
        FormFieldKey.FIRST_NAME -> copy(firstNameError = message)
        FormFieldKey.LAST_NAME -> copy(lastNameError = message)
        FormFieldKey.DNI -> copy(dniError = message)
        FormFieldKey.ADDRESS -> copy(addressError = message)
        FormFieldKey.PHONE -> copy(phoneError = message)
        FormFieldKey.PLACE -> copy(placeError = message)
        FormFieldKey.PLAN -> copy(planError = message)
        FormFieldKey.ONU -> copy(onuError = message)
        FormFieldKey.NAP_BOX -> copy(napBoxError = message)
        FormFieldKey.FACADE_PHOTO -> copy(facadePhotoError = message)
        FormFieldKey.NOTE -> copy(noteError = message)
        FormFieldKey.HOST_DEVICE -> copy(hostDeviceError = message)
        FormFieldKey.EQUIPMENT_CONDITION -> this
        FormFieldKey.CLIENT_IP_ADDRESS -> copy(clientIpAddressError = message)
        FormFieldKey.WIFI_SSID_24 -> copy(wifiSsid24Error = message)
        FormFieldKey.WIFI_PASSWORD_24 -> copy(wifiPassword24Error = message)
        FormFieldKey.WIFI_SSID_5 -> copy(wifiSsid5Error = message)
        FormFieldKey.WIFI_PASSWORD_5 -> copy(wifiPassword5Error = message)
        FormFieldKey.LOCATION -> copy(locationError = message)
        FormFieldKey.TV_CPE_KIND -> copy(tvCpeKindError = message)
    }
}
