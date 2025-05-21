package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import com.dscorp.ispadmin.domain.model.PlanResponse
import com.example.data2.data.repository.IRepository

class GetPlanListUseCase(private val repository: IRepository) {

    suspend operator fun invoke(): Result<List<PlanResponse>> = runCatching {
        val planList = repository.getPlans()
        planList
    }
}
