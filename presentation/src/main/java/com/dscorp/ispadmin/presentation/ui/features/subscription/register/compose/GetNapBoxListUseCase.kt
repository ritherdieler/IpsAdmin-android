package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.NapBoxResponse

class GetNapBoxListUseCase(private val repository: IRepository) {
    suspend operator fun invoke(): Result<List<NapBoxResponse>> {
        return try {
            val napBoxList = repository.getNapBoxes()
            Result.success(napBoxList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}