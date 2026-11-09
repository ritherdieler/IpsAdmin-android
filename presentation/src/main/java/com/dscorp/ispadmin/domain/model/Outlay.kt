package com.dscorp.ispadmin.domain.model

import java.util.Date

/** FIXED: recurring expense (admin/accountant, no receipt, no monthly cap). VARIABLE: any profile, capped per month. */
enum class OutlayType { FIXED, VARIABLE }

/** What the Core answered to a saved outlay: its id and whether the whole team was notified of the month's cap. */
data class OutlaySaveResult(val id: Int?, val teamNotified: Boolean)

data class Outlay(
    val amount: String?=null,
    val description: String?=null,
    val document_code: String?=null,
    val date: Date? = null,
    val category: String? = null,
    val receipt_url: String? = null,
    val cost_center: String? = null,
    var responsibleId: Int? = null,
    val responsibleName: String? = null,
    val type: OutlayType = OutlayType.VARIABLE,
) {
    fun isValid(): Boolean {
        val isAmountValid = amount != null && amount.toDoubleOrNull()?.let { it > 0 } ?: false
        val isDescriptionValid = !description.isNullOrEmpty()
        val isCategoryValid = !category.isNullOrEmpty()
        // The approved design (egresos fijos y variables) no longer asks for a cost centre.
        return isAmountValid && isDescriptionValid && isCategoryValid
    }
}
