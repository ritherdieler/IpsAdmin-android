package com.dscorp.ispadmin.presentation.ui.features.subscription.register

object E2eAccessModeResolver {
    @Volatile
    var override: String? = null

    fun resolve(raw: String? = override): String? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return if (value.equals("STATIC_IP", ignoreCase = true)) "STATIC_IP" else null
    }
}
