package com.dscorp.ispadmin.domain.usecase.plan

import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.repository.PlanRepository

class GetPlanListUseCase(private val planRepository: PlanRepository) {

    suspend operator fun invoke(): Result<List<PlanResponse>> = runCatching {
        planRepository.getPlans()
    }
}
