package com.dscorp.ispadmin.domain.model

enum class InstallationType {
    FIBER,
    WIRELESS,
    ONLY_TV_FIBER;

    override fun toString(): String {
        return when (this) {
            FIBER -> "Fibra"
            WIRELESS -> "Inalambrico"
            ONLY_TV_FIBER -> "Tv Cable Fibra"
        }
    }
}