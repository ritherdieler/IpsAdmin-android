package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.Payment
import com.dscorp.ispadmin.domain.model.PaymentMethodOption
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.usecase.service.RestoreInternetConnectionUseCase

/** Adapts the existing repository calls the hub uses; no new endpoints. */
class RepositorySubscriptionHubGateway(
    private val repository: IRepository,
    private val restoreInternetConnection: RestoreInternetConnectionUseCase,
) : SubscriptionHubGateway {
    override suspend fun subscription(subscriptionId: Int): SubscriptionResponse = repository.subscriptionById(subscriptionId)
    override suspend fun recentPayments(subscriptionId: Int, limit: Int): List<Payment> =
        repository.getRecentPaymentsHistory(subscriptionId, limit)
    override suspend fun paymentMethods(): List<PaymentMethodOption> = repository.getPaymentMethods()
    override suspend fun registerPayment(payment: Payment): Payment = repository.registerPayment(payment)
    override suspend fun restoreInternet(subscriptionId: Int): Result<Unit> = restoreInternetConnection(subscriptionId)
    override fun currentUser(): User? = repository.getUserSession()
}
