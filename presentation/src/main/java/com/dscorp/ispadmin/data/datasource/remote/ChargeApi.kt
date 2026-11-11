package com.dscorp.ispadmin.data.datasource.remote

import com.dscorp.ispadmin.domain.model.Payment
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Core multi-receipt charge (PUT payment/batch): one transaction. The new Core takes the collector from the
 * session; the current production Core requires it in the body.
 */
interface ChargeApi {
    @PUT("payment/batch")
    suspend fun registerBatch(@Body batch: PaymentBatchRequest): List<Payment>

    /** Proofs the charge has to use or discard first: WhatsApp receipts and Mi GigaFiber proofs in review. */
    @GET("payment/pending-proofs")
    suspend fun pendingProofs(@Query("subscriptionId") subscriptionId: Int): PendingProofsResponse

    /** The charge with a Mi GigaFiber proof: Core approves it for these fees, with the discount. */
    @POST("payment/pending-proofs/app/{id}/charge")
    suspend fun chargeWithAppProof(@Path("id") proofId: String, @Body body: AppProofChargeRequest): Response<Unit>

    @POST("payment/pending-proofs/app/{id}/rejection")
    suspend fun discardAppProof(@Path("id") proofId: String, @Body body: AppProofDiscardRequest): Response<Unit>

    @POST("payment/pending-proofs/whatsapp/{id}/rejection")
    suspend fun discardWhatsAppProof(@Path("id") inboundMessageId: Int, @Body body: WhatsAppProofDiscardRequest): Response<Unit>
}

data class PendingProofsResponse(
    val subscriptionId: Int = 0,
    val whatsApp: List<WhatsAppPendingProofDto>? = null,
    val app: List<AppPendingProofDto>? = null,
)

data class WhatsAppPendingProofDto(
    val inboundMessageId: Int,
    val receivedAt: String? = null,
    val provider: String? = null,
    val amount: Double? = null,
    val operationNumber: String? = null,
    val payerName: String? = null,
    val sentCount: Int = 1,
    /** Core path of the image or PDF, e.g. /whatsapp/inbound-messages/900/media. */
    val mediaPath: String? = null,
)

data class AppPendingProofDto(
    val id: String,
    val provider: String? = null,
    val amountRead: Double? = null,
    val operationNumber: String? = null,
    val sentAt: String? = null,
    /** PASSED, REVIEW_REQUIRED or NOT_VOUCHER. */
    val reading: String? = null,
    val subscriptionId: Int = 0,
    /** Core path of the capture, e.g. /customer-proofs/p_55/image. */
    val imageUrl: String? = null,
    val items: List<AppPendingProofItemDto>? = null,
)

data class AppPendingProofItemDto(
    val paymentId: Int,
    val subscriptionId: Int,
    val place: String? = null,
    val period: String? = null,
    val amount: Double = 0.0,
)

data class AppProofChargeRequest(
    val paymentIds: List<Int>,
    val payerName: String? = null,
    val discountAmount: Double = 0.0,
    val discountReason: String? = null,
)

/** [code]: NOT_FOUND, AMOUNT_MISMATCH, ALREADY_USED, ILLEGIBLE, MISREAD or OTHER (OTHER needs [reason]). */
data class AppProofDiscardRequest(val code: String, val reason: String = "")

/** [reason]: ILLEGIBLE, MISREAD, NOT_FOUND, DUPLICATE, NOT_VOUCHER or OTHER. */
data class WhatsAppProofDiscardRequest(val reason: String)

data class PaymentBatchRequest(
    val paymentIds: List<Int>,
    val method: String,
    /** Collector (session user); without it the current production Core fails with "User not found with id 0". */
    val responsibleId: Int,
    val discountAmount: Double = 0.0,
    val discountReason: String? = null,
    val electronicPayerName: String? = null,
    /** The pending WhatsApp receipt this charge uses. */
    val inboundMessageId: Int? = null,
    /** What the receipt says was paid; anything above the total stays as the customer's credit. */
    val amountReceived: Double? = null,
)
