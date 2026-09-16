package com.dscorp.ispadmin.domain.model

enum class AccessMode {
    STATIC_IP,
    PPPOE_FIXED,
    PPPOE_DYNAMIC;

    fun usesPppoe(): Boolean = this == PPPOE_FIXED || this == PPPOE_DYNAMIC

    fun registerLabel(): String = when (this) {
        STATIC_IP -> "IP estática"
        PPPOE_FIXED, PPPOE_DYNAMIC -> "PPPoE"
    }

    companion object {
        fun parse(raw: String?): AccessMode? {
            val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            entries.find { it.name.equals(value, ignoreCase = true) }?.let { return it }
            return when (value.lowercase()) {
                "pppoe" -> PPPOE_DYNAMIC
                "static" -> STATIC_IP
                else -> null
            }
        }

        fun parseOrDefault(raw: String?): AccessMode = parse(raw) ?: PPPOE_DYNAMIC

        fun registerChoices(): List<AccessMode> = listOf(PPPOE_DYNAMIC, STATIC_IP)
    }
}
