package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail

object SubscriptionDetailTestTags {
    const val CALL = "subscription_detail_call"
    const val WHATSAPP = "subscription_detail_whatsapp"
    const val FACADE_PHOTO = "subscription_detail_facade_photo"
    const val FACADE_DIALOG_CAMERA = "subscription_detail_facade_dialog_camera"
    const val FACADE_DIALOG_GALLERY = "subscription_detail_facade_dialog_gallery"
    const val FACADE_FULL_UPDATE = "subscription_detail_facade_full_update"
    const val FACADE_FULL_CLOSE = "subscription_detail_facade_full_close"
    const val NETWORK_ACCESS_VALUE = "subscription_detail_network_access_value"
    const val MIGRATE_PPPOE = "subscription_detail_migrate_pppoe"
    const val MIGRATION_STAGE = "subscription_detail_migration_stage"
    const val MIGRATION_PROGRESS = "subscription_detail_migration_progress"
    const val MIGRATION_ERROR = "subscription_detail_migration_error"

    val interactive = listOf(
        CALL,
        WHATSAPP,
        FACADE_PHOTO,
        FACADE_DIALOG_CAMERA,
        FACADE_DIALOG_GALLERY,
        FACADE_FULL_UPDATE,
        FACADE_FULL_CLOSE,
        MIGRATE_PPPOE,
    )
}

object SubscriptionDetailContentDescriptions {
    const val FACADE_PHOTO = "Foto de fachada del cliente"
    const val FACADE_PHOTO_FULL = "Foto de fachada ampliada"
}
