package com.example.data2.data.apirequestmodel

import com.dscorp.ispadmin.domain.model.FixedCostType

data class FixedCostRequest(
    val amount: Double = 0.0,
    val description: String = "",
    val note: String = "",
    val type: FixedCostType? = null,
    var userId: Int = 0
) {
    fun isValid(): Boolean {
        return amount > 0 && description.isNotBlank() && userId > 0 && type != null
    }
}

