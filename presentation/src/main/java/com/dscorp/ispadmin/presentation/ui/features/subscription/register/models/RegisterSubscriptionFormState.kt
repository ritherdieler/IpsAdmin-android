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
    fun requiresNapBox(): Boolean {
        return installationType == InstallationType.FIBER ||
                installationType == InstallationType.ONLY_TV_FIBER
    }

    fun requiresOnu(): Boolean {
        return installationType == InstallationType.FIBER
    }

    fun validate(field: FormFieldKey): String? {
        return when (field) {
            FormFieldKey.FIRST_NAME -> when {
                firstName.isBlank() -> "Ingrese el nombre"
                !firstName.isAValidName() -> "El nombre debe tener al menos 2 caracteres"
                else -> null
            }

            FormFieldKey.LAST_NAME -> when {
                lastName.isBlank() -> "Ingrese el apellido"
                !lastName.isAValidName() -> "El apellido debe tener al menos 2 caracteres"
                else -> null
            }

            FormFieldKey.DNI -> when {
                dni.isBlank() -> "Ingrese el DNI"
                !dni.isValidDni(strictValidation = true) -> "El DNI debe contener 8 dígitos"
                else -> null
            }

            FormFieldKey.ADDRESS -> when {
                address.isBlank() -> "Ingrese la dirección"
                !address.isAValidAddress() -> "La dirección debe tener al menos 5 caracteres"
                else -> null
            }

            FormFieldKey.PHONE -> when {
                phone.isBlank() -> "Ingrese el teléfono"
                !phone.isValidPhone() -> "El teléfono debe tener 9 dígitos"
                else -> null
            }

            FormFieldKey.PLAN -> when {
                selectedPlan == null -> "Seleccione un plan"
                planList.none { it.id == selectedPlan.id } -> "Seleccione un plan válido"
                else -> null
            }

            FormFieldKey.PLACE -> if (selectedPlace == null) "Seleccione un lugar" else null

            FormFieldKey.ONU -> when {
                !requiresOnu() -> null
                selectedOnu == null -> "Seleccione una ONU"
                onuList.none { it.sn == selectedOnu.sn } -> "Seleccione una ONU válida"
                else -> null
            }

            FormFieldKey.NAP_BOX -> when {
                !requiresNapBox() -> null
                selectedNapBox == null -> "Seleccione una caja NAP"
                napBoxList.none { it.id == selectedNapBox.id } -> "Seleccione una caja NAP válida"
                else -> null
            }

            FormFieldKey.NOTE,
            FormFieldKey.EQUIPMENT_CONDITION -> null
        }
    }

    fun validated(vararg fields: FormFieldKey): RegisterSubscriptionFormState {
        val fieldsToValidate = if (fields.isEmpty()) FormFieldKey.values() else fields
        var validatedForm = this

        fieldsToValidate.forEach { field ->
            validatedForm = when (field) {
                FormFieldKey.FIRST_NAME -> validatedForm.copy(firstNameError = validatedForm.validate(field))
                FormFieldKey.LAST_NAME -> validatedForm.copy(lastNameError = validatedForm.validate(field))
                FormFieldKey.DNI -> validatedForm.copy(dniError = validatedForm.validate(field))
                FormFieldKey.ADDRESS -> validatedForm.copy(addressError = validatedForm.validate(field))
                FormFieldKey.PHONE -> validatedForm.copy(phoneError = validatedForm.validate(field))
                FormFieldKey.PLAN -> validatedForm.copy(planError = validatedForm.validate(field))
                FormFieldKey.PLACE -> validatedForm.copy(placeError = validatedForm.validate(field))
                FormFieldKey.ONU -> validatedForm.copy(onuError = validatedForm.validate(field))
                FormFieldKey.NAP_BOX -> validatedForm.copy(napBoxError = validatedForm.validate(field))
                FormFieldKey.NOTE,
                FormFieldKey.EQUIPMENT_CONDITION -> validatedForm
            }
        }

        return validatedForm
    }

    fun isValid(): Boolean {
        val validatedForm = validated()
        return validatedForm.firstNameError == null &&
                validatedForm.lastNameError == null &&
                validatedForm.dniError == null &&
                validatedForm.addressError == null &&
                validatedForm.phoneError == null &&
                validatedForm.planError == null &&
                validatedForm.placeError == null &&
                validatedForm.onuError == null &&
                validatedForm.napBoxError == null
    }
}
