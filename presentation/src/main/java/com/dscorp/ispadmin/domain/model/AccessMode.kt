package com.dscorp.ispadmin.domain.model

enum class AccessMode {
    STATIC_IP,
    PPPOE_FIXED,
    PPPOE_DYNAMIC;

    fun usesPppoe(): Boolean = this == PPPOE_FIXED || this == PPPOE_DYNAMIC

    companion object {
        fun parse(raw: String?): AccessMode? =
            entries.find { it.name.equals(raw, ignoreCase = true) }
    }
}
