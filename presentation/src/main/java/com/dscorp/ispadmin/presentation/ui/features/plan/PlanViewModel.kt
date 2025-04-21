package com.dscorp.ispadmin.presentation.ui.features.plan

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.domain.model.Plan
import com.example.data2.data.repository.IRepository
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent

class PlanViewModel : ViewModel() {
    private val repository: IRepository by KoinJavaComponent.inject(IRepository::class.java)

    val planResponseLiveData = MutableLiveData<PlanResponse>()
    val formErrorLiveData = MutableLiveData<PlanFormError>()
    val errorCleanFormLiveData = MutableLiveData<PlanErrorCleanForm>()
    fun registerPlan(plan: Plan) = viewModelScope.launch {

        try {
            if (formIsValid(plan)) {
                val planFromRepository = repository.registerPlan(plan)
                planResponseLiveData.postValue(PlanResponse.OnPlanRegistered(planFromRepository))
            }
        } catch (error: Exception) {
            planResponseLiveData.postValue(PlanResponse.OnError(error))
        }
    }

    private fun formIsValid(plan: Plan): Boolean {
        if (plan.name.isEmpty()) {
            formErrorLiveData.value = PlanFormError.OnEtNamePlanError()
            return false
        } else {
            errorCleanFormLiveData.value = PlanErrorCleanForm.OnEtNamePlanHasNotError
        }
        if (plan.price == 0.0) {
            formErrorLiveData.value = PlanFormError.OnEtPriceError()
            return false
        } else {
            errorCleanFormLiveData.value = PlanErrorCleanForm.OnEtPriceHasNotError
        }

        if (plan.downloadSpeed.isEmpty()) {
            formErrorLiveData.value = PlanFormError.OnEtDowloadSpeedError()
            return false
        } else {
            errorCleanFormLiveData.value = PlanErrorCleanForm.OnEtDownloadSpeedHasNotError
        }

        if (plan.uploadSpeed.isEmpty()) {
            formErrorLiveData.value = PlanFormError.OnEtUploadSpeedError()
            return false
        } else {
            errorCleanFormLiveData.value = PlanErrorCleanForm.OnEtUploadSpeedHasNotError
        }
        return true
    }
}
