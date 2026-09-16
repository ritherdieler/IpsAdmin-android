package com.dscorp.ispadmin.presentation.ui.features.subscription.register

import com.dscorp.ispadmin.domain.model.AccessMode

object E2eAccessModeResolver {
    @Volatile
    var override: String? = null

    fun resolve(raw: String? = override): AccessMode = AccessMode.parseOrDefault(raw)
}
