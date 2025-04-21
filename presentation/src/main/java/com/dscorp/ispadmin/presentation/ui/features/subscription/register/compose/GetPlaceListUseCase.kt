package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import com.dscorp.ispadmin.domain.model.PlaceResponse
import com.example.data2.data.repository.IRepository

class GetPlaceListUseCase(
    private val repository: IRepository
) {
    suspend operator fun invoke(): Result<List<PlaceResponse>> {
        return try {
            val planList = repository.getPlaces()
            Result.success(planList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}