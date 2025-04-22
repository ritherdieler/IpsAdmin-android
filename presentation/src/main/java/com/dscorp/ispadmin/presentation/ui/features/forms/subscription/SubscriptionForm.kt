package com.dscorp.ispadmin.presentation.ui.features.forms.subscription

import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.NetworkDevice
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.model.extensions.isValidDni
import com.dscorp.ispadmin.domain.model.extensions.isValidIpv4
import com.dscorp.ispadmin.domain.model.extensions.isValidPhone
import com.dscorp.ispadmin.presentation.ui.features.formvalidation.ReactiveFormField
import com.google.android.gms.maps.model.LatLng


abstract class SubscriptionForm {

    val idField = ReactiveFormField<String>(
        validator = { it != null }
    )

    val firstNameField = ReactiveFormField<String?>(
        hintResourceId = R.string.name,
        errorResourceId = R.string.fieldMustNotBeEmpty,
        validator = { !it.isNullOrEmpty() && it.matches(Regex("^[a-zA-Z\\s]+$")) }
    )

    val lastNameField = ReactiveFormField<String?>(
        hintResourceId = R.string.lastName,
        errorResourceId = R.string.fieldMustNotBeEmpty,
        validator = { !it.isNullOrEmpty() && it.matches(Regex("^[a-zA-Z\\s]+$")) }
    )

    val dniField = ReactiveFormField<String?>(
        hintResourceId = R.string.dni,
        errorResourceId = R.string.invalidDNI,
        validator = { it.isValidDni() }
    )

    val addressField = ReactiveFormField<String?>(
        hintResourceId = R.string.address,
        errorResourceId = R.string.fieldMustNotBeEmpty,
        validator = { !it.isNullOrEmpty() && it.matches(Regex("^[a-zA-Z0-9\\s]+$")) }
    )

    val phoneField = ReactiveFormField<String?>(
        hintResourceId = R.string.phoneNumer,
        errorResourceId = R.string.invalidPhoneNumber,
        validator = { it.isValidPhone() }
    )

    val couponField = ReactiveFormField<String?>(
        hintResourceId = R.string.coupon,
        errorResourceId = R.string.fieldMustNotBeEmpty,
        validator = { true }
    )

    val priceField = ReactiveFormField<String?>(
        hintResourceId = R.string.price,
        errorResourceId = R.string.invalidPrice,
        validator = { (it != null) && it.isNotEmpty() && (it.toDouble() > 0) }
    )

    val locationField = ReactiveFormField<LatLng?>(
        hintResourceId = R.string.location,
        errorResourceId = R.string.mustSelectLocation,
        validator = { it != null }
    )

    val planField = ReactiveFormField<PlanResponse?>(
        hintResourceId = R.string.plan,
        errorResourceId = R.string.mustSelectPlan,
        validator = { it != null }
    )

    val onuField = ReactiveFormField<Onu?>(
        hintResourceId = R.string.select_onu,
        errorResourceId = R.string.mustSelectOnu,
        validator = {
            if (planField.getValue()?.name?.contains("cable") == true) true
            else (it != null)
        }
    )

    val placeField = ReactiveFormField<Place?>(
        hintResourceId = R.string.place,
        errorResourceId = R.string.mustSelectPlace,
        validator = { it != null }
    )

    val technicianField = ReactiveFormField<User?>(
        hintResourceId = R.string.technician,
        errorResourceId = R.string.mustSelectTechnician,
        validator = { it != null }
    )

    val hostDeviceField = ReactiveFormField<NetworkDevice?>(
        hintResourceId = R.string.host_device,
        errorResourceId = R.string.mustSelectHostDevice,
        validator = { it != null }
    )

    val subscriptionDateField = ReactiveFormField<Long?>(
        hintResourceId = R.string.subscriptionDate,
        errorResourceId = R.string.mustSelectSubscriptionDate,
        validator = { it != null }
    )

    val migrationField = ReactiveFormField<Boolean?>(
        hintResourceId = R.string.isMigration,
        errorResourceId = R.string.empty,
        validator = { true }
    )

    val cpeDeviceField = ReactiveFormField<NetworkDevice?>(
        hintResourceId = R.string.select_cpe_device,
        errorResourceId = R.string.mustSelectCpeDevice,
        validator = { it != null }
    )

    val napBoxField = ReactiveFormField<NapBoxResponse?>(
        hintResourceId = R.string.selec_nap_box,
        errorResourceId = R.string.mustSelectNapBox,
        validator = { it != null }
    )



    val additionalDevicesField = ReactiveFormField<NetworkDevice?>(
        hintResourceId = R.string.additionalDevices,
        errorResourceId = R.string.youCanSelectAdditionalNetworkDevices,
        validator = { true }
    )

    val noteField = ReactiveFormField<String?>(
        hintResourceId = R.string.note,
        errorResourceId = R.string.errorNote,
        validator = { true }
    )

    val ipField = ReactiveFormField<String?>(
        hintResourceId = R.string.ip_address,
        errorResourceId = R.string.must_digit_a_valid_ip,
        validator = { it?.isValidIpv4() ?: false }
    )
}