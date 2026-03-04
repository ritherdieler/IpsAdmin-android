package com.dscorp.ispadmin.domain.model

enum class ServiceStatus {
    ACTIVE,
    CANCELLED;

    fun getFormattedStatus(): String {
        return when (this) {
            CANCELLED -> "Cancelado"

            else -> "Activo"
        }
    }
}