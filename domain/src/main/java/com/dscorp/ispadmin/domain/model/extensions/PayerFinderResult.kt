package com.dscorp.ispadmin.domain.model.extensions

data class PayerFinderResult(
    val subscriptionId: Int,
    val subscriptionName: String,
    val electronicPayerName: String,
    val paymentMethod: String,
    val paymentDate: String,
    val amountPaid: Double,
)