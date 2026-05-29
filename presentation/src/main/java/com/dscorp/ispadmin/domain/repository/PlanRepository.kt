package com.dscorp.ispadmin.domain.repository

import com.dscorp.ispadmin.domain.model.Plan
import com.dscorp.ispadmin.domain.model.PlanResponse

interface PlanRepository {
    suspend fun getPlans(): List<PlanResponse>

    suspend fun updatePlan(plan: Plan): PlanResponse
}
