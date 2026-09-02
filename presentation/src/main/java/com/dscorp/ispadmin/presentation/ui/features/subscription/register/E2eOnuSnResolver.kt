package com.dscorp.ispadmin.presentation.ui.features.subscription.register

object E2eOnuSnResolver {
    fun resolve(onuSnArg: String?): String? =
        onuSnArg?.trim()?.takeIf { it.isNotEmpty() }

    fun shouldSelectFirstOnu(onuSnArg: String?): Boolean = resolve(onuSnArg) == null

    fun matches(displayedSn: String, wanted: String): Boolean {
        val wantedCompact = compact(wanted)
        if (wantedCompact.isEmpty()) return false
        val displayedCompact = compact(displayedSn)
        if (displayedCompact.contains(wantedCompact) || wantedCompact.contains(displayedCompact)) {
            return true
        }
        val wantedHex = vendorPrefixToHex(wantedCompact)
        return wantedHex != wantedCompact && displayedCompact.contains(wantedHex)
    }

    private fun compact(value: String): String =
        value.uppercase().replace(Regex("[^A-Z0-9]"), "")

    private fun vendorPrefixToHex(compactSn: String): String {
        val prefixes = listOf("ZTEG" to "5A544547", "HWTC" to "48575443", "VSOL" to "56534F4C")
        prefixes.forEach { (ascii, hex) ->
            if (compactSn.startsWith(ascii)) {
                return hex + compactSn.removePrefix(ascii)
            }
        }
        return compactSn
    }
}
