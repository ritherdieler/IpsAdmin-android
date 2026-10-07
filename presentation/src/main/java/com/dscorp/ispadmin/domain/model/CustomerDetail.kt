package com.dscorp.ispadmin.domain.model

data class CustomerDetail(
    val id: Int,
    val firstName: String? = null,
    val lastName: String? = null,
    val dni: String? = null,
    val subscriptions: List<CustomerServiceSummary> = emptyList(),
)

data class CustomerServiceSummary(
    val id: Int,
    val serviceStatus: String? = null,
    val planName: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val installationType: String? = null,
    val totalDebt: Double? = null,
)
