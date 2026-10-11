package com.dscorp.ispadmin.domain.model

/** Payment method from the Core catalog; electronic wallets require the payer name. */
data class PaymentMethodOption(
    val name: String,
    val requiresPayerName: Boolean = false,
)
