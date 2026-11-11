package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub

import android.content.SharedPreferences
import com.dscorp.ispadmin.data.datasource.remote.ChargeApi
import com.dscorp.ispadmin.data.datasource.remote.AppPendingProofDto
import com.dscorp.ispadmin.data.datasource.remote.AppProofChargeRequest
import com.dscorp.ispadmin.data.datasource.remote.AppProofDiscardRequest
import com.dscorp.ispadmin.data.datasource.remote.PaymentBatchRequest
import com.dscorp.ispadmin.data.datasource.remote.WhatsAppPendingProofDto
import com.dscorp.ispadmin.data.datasource.remote.WhatsAppProofDiscardRequest
import retrofit2.HttpException
import retrofit2.Response
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.Payment
import com.dscorp.ispadmin.domain.model.PaymentMethodOption
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.data.datasource.remote.FichaApi
import com.dscorp.ispadmin.domain.usecase.service.RebootFiberOnuUseCase
import com.dscorp.ispadmin.domain.usecase.service.RestoreInternetConnectionUseCase
import com.dscorp.ispadmin.domain.usecase.subscription.RetryTr069ProvisioningUseCase
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.CpeReading
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.mufaLabel

private const val LAST_METHOD_KEY = "charge_last_method"

/** Adapts the existing repository calls the hub uses; no new endpoints. */
class RepositorySubscriptionHubGateway(
    private val repository: IRepository,
    private val restoreInternetConnection: RestoreInternetConnectionUseCase,
    private val rebootFiberOnu: RebootFiberOnuUseCase? = null,
    private val retryTr069: RetryTr069ProvisioningUseCase? = null,
    private val networkApi: FichaApi? = null,
    private val chargeApi: ChargeApi? = null,
    private val preferences: SharedPreferences? = null,
    /** Core base URL (BuildConfig.BASE_URL) and the session token, to show the proof's capture. */
    private val imageBase: String? = null,
    private val accessToken: () -> String? = { null },
) : SubscriptionHubGateway {
    override suspend fun cpeReading(subscriptionId: Int): CpeReading =
        requireNotNull(networkApi) { "Network API not wired" }.cpeStatus(subscriptionId).toReading()
    override suspend fun tickets(subscriptionId: Int) =
        requireNotNull(networkApi) { "Ficha API not wired" }.ticketsBySubscription(subscriptionId)
    override suspend fun history(subscriptionId: Int) =
        requireNotNull(networkApi) { "Ficha API not wired" }.history(subscriptionId)
    override suspend fun mufaName(mufaId: Int): String? =
        repository.getMufas().firstOrNull { it.id == mufaId }?.let(::mufaLabel)
    override suspend fun rebootOnu(subscriptionId: Int): Result<Unit> =
        rebootFiberOnu?.invoke(subscriptionId) ?: Result.failure(UnsupportedOperationException())
    override suspend fun resendConfig(subscriptionId: Int): Result<Unit> =
        retryTr069?.invoke(subscriptionId)?.map { } ?: Result.failure(UnsupportedOperationException())

    override suspend fun subscription(subscriptionId: Int): SubscriptionResponse = repository.subscriptionById(subscriptionId)
    override suspend fun recentPayments(subscriptionId: Int, limit: Int): List<Payment> =
        repository.getRecentPaymentsHistory(subscriptionId, limit)
    override suspend fun paymentMethods(): List<PaymentMethodOption> = repository.getPaymentMethods()
    override suspend fun registerCharge(batch: ChargeBatch): List<Payment> = coreCall {
        requireNotNull(chargeApi) { "Charge API not wired" }.registerBatch(
            PaymentBatchRequest(
                paymentIds = batch.paymentIds,
                method = batch.method,
                responsibleId = batch.responsibleId,
                discountAmount = batch.discountAmount,
                discountReason = batch.discountReason,
                electronicPayerName = batch.electronicPayerName,
                inboundMessageId = batch.inboundMessageId,
                amountReceived = batch.amountReceived,
            ),
        )
    }

    override suspend fun pendingProofs(subscriptionId: Int): List<PendingProof> {
        val api = chargeApi ?: return emptyList()
        val response = coreCall { api.pendingProofs(subscriptionId) }
        val today = java.time.LocalDate.now(com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.FICHA_ZONE)
        val authorization = accessToken()?.takeIf { it.isNotBlank() }?.let { "Bearer $it" }
        fun image(path: String?): String? = path?.takeIf { imageBase != null }?.let { imageBase!!.trimEnd('/') + "/" + it.trimStart('/') }
        return response.app.orEmpty().map { it.toPending(subscriptionId, today).copy(imageUrl = image(it.imageUrl), imageAuthorization = authorization) } +
            response.whatsApp.orEmpty().map { it.toPending(today).copy(imageUrl = image(it.mediaPath), imageAuthorization = authorization) }
    }

    override suspend fun chargeWithAppProof(proofId: String, paymentIds: List<Int>, payerName: String?, discount: Double, reason: String?) {
        val api = requireNotNull(chargeApi) { "Charge API not wired" }
        coreCall { api.chargeWithAppProof(proofId, AppProofChargeRequest(paymentIds, payerName, discount, reason)).orThrow() }
    }

    override suspend fun discardProof(proof: PendingProof, reason: DiscardReason) {
        val api = requireNotNull(chargeApi) { "Charge API not wired" }
        coreCall {
            when {
                proof.appProofId != null -> api.discardAppProof(proof.appProofId, AppProofDiscardRequest(reason.app, reason.appText)).orThrow()
                proof.inboundMessageId != null ->
                    api.discardWhatsAppProof(proof.inboundMessageId, WhatsAppProofDiscardRequest(reason.whatsApp)).orThrow()
                else -> Unit
            }
        }
    }
    override fun lastMethod(): String? = preferences?.getString(LAST_METHOD_KEY, null)
    override fun rememberMethod(method: String) {
        preferences?.edit()?.putString(LAST_METHOD_KEY, method)?.apply()
    }
    override suspend fun restoreInternet(subscriptionId: Int): Result<Unit> = restoreInternetConnection(subscriptionId)
    override fun currentUser(): User? = repository.getUserSession()
}

