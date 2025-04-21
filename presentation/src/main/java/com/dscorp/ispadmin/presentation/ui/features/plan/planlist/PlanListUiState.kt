package com.dscorp.ispadmin.presentation.ui.features.plan.planlist

import com.dscorp.ispadmin.domain.model.PlanResponse

sealed class PlanListUiState {
    class OnPlanListFound(val planList: List<PlanResponse>) : PlanListUiState()
}