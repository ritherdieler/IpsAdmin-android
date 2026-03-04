package com.dscorp.ispadmin.domain.usecase

import com.dscorp.ispadmin.data.repository.IRepository

class ReactivateServiceUseCase(
    private val repository: IRepository
) {
    suspend operator fun invoke(
        subscriptionId: Int,
        notes: String? = null
    ): Result<Unit> = runCatching {
        val user = repository.getUserSession()
        val responsibleId = user?.id ?: throw IllegalStateException("Usuario no encontrado")
        repository.reactivateService(subscriptionId, responsibleId, notes)
    }
}
