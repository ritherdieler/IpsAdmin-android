package com.dscorp.ispadmin.domain.usecase

import com.dscorp.ispadmin.data.repository.IRepository

class RebootFiberOnuUseCase(
    private val repository: IRepository
) {
    suspend operator fun invoke(subscriptionId: Int): Result<Unit> = runCatching {
        repository.rebootFiberOnu(subscriptionId)
    }
}
