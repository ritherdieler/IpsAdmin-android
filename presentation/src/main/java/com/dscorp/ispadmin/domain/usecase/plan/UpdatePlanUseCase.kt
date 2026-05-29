package com.dscorp.ispadmin.domain.usecase.plan

import com.dscorp.ispadmin.domain.model.Plan
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.repository.PlanRepository

class UpdatePlanUseCase(private val planRepository: PlanRepository) {
    suspend operator fun invoke(plan: Plan): Result<PlanResponse> = runCatching {
        planRepository.updatePlan(plan)
    }
}
