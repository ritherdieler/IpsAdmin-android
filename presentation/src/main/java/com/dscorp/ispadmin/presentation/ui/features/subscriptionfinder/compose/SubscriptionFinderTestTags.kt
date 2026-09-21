package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

object SubscriptionFinderTestTags {
    const val FILTER_NAME = "subscription_search_filter_name"
    const val FILTER_DOCUMENT = "subscription_search_filter_document"
    const val FILTER_DATE = "subscription_search_filter_date"
    const val FILTER_IP = "subscription_search_filter_ip"
    const val FILTER_CODE = "subscription_search_filter_code"

    const val QUERY_NAME = "subscription_search_query_name"
    const val QUERY_DOCUMENT = "subscription_search_query_document"
    const val QUERY_IP = "subscription_search_query_ip"
    const val QUERY_CODE = "subscription_search_query_code"

    const val DATE_START = "subscription_search_date_start"
    const val DATE_END = "subscription_search_date_end"
    const val DATE_SUBMIT = "subscription_search_date_submit"
    const val DATE_PICKER_CONFIRM = "subscription_search_date_picker_confirm"
    const val DATE_PICKER_DISMISS = "subscription_search_date_picker_dismiss"

    const val MENU_PAYMENT_HISTORY = "payment_history"
    const val MENU_EDIT_PLAN = "edit_plan"
    const val MENU_SEE_DETAILS = "see_details"
    const val MENU_MIGRATE_FIBER = "migrate_fiber"
    const val MENU_CANCEL = "cancel"
    const val MENU_REACTIVATE = "reactivate"
    const val MENU_CHANGE_NAP_BOX = "change_nap_box"
    const val MENU_UPDATE_LOCATION = "update_location"
    const val MENU_REBOOT_ONU = "reboot_onu"
    const val MENU_RETRY_TR069 = "retry_tr069"

    const val RETRY_TR069_DIALOG_DISMISS = "subscription_retry_tr069_dialog_dismiss"
    const val RETRY_TR069_DIALOG_CONFIRM = "subscription_retry_tr069_dialog_confirm"

    const val CANCEL_DIALOG_DISMISS = "subscription_cancel_dialog_dismiss"
    const val CANCEL_DIALOG_CONFIRM = "subscription_cancel_dialog_confirm"

    const val REACTIVATE_DIALOG_DISMISS = "subscription_reactivate_dialog_dismiss"
    const val REACTIVATE_DIALOG_CONFIRM = "subscription_reactivate_dialog_confirm"
    const val REACTIVATE_NOTES = "subscription_reactivate_notes"

    const val REBOOT_DIALOG_DISMISS = "subscription_reboot_dialog_dismiss"
    const val REBOOT_DIALOG_CONFIRM = "subscription_reboot_dialog_confirm"

    const val NAP_BOX_DROPDOWN = "subscription_nap_box_dropdown"
    const val NAP_BOX_CONFIRM = "subscription_nap_box_confirm"
    const val NAP_BOX_CLOSE = "subscription_nap_box_close"

    const val LOCATION_BACK = "subscription_location_back"
    const val LOCATION_MAP = "subscription_location_map"
    const val LOCATION_GPS = "subscription_location_gps"
    const val LOCATION_CANCEL = "subscription_location_cancel"
    const val LOCATION_UPDATE = "subscription_location_update"

