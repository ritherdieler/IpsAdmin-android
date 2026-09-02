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
    HOST_DEVICE,
    EQUIPMENT_CONDITION,
    CLIENT_IP_ADDRESS,
    WIFI_SSID_24,
    WIFI_PASSWORD_24,
    WIFI_SSID_5,
    WIFI_PASSWORD_5,
    LOCATION,
    TV_CPE_KIND;

    companion object {
        val blockingForSubmit: List<FormFieldKey> =
            entries.filter {
                it != EQUIPMENT_CONDITION &&
                    it != CLIENT_IP_ADDRESS &&
                    it != WIFI_SSID_5 &&
                    it != WIFI_PASSWORD_5 &&
                    it != TV_CPE_KIND
            }
    }
}
