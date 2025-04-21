package com.dscorp.ispadmin.presentation.ui.features.plan.edit

import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.presentation.extension.formIsValid
import com.dscorp.ispadmin.presentation.ui.features.base.BaseViewModel
import com.dscorp.ispadmin.presentation.ui.features.base.BaseUiState
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.formvalidation.ReactiveFormField
import com.dscorp.ispadmin.domain.model.Plan
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.example.data2.data.repository.IRepository

class EditPlanViewModel(val repository: IRepository) : BaseViewModel<EditPlanUiState>() {

    private var planId: String? = null

    val planNameField = ReactiveFormField<String>(
        hintResourceId = R.string.plan_name,
        errorResourceId = R.string.invalidPlanName
    ) { !it.isNullOrEmpty() }

    val planPriceField = ReactiveFormField<String>(
        hintResourceId = R.string.plan_price,
        errorResourceId = R.string.invalidPrice
    ) { !it.isNullOrEmpty() }

    val planDownloadSpeedField = ReactiveFormField<String>(
        hintResourceId = R.string.download_speed_in_mb,
        errorResourceId = R.string.invalid_value
    ) { !it.isNullOrEmpty() }

    val planUploadSpeedField = ReactiveFormField<String>(
        hintResourceId = R.string.upload_speed_in_mb,
        errorResourceId = R.string.invalid_value
    ) { !it.isNullOrEmpty() }

    fun setInitialPlanData(plan: PlanResponse) {
        planId = plan.id
        planNameField.liveData.value = plan.name
        planPriceField.liveData.value = plan.price.toString()
        planUploadSpeedField.liveData.value = plan.uploadSpeed
        planDownloadSpeedField.liveData.value = plan.downloadSpeed
    }


    fun editPlan() = executeWithProgress {
        if (formIsValid()) {
            val plan = createPlan()
            val response = repository.updatePlan(plan)
            uiState.value = BaseUiState(uiState = EditPlanUiState.EditPlanUpdateSuccess(response))
        }
    }

    private fun createPlan() = Plan(
        id = planId,
        name = planNameField.getValue()!!,
        price = planPriceField.getValue()?.toDouble()!!,
        downloadSpeed = planDownloadSpeedField.getValue()!!,
        uploadSpeed = planUploadSpeedField.getValue()!!
    )

    private fun formIsValid() = listOf(
        planNameField,
        planPriceField,
        planDownloadSpeedField,
        planUploadSpeedField
    ).formIsValid()
}