package com.example.cleanarchitecture.domain.entity

data class FixedCost(
    val amount: Double,
    val description: String,
    val note: String,
    val type: FixedCostType,
    val userId: Int
)

enum class FixedCostType {
    STAFF_PAYMENT,
    PROVIDER_PAYMENT,
    SYSTEM_INFRASTRUCTURE,
    OFFICE,
    OTHER;

    override fun toString(): String {
        return when (this) {
            STAFF_PAYMENT -> "Pago de personal"
            PROVIDER_PAYMENT -> "Pago a proveedor"
            SYSTEM_INFRASTRUCTURE -> "Infraestructura de sistema"
            OFFICE -> "Oficina"
            OTHER -> "Otros"
        }
    }
}