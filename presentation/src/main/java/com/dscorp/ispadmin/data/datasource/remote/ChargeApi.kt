package com.dscorp.ispadmin.data.datasource.remote

import com.dscorp.ispadmin.domain.model.Payment
import retrofit2.http.Body
import retrofit2.http.PUT

/** Core multi-receipt charge (PUT payment/batch): one transaction, collector taken from the session. */
interface ChargeApi {
    @PUT("payment/batch")
    suspend fun registerBatch(@Body batch: PaymentBatchRequest): List<Payment>
}

data class PaymentBatchRequest(
    val paymentIds: List<Int>,
    val method: String,
    val discountAmount: Double = 0.0,
    val discountReason: String? = null,
    val electronicPayerName: String? = null,
)
