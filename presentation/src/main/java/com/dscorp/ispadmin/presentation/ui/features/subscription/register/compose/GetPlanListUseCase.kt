package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.PlanResponse

class GetPlanListUseCase(private val repository: IRepository) {

    suspend operator fun invoke(): Result<List<PlanResponse>> = runCatching {
        val planList = repository.getPlans()
        planList
    }
}
