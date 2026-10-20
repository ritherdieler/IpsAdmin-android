package com.dscorp.ispadmin.data.datasource.remote

import com.dscorp.ispadmin.domain.model.Payment
import retrofit2.http.Body
import retrofit2.http.PUT

/**
 * Core multi-receipt charge (PUT payment/batch): one transaction. The new Core takes the collector from the
 * session; the current production Core requires it in the body.
 */
interface ChargeApi {
    @PUT("payment/batch")
    suspend fun registerBatch(@Body batch: PaymentBatchRequest): List<Payment>
}

data class PaymentBatchRequest(
    val paymentIds: List<Int>,
    val method: String,
    /** Collector (session user); without it the current production Core fails with "User not found with id 0". */
    val responsibleId: Int,
    val discountAmount: Double = 0.0,
    val discountReason: String? = null,
    val electronicPayerName: String? = null,
)
