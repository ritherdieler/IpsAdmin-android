package com.dscorp.ispadmin.domain.model

enum class EquipmentCondition {
    LOAN,
    SOLD;

    override fun toString(): String {
        return when (this) {
            LOAN -> "Préstamo"
            SOLD -> "Vendido"
        }
    }
}





