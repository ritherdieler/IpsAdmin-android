package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub

import android.content.SharedPreferences
import com.dscorp.ispadmin.data.datasource.remote.ChargeApi
import com.dscorp.ispadmin.data.datasource.remote.PaymentBatchRequest
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
    override suspend fun registerCharge(batch: ChargeBatch): List<Payment> =
        requireNotNull(chargeApi) { "Charge API not wired" }.registerBatch(
            PaymentBatchRequest(batch.paymentIds, batch.method, batch.discountAmount, batch.discountReason, batch.electronicPayerName),
        )
    override fun lastMethod(): String? = preferences?.getString(LAST_METHOD_KEY, null)
    override fun rememberMethod(method: String) {
        preferences?.edit()?.putString(LAST_METHOD_KEY, method)?.apply()
    }
    override suspend fun restoreInternet(subscriptionId: Int): Result<Unit> = restoreInternetConnection(subscriptionId)
    override fun currentUser(): User? = repository.getUserSession()
}
