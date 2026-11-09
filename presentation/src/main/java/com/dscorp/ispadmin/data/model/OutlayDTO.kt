package com.dscorp.ispadmin.data.model

import java.util.Date

data class OutlayDTO(
    val id: Int? = null,
    val amount: Double,
    val document_code: String? = null,
    val description: String,
    val date: Date,
    val category: String? = null,
    val receipt_urls: List<String> = emptyList(),
    val cost_center: String? = null,
    val responsibleName: String? = null,
    val responsibleId: Int? = null,
    /** "FIXED" or "VARIABLE"; null on Core versions that predate outlay types (counts as variable). */
    val type: String? = null,
    /** True when this outlay took the month past the alert threshold and the Core notified the team. */
    val teamNotified: Boolean = false,
)


