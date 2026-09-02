package com.dscorp.ispadmin.data.repository.adapters

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.repository.SubscriptionActionsRepository

class SubscriptionActionsRepositoryAdapter(
    private val repository: IRepository
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
}
