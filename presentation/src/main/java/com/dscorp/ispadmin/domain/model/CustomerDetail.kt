package com.dscorp.ispadmin.domain.model

data class CustomerDetail(
    val id: Int,
    val firstName: String? = null,
    val lastName: String? = null,
    val dni: String? = null,
    val subscriptions: List<CustomerServiceSummary> = emptyList(),
    val creditBalance: Double = 0.0,
    val clientType: String? = null,
    /** DNI, RUC, CE or PAS; null when the document is missing or a placeholder. */
    val documentType: String? = null,
    val ruc: String? = null,
    val businessName: String? = null,
    val email: String? = null,
    /** Open debt of all the customer's services, added up by the Core. */
    val totalDebt: Double = 0.0,
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
