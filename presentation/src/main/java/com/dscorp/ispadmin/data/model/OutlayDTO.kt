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
    val responsibleId: Int? = null
)


