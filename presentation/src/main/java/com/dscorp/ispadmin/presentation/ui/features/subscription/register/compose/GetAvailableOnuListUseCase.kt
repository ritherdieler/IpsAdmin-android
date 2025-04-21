package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import com.dscorp.ispadmin.domain.model.Onu
import com.example.data2.data.repository.IRepository

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
