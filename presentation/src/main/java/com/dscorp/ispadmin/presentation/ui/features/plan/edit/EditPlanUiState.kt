package com.dscorp.ispadmin.presentation.ui.features.plan.edit

import com.dscorp.ispadmin.domain.model.PlanResponse

sealed class EditPlanUiState {
    class EditPlanUpdateSuccess(val plan: PlanResponse):EditPlanUiState()
}