package com.dscorp.ispadmin.presentation.ui.features.subscription.register

object E2eNapCodeResolver {
    fun resolve(napCodeArg: String?): String? =
        napCodeArg?.trim()?.takeIf { it.isNotEmpty() }

    fun matches(displayed: String, wanted: String): Boolean {
        val wantedCompact = compact(wanted)
        if (wantedCompact.isEmpty()) return false
        val displayedCompact = compact(displayed)
        return displayedCompact.contains(wantedCompact) ||
            displayed.contains(wanted, ignoreCase = true)
    }

    private fun compact(value: String): String =
        value.uppercase().replace(Regex("[^A-Z0-9]"), "")
}
