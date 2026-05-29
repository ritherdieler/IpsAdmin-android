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
import com.dscorp.ispadmin.domain.model.subscription.subscriptionDniError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionFacadePhotoError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionFirstNameError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionLastNameError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionNapBoxError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionNoteError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionOnuError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionPhoneError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionPlaceError
import com.dscorp.ispadmin.domain.model.subscription.subscriptionPlanError
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
    val noteError: String? = null,
    val facadePhotoUri: Uri? = null,
    val facadePhotoError: String? = null,
    val installationType: InstallationType = InstallationType.FIBER,
    val equipmentCondition: EquipmentCondition = EquipmentCondition.LOAN,
) {
    fun requiresNapBox(): Boolean {
        return installationType == InstallationType.FIBER ||
            installationType == InstallationType.ONLY_TV_FIBER
    }

    fun requiresOnu(): Boolean {
        return installationType == InstallationType.FIBER
    }

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
            FormFieldKey.EQUIPMENT_CONDITION -> null
        }
    }

    fun validated(vararg fields: FormFieldKey): RegisterSubscriptionFormState {
        val fieldsToValidate =
            if (fields.isEmpty()) FormFieldKey.entries else fields.asList()
        return fieldsToValidate.fold(this) { form, field ->
            form.withFieldError(field, form.validate(field))
        }
    }

    fun isValid(): Boolean {
        val form = validated()
        return FormFieldKey.blockingForSubmit.all { form.validate(it) == null }
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
        FormFieldKey.PLAN -> copy(planError = message)
        FormFieldKey.PLACE -> copy(placeError = message)
        FormFieldKey.ONU -> copy(onuError = message)
        FormFieldKey.NAP_BOX -> copy(napBoxError = message)
        FormFieldKey.FACADE_PHOTO -> copy(facadePhotoError = message)
        FormFieldKey.NOTE -> copy(noteError = message)
        FormFieldKey.EQUIPMENT_CONDITION -> this
    }
}
