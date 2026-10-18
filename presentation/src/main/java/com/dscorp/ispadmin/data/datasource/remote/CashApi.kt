package com.dscorp.ispadmin.data.datasource.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Core cash of the day (GET cash/day) for collection roles. */
interface CashApi {
    @GET("cash/day")
    suspend fun day(@Query("date") date: String?): CashDayResponse

    @GET("cash/payments")
    suspend fun payments(@Query("q") query: String?, @Query("method") method: String?, @Query("days") days: Int): CashSearchResponse

    @GET("cash/payments/{id}")
    suspend fun payment(@Path("id") receiptId: Int): CashPaymentResponse
}

data class CashSearchResponse(val from: String = "", val to: String = "", val receipts: List<CashReceiptResponse> = emptyList())

data class CashPaymentResponse(
    val id: Int = 0,
    val subscriptionId: Int? = null,
    val subscriber: String? = null,
    val amount: Double = 0.0,
    val method: String? = null,
    val payerName: String? = null,
    val collector: String? = null,
    val paidAt: String = "",
    val serviceMonth: String? = null,
    val dueDate: String? = null,
    val discountAmount: Double? = null,
    val discountReason: String? = null,
    val hasProof: Boolean = false,
)

data class CashMethodResponse(val label: String = "", val amount: Double = 0.0, val count: Long = 0)

data class CashCollectorResponse(val name: String = "", val amount: Double = 0.0, val count: Long = 0)

data class CashReceiptResponse(
    val id: Int = 0,
    val subscriptionId: Int? = null,
    val subscriber: String? = null,
    val amount: Double = 0.0,
    val method: String? = null,
    val payerName: String? = null,
    val collector: String? = null,
    val paidAt: String = "",
    val serviceMonth: String? = null,
)

data class CashDayResponse(
    val date: String = "",
    val total: MoneyCountResponse = MoneyCountResponse(),
    val byMethod: List<CashMethodResponse> = emptyList(),
    val byCollector: List<CashCollectorResponse> = emptyList(),
    val receipts: List<CashReceiptResponse> = emptyList(),
)
