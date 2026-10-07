package com.dscorp.ispadmin.data.repository.adapters

import com.dscorp.ispadmin.data.datasource.remote.RestApiServices
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.CustomerDetail
import com.dscorp.ispadmin.domain.model.DniCheck
import com.dscorp.ispadmin.domain.repository.SubscriptionActionsRepository

class SubscriptionActionsRepositoryAdapter(
    private val repository: IRepository,
    private val restApiServices: RestApiServices? = null,
) : SubscriptionActionsRepository {

    override suspend fun reactivateService(subscriptionId: Int, notes: String?) {
        val user = repository.getUserSession()
        val responsibleId = user?.id ?: throw IllegalStateException("Usuario no encontrado")
        repository.reactivateService(subscriptionId, responsibleId, notes)
    }

    override suspend fun rebootFiberOnu(subscriptionId: Int) {
        repository.rebootFiberOnu(subscriptionId)
    }

    override suspend fun retryTr069Provisioning(subscriptionId: Int) =
        repository.retryTr069Provisioning(subscriptionId)

    override suspend fun getRegistrationProgress(subscriptionId: Int) =
        repository.getRegistrationProgress(subscriptionId)

    override suspend fun restoreInternetConnection(subscriptionId: Int, notes: String?) {
        val user = repository.getUserSession()
        val responsibleId = user?.id ?: throw IllegalStateException("Usuario no encontrado")
        repository.restoreInternetConnection(subscriptionId, responsibleId, notes)
    }

    override suspend fun checkDni(dni: String): DniCheck {
        val api = requireNotNull(restApiServices) { "DNI_CHECK_UNAVAILABLE" }
        val response = api.checkDni(dni)
        check(response.isSuccessful) { "DNI_CHECK_FAILED (HTTP ${response.code()})" }
        return response.body() ?: DniCheck()
    }

    override suspend fun getCustomer(customerId: Int): CustomerDetail {
        val api = requireNotNull(restApiServices) { "CUSTOMER_UNAVAILABLE" }
        val response = api.getCustomer(customerId)
        check(response.isSuccessful) { "CUSTOMER_FAILED (HTTP ${response.code()})" }
        return response.body() ?: throw IllegalStateException("CUSTOMER_EMPTY")
    }
}
