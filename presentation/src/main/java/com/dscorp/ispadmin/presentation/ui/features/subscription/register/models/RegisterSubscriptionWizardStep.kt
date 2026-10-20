package com.dscorp.ispadmin.presentation.ui.features.subscription.register.models

enum class RegisterSubscriptionWizardStep {
    SERVICE_TYPE,
    ONU_SELECTION,
    ONU_CONFIRMATION,
    WAITING_FOR_ACS,
    CLIENT_LOCATION,
    INSTALLATION,
    CONFIRMATION,
    ;

    fun next(): RegisterSubscriptionWizardStep? = when (this) {
        SERVICE_TYPE -> null
        ONU_SELECTION -> ONU_CONFIRMATION
        ONU_CONFIRMATION -> WAITING_FOR_ACS
        WAITING_FOR_ACS -> null
        CLIENT_LOCATION -> INSTALLATION
        INSTALLATION -> CONFIRMATION
        CONFIRMATION -> null
    }

    fun previous(): RegisterSubscriptionWizardStep? = when (this) {
        SERVICE_TYPE -> null
        ONU_SELECTION -> null
        ONU_CONFIRMATION -> ONU_SELECTION
        WAITING_FOR_ACS -> null
        CLIENT_LOCATION -> null
        INSTALLATION -> CLIENT_LOCATION
        CONFIRMATION -> INSTALLATION
    }

    fun afterAcsConfirmed(): RegisterSubscriptionWizardStep =
        if (this == WAITING_FOR_ACS) CLIENT_LOCATION else this

    fun isPreauthorizationStep(): Boolean = this in setOf(ONU_SELECTION, ONU_CONFIRMATION, WAITING_FOR_ACS)
}

fun wizardFieldsFor(
    step: RegisterSubscriptionWizardStep,
    form: RegisterSubscriptionFormState,
): List<FormFieldKey> = when (step) {
    RegisterSubscriptionWizardStep.SERVICE_TYPE -> emptyList()
    RegisterSubscriptionWizardStep.ONU_SELECTION -> listOf(FormFieldKey.ONU)
    RegisterSubscriptionWizardStep.ONU_CONFIRMATION,
    RegisterSubscriptionWizardStep.WAITING_FOR_ACS -> emptyList()
    RegisterSubscriptionWizardStep.CLIENT_LOCATION -> listOf(
        FormFieldKey.FIRST_NAME,
        FormFieldKey.LAST_NAME,
        FormFieldKey.DNI,
        FormFieldKey.PHONE,
        FormFieldKey.ADDRESS,
        FormFieldKey.PLACE,
        FormFieldKey.LOCATION,
    )
    RegisterSubscriptionWizardStep.INSTALLATION -> buildList {
        add(FormFieldKey.PLAN)
        add(FormFieldKey.HOST_DEVICE)
        if (form.requiresNapBox()) add(FormFieldKey.NAP_BOX)
        if (form.requiresOnu()) add(FormFieldKey.ONU)
        if (form.requiresWifiConfig()) {
            add(FormFieldKey.WIFI_SSID_24)
            add(FormFieldKey.WIFI_PASSWORD_24)
            if (form.useDifferentWifiNames) add(FormFieldKey.WIFI_SSID_5)
        }
    }
    RegisterSubscriptionWizardStep.CONFIRMATION -> emptyList()
}

fun canAdvanceWizardStep(
    step: RegisterSubscriptionWizardStep,
    form: RegisterSubscriptionFormState,
): Boolean = when (step) {
    RegisterSubscriptionWizardStep.ONU_CONFIRMATION -> form.selectedOnu != null
    RegisterSubscriptionWizardStep.WAITING_FOR_ACS -> false
    else -> wizardFieldsFor(step, form).all { form.validate(it) == null }
}
