package com.dscorp.ispadmin.domain.model.subscription

import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.extensions.isAValidName
import com.dscorp.ispadmin.domain.model.extensions.isValidDni
import com.dscorp.ispadmin.domain.model.extensions.isValidPhone

object RegisterSubscriptionFormConstraints {
    const val MAX_PERSON_NAME_LENGTH = 50
    const val MAX_DNI_INPUT_LENGTH = 8
    const val MAX_PHONE_LENGTH = 9
    const val MAX_NOTE_LENGTH = 180
    const val MIN_ADDRESS_CHARS = 5
}

fun subscriptionFirstNameError(value: String): String? = when {
    value.length > RegisterSubscriptionFormConstraints.MAX_PERSON_NAME_LENGTH ->
        "El nombre no puede superar ${RegisterSubscriptionFormConstraints.MAX_PERSON_NAME_LENGTH} caracteres"
    value.isBlank() -> "Ingrese el nombre"
    !value.isAValidName() -> "El nombre debe tener al menos 2 caracteres"
    else -> null
}

fun subscriptionLastNameError(value: String): String? = when {
    value.length > RegisterSubscriptionFormConstraints.MAX_PERSON_NAME_LENGTH ->
        "El apellido no puede superar ${RegisterSubscriptionFormConstraints.MAX_PERSON_NAME_LENGTH} caracteres"
    value.isBlank() -> "Ingrese el apellido"
    !value.isAValidName() -> "El apellido debe tener al menos 2 caracteres"
    else -> null
}

fun subscriptionDniError(value: String): String? = when {
    value.length > RegisterSubscriptionFormConstraints.MAX_DNI_INPUT_LENGTH ->
        "El DNI no puede superar ${RegisterSubscriptionFormConstraints.MAX_DNI_INPUT_LENGTH} dígitos"
    value.isBlank() -> "Ingrese el DNI"
    !value.isValidDni(strictValidation = true) -> "El DNI debe contener 8 dígitos"
    else -> null
}

fun subscriptionAddressError(value: String): String? = when {
    value.isBlank() -> "Ingrese la dirección"
    value.length < RegisterSubscriptionFormConstraints.MIN_ADDRESS_CHARS ->
        "La dirección debe tener al menos ${RegisterSubscriptionFormConstraints.MIN_ADDRESS_CHARS} caracteres"
    else -> null
}

fun subscriptionPhoneError(value: String): String? = when {
    value.length > RegisterSubscriptionFormConstraints.MAX_PHONE_LENGTH ->
        "El teléfono no puede superar ${RegisterSubscriptionFormConstraints.MAX_PHONE_LENGTH} dígitos"
    value.isBlank() -> "Ingrese el teléfono"
    !value.isValidPhone() -> "El teléfono debe tener 9 dígitos"
    else -> null
}

fun subscriptionNoteError(value: String): String? = when {
    value.length > RegisterSubscriptionFormConstraints.MAX_NOTE_LENGTH ->
        "Máximo ${RegisterSubscriptionFormConstraints.MAX_NOTE_LENGTH} caracteres"
    else -> null
}

fun subscriptionPlanError(selectedPlan: PlanResponse?, planList: List<PlanResponse>): String? = when {
    selectedPlan == null -> "Seleccione un plan"
    planList.none { it.id == selectedPlan.id } -> "Seleccione un plan válido"
    else -> null
}

fun subscriptionPlaceError(selectedPlace: Place?): String? =
    if (selectedPlace == null) "Seleccione un lugar" else null

fun subscriptionOnuError(
    requiresOnu: Boolean,
    selectedOnu: Onu?,
    onuList: List<Onu>
): String? = when {
    !requiresOnu -> null
    selectedOnu == null -> "Seleccione una ONU"
    onuList.none { it.sn == selectedOnu.sn } -> "Seleccione una ONU válida"
    else -> null
}

fun subscriptionNapBoxError(
    requiresNapBox: Boolean,
    selectedNapBox: NapBoxResponse?,
    napBoxList: List<NapBoxResponse>
): String? = when {
    !requiresNapBox -> null
    selectedNapBox == null -> "Seleccione una caja NAP"
    napBoxList.none { it.id == selectedNapBox.id } -> "Seleccione una caja NAP válida"
    else -> null
}

fun subscriptionOnuErrorAfterListRefresh(
    requiresOnu: Boolean,
    previousSelected: Onu?,
    newSelected: Onu?,
    newList: List<Onu>,
    previousFieldError: String?
): String? {
    if (!requiresOnu) return null
    if (previousSelected != null && newSelected == null) {
        return "La ONU seleccionada ya no está disponible"
    }
    if (previousFieldError != null) {
        return subscriptionOnuError(requiresOnu, newSelected, newList)
    }
    return null
}

fun subscriptionNapBoxErrorAfterNearbyRefresh(
    requiresNapBox: Boolean,
    previousSelected: NapBoxResponse?,
    newSelected: NapBoxResponse?,
    newList: List<NapBoxResponse>,
    previousFieldError: String?
): String? {
    if (!requiresNapBox) return null
    if (previousSelected != null && newSelected == null) {
        return "La caja NAP seleccionada ya no está disponible"
    }
    if (previousFieldError != null) {
        return subscriptionNapBoxError(requiresNapBox, newSelected, newList)
    }
    return null
}
