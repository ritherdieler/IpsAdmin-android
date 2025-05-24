package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.Place

class GetPlaceListUseCase(
    private val repository: IRepository
) {
    suspend operator fun invoke(): Result<List<Place>> {
        return try {
            val planList = repository.getPlaces()
            Result.success(planList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}