package com.dscorp.ispadmin.presentation.ui.features.subscription.register.models

enum class FormFieldKey {
    FIRST_NAME,
    LAST_NAME,
    DNI,
    ADDRESS,
    PHONE,
    PLAN,
    PLACE,
    ONU,
    NAP_BOX,
    NOTE,
    FACADE_PHOTO,
    EQUIPMENT_CONDITION;

    companion object {
        val blockingForSubmit: List<FormFieldKey> =
            entries.filter { it != EQUIPMENT_CONDITION }
    }
}