    fun listRetryTr069(subscriptionId: Int) = "btn_list_retry_tr069_$subscriptionId"
    fun resultItem(subscriptionId: Int) = "subscription_result_item_$subscriptionId"
    fun resultExpand(subscriptionId: Int) = "subscription_result_expand_$subscriptionId"
    fun resultMenu(subscriptionId: Int) = "subscription_result_menu_$subscriptionId"
    fun resultMenuItem(subscriptionId: Int, action: String) = "subscription_result_menu_${action}_$subscriptionId"
    fun resultMap(subscriptionId: Int) = "subscription_result_map_$subscriptionId"
    fun resultWhatsapp(subscriptionId: Int) = "subscription_result_whatsapp_$subscriptionId"
    fun resultIp(subscriptionId: Int) = "subscription_result_ip_$subscriptionId"
    fun customerName(subscriptionId: Int) = "subscription_result_customer_name_$subscriptionId"
    fun customerLastName(subscriptionId: Int) = "subscription_result_customer_last_name_$subscriptionId"
    fun customerPhone(subscriptionId: Int) = "subscription_result_customer_phone_$subscriptionId"
    fun customerDni(subscriptionId: Int) = "subscription_result_customer_dni_$subscriptionId"
    fun customerPlace(subscriptionId: Int) = "subscription_result_customer_place_$subscriptionId"
    fun customerAddress(subscriptionId: Int) = "subscription_result_customer_address_$subscriptionId"
    fun customerEmail(subscriptionId: Int) = "subscription_result_customer_email_$subscriptionId"
    fun customerSave(subscriptionId: Int) = "subscription_result_customer_save_$subscriptionId"

    fun menuActionTag(menu: SubscriptionMenu): String = when (menu) {
        SubscriptionMenu.SHOW_PAYMENT_HISTORY -> MENU_PAYMENT_HISTORY
        SubscriptionMenu.EDIT_PLAN_SUBSCRIPTION -> MENU_EDIT_PLAN
        SubscriptionMenu.SEE_DETAILS -> MENU_SEE_DETAILS
        SubscriptionMenu.MIGRATE_TO_FIBER -> MENU_MIGRATE_FIBER
        SubscriptionMenu.CANCEL_SUBSCRIPTION -> MENU_CANCEL
        SubscriptionMenu.REACTIVATE_SERVICE -> MENU_REACTIVATE
        SubscriptionMenu.CHANGE_NAP_BOX -> MENU_CHANGE_NAP_BOX
        SubscriptionMenu.UPDATE_LOCATION -> MENU_UPDATE_LOCATION
        SubscriptionMenu.REBOOT_FIBER_ONU -> MENU_REBOOT_ONU
        SubscriptionMenu.RETRY_TR069 -> MENU_RETRY_TR069
    }

    val searchInteractive = listOf(
        FILTER_NAME,
        FILTER_DOCUMENT,
        FILTER_DATE,
        FILTER_IP,
        FILTER_CODE,
        QUERY_NAME,
        QUERY_DOCUMENT,
        QUERY_IP,
        QUERY_CODE,
        DATE_START,
        DATE_END,
        DATE_SUBMIT,
        DATE_PICKER_CONFIRM,
        DATE_PICKER_DISMISS
    )

    val dialogInteractive = listOf(
        CANCEL_DIALOG_DISMISS,
        CANCEL_DIALOG_CONFIRM,
        REACTIVATE_DIALOG_DISMISS,
        REACTIVATE_DIALOG_CONFIRM,
        REACTIVATE_NOTES,
        REBOOT_DIALOG_DISMISS,
        REBOOT_DIALOG_CONFIRM,
        RETRY_TR069_DIALOG_DISMISS,
        RETRY_TR069_DIALOG_CONFIRM,
        NAP_BOX_DROPDOWN,
        NAP_BOX_CONFIRM,
        NAP_BOX_CLOSE,
        LOCATION_BACK,
        LOCATION_MAP,
        LOCATION_GPS,
        LOCATION_CANCEL,
        LOCATION_UPDATE
    )
}

object SubscriptionFinderContentDescriptions {
    const val EMPTY_STATE_ICON = "Estado de búsqueda de suscripciones"
    const val SEARCH_LOADING = "Búsqueda en progreso"
    const val DATE_SEARCH_ICON = "Buscar por rango de fechas"
    const val NAP_BOX_ERROR = "Error al cargar NAP Box"
    const val NAP_BOX_SUCCESS = "NAP Box cambiado exitosamente"
    const val LOCATION_ERROR = "Error al actualizar ubicación"
}
