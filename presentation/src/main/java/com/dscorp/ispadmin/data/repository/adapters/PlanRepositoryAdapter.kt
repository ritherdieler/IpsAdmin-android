package com.dscorp.ispadmin.data.repository.adapters

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.Plan
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.repository.PlanRepository

class PlanRepositoryAdapter(
    private val repository: IRepository
) : PlanRepository {

    override suspend fun getPlans(): List<PlanResponse> = repository.getPlans()

    override suspend fun updatePlan(plan: Plan): PlanResponse = repository.updatePlan(plan)
}
