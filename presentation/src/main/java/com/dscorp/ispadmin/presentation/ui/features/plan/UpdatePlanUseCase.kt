package com.dscorp.ispadmin.presentation.ui.features.plan

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.Plan
import com.dscorp.ispadmin.domain.model.PlanResponse

class UpdatePlanUseCase(private val repository: IRepository) {
    suspend operator fun invoke(plan: Plan): Result<PlanResponse> = runCatching {
        repository.updatePlan(plan)
    }
} 