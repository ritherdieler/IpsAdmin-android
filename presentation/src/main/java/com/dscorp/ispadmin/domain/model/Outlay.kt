package com.dscorp.ispadmin.domain.model

import java.util.Date

data class Outlay(
    val amount: String?=null,
    val description: String?=null,
    val document_code: String?=null,
    val date: Date? = null,
    val category: String? = null,
    val receipt_url: String? = null,
    val cost_center: String? = null,
    var responsibleId: Int? = null,
    val responsibleName: String? = null
) {
    fun isValid(): Boolean {
        val isAmountValid = amount != null && amount.toDoubleOrNull()?.let { it > 0 } ?: false
        val isDescriptionValid = !description.isNullOrEmpty()
        val isCategoryValid = !category.isNullOrEmpty()
        val isCostCenterValid = !cost_center.isNullOrEmpty()
        return isAmountValid && isDescriptionValid && isCategoryValid && isCostCenterValid
    }
}

