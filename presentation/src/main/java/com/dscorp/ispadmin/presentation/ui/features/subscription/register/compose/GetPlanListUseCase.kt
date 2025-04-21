package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import com.dscorp.ispadmin.domain.model.PlanResponse
import com.example.data2.data.repository.IRepository

class GetPlanListUseCase(private val repository: IRepository) {

    suspend operator fun invoke(): Result<List<PlanResponse>> {
        return try {
            val planList = repository.getPlans()
            Result.success(planList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
