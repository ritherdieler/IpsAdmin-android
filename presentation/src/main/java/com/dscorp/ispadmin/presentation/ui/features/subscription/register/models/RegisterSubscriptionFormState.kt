package com.dscorp.ispadmin.presentation.ui.features.subscription.register.models

import com.dscorp.ispadmin.domain.model.EquipmentCondition
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.NetworkDevice
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.extensions.isAValidAddress
import com.dscorp.ispadmin.domain.model.extensions.isAValidName
import com.dscorp.ispadmin.domain.model.extensions.isValidDni
import com.dscorp.ispadmin.domain.model.extensions.isValidPhone
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
    val selectedHostDevice: NetworkDevice? = null,
    val location: LatLng? = null,
    val cpeDevice: NetworkDevice? = null,
    val napBoxError: String? = null,
    val napBoxList: List<NapBoxResponse> = emptyList(),
    val selectedNapBox: NapBoxResponse? = null,
    val onuList: List<Onu> = emptyList(),
    val selectedOnu: Onu? = null,
    val onuError: String? = null,
    val coupon: String = "",
    val note: String = "",
    val installationType: InstallationType = InstallationType.FIBER,
    val equipmentCondition: EquipmentCondition = EquipmentCondition.LOAN,
) {
    fun isValid(): Boolean {
        val isFirstNameValid = firstName.isNotBlank() && firstName.isAValidName()
        val isLastNameValid = lastName.isNotBlank() && lastName.isAValidName()
        val isDniValid = dni.isNotBlank() && dni.isValidDni(strictValidation = true)
        val isAddressValid = address.isNotBlank() && address.isAValidAddress()
        val isPhoneValid = phone.isNotBlank() && phone.isValidPhone()
        val isPlanSelected = selectedPlan != null
        val isPlaceSelected = selectedPlace != null
        val requiresNapBox = installationType == InstallationType.FIBER ||
                installationType == InstallationType.ONLY_TV_FIBER
        val requiresOnu = installationType == InstallationType.FIBER
        val installationRequirementsValid = (!requiresOnu || selectedOnu != null) &&
                (!requiresNapBox || selectedNapBox != null)

        val noErrors = firstNameError == null &&
                lastNameError == null &&
                dniError == null &&
                addressError == null &&
                phoneError == null &&
                planError == null &&
                placeError == null &&
                onuError == null &&
                napBoxError == null

        return isFirstNameValid && isLastNameValid && isDniValid &&
                isAddressValid && isPhoneValid && isPlanSelected &&
                isPlaceSelected && installationRequirementsValid && noErrors
    }

}
