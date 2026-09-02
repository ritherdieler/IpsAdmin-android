package com.dscorp.ispadmin.presentation.ui.features.subscription.register

object RegisterSubscriptionTestTags {
    const val FIRST_NAME = "tf_register_first_name"
    const val LAST_NAME = "tf_register_last_name"
    const val DNI = "tf_register_dni"
    const val PHONE = "tf_register_phone"
    const val ADDRESS = "tf_register_address"
    const val NOTE = "tf_register_note"
    const val CLIENT_IP = "tf_client_ip_address"

    const val PLACE = "register_place_field"
    const val NAP_BOX = "register_nap_box_field"
    const val NEARBY_NAP_LOADING = "register_nearby_nap_loading"
    const val INSTALLATION_TYPE = "register_installation_type_dropdown"
    const val PLAN = "register_plan_dropdown"
    const val HOST_DEVICE = "register_host_device_dropdown"
    const val ONU = "register_onu_dropdown"
    const val VLAN = "register_vlan_dropdown"
    const val REFRESH_ONU = "btn_refresh_onu_list"

    const val FACADE_PHOTO = "register_facade_photo"
    const val SUBMIT = "btn_register_subscription"
    /** Present only while catalog/form load finished (`!isLoading`). */
    const val FORM_READY = "register_form_ready"

    const val WIZARD_CONTINUE = "wizard_continue"
    const val WIZARD_BACK = "wizard_back"
    const val LOCATION_METHOD_CURRENT = "location_method_current"
    const val LOCATION_METHOD_MANUAL = "location_method_manual"
    const val LOCATION_COORDINATES = "selected_location_coordinates"

    const val WIFI_DIFFERENT_NAMES = "cb_wifi_different_names"
    const val WIFI_SSID_24 = "tf_wifi_ssid_24"
    const val WIFI_SSID_5 = "tf_wifi_ssid_5"
    const val WIFI_PASSWORD_24 = "tf_wifi_password_24"
    const val WIFI_PASSWORD_5 = "tf_wifi_password_5"
    const val WIFI_PASSWORD_24_TOGGLE = "btn_toggle_wifi_password_24"

    const val ONU_ITEM_PREFIX = "register_onu_item_"
    const val PLAN_ITEM_PREFIX = "register_plan_item_"
    const val INSTALLATION_TYPE_ITEM_PREFIX = "register_installation_type_item_"
    const val HOST_DEVICE_ITEM_PREFIX = "register_host_device_item_"
    const val VLAN_ITEM_PREFIX = "register_vlan_item_"

    const val PROGRESS_OVERLAY = "registration_progress_overlay"
    const val PROGRESS_STEP = "registration_progress_step"
    const val PROGRESS_HINT = "registration_progress_hint"
    const val SUCCESS_FULLSCREEN = "register_success_fullscreen"
    const val SUCCESS_MESSAGE = "register_success_message"
    const val TR069_STATUS_CARD = "tr069_status_card"
    const val TR069_STATUS_MESSAGE = "tr069_status_message"

    fun onuItem(index: Int): String = "$ONU_ITEM_PREFIX$index"
    fun planItem(index: Int): String = "$PLAN_ITEM_PREFIX$index"
    fun installationTypeItem(index: Int): String = "$INSTALLATION_TYPE_ITEM_PREFIX$index"
    fun hostDeviceItem(index: Int): String = "$HOST_DEVICE_ITEM_PREFIX$index"
    fun vlanItem(index: Int): String = "$VLAN_ITEM_PREFIX$index"

    val interactive = listOf(
        FIRST_NAME,
        LAST_NAME,
        DNI,
        PHONE,
        ADDRESS,
        NOTE,
        PLACE,
        NAP_BOX,
        INSTALLATION_TYPE,
        PLAN,
        HOST_DEVICE,
        ONU,
        VLAN,
        REFRESH_ONU,
        FACADE_PHOTO,
        SUBMIT,
        WIZARD_CONTINUE,
        WIZARD_BACK,
        LOCATION_METHOD_CURRENT,
        LOCATION_METHOD_MANUAL,
        WIFI_DIFFERENT_NAMES,
        WIFI_SSID_24,
        WIFI_SSID_5,
        WIFI_PASSWORD_24,
        WIFI_PASSWORD_5,
        WIFI_PASSWORD_24_TOGGLE,
    )
}

object RegisterSubscriptionDebugActions {
    const val SET_FACADE_PHOTO = "com.dscorp.ispadmin.DEBUG_SET_FACADE_PHOTO"
    const val EXTRA_PATH = "path"
}

object RegisterSubscriptionNavTestTags {
    const val DRAWER_REGISTER = "drawer_nav_register_subscription"
}