private fun Response<Unit>.orThrow() {
    if (!isSuccessful) throw HttpException(this)
}

/** Turns a Core error into [CoreRejection] with the message staff should read (Core's «message»). */
private suspend fun <T> coreCall(call: suspend () -> T): T = try {
    call()
} catch (error: HttpException) {
    val body = runCatching { error.response()?.errorBody()?.string() }.getOrNull()
    val message = body?.let {
        runCatching { com.google.gson.JsonParser.parseString(it).asJsonObject.get("message")?.asString }.getOrNull()
    }?.takeIf { it.isNotBlank() }
    if (message != null) throw CoreRejection(error.code(), message) else throw error
}

private fun AppPendingProofDto.toPending(subscriptionId: Int, today: java.time.LocalDate) = PendingProof(
    appProofId = id,
    provider = provider,
    amount = amountRead,
    operation = operationNumber,
    received = receivedLabel(sentAt, today),
    readConfirmed = reading == "PASSED",
    paymentIds = items.orEmpty().filter { it.subscriptionId == subscriptionId }.map { it.paymentId },
    otherServiceFees = items.orEmpty().filter { it.subscriptionId != subscriptionId }.map {
        PendingProofFee(it.paymentId, it.place ?: "#${it.subscriptionId}", it.period, it.amount)
    },
)

private fun WhatsAppPendingProofDto.toPending(today: java.time.LocalDate) = PendingProof(
    inboundMessageId = inboundMessageId,
    provider = provider,
    amount = amount,
    operation = operationNumber,
    received = receivedLabel(receivedAt, today),
    payerName = payerName?.takeIf { it.isNotBlank() },
    sentCount = sentCount,
)
