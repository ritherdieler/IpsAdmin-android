package com.example.cleanarchitecture.domain.entity

enum class ServiceStatus {
    ACTIVE,
    CUT_OFF,
    SUSPENDED,
    CANCELLED;

    fun getFormattedStatus(): String {
        return when (this) {
            CANCELLED -> "Cancelado"
            else -> "Activo"
        }
    }
}