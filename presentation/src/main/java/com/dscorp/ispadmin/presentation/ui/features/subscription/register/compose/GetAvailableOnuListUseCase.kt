package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.Onu

class GetAvailableOnuListUseCase(private val repository: IRepository) {

    suspend operator fun invoke(): Result<List<Onu>> {
        return try {
            val onuList = repository.getUnconfirmedOnus()
            Result.success(onuList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
