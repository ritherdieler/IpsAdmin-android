package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.presentation.ui.features.base.BaseUiState
import com.dscorp.ispadmin.presentation.ui.features.base.BaseViewModel
import com.dscorp.ispadmin.presentation.ui.features.forms.subscription.EditSubscriptionDataForm
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class SubscriptionDetailViewModel(
    val editSubscriptionForm: EditSubscriptionDataForm,
    val repository: IRepository,
) :
    BaseViewModel<SubscriptionDetailUiState>() {

    var isEditingForm = false
    val editingIcon = MutableLiveData(R.drawable.baseline_edit_24)

    val places = MutableLiveData<List<PlanResponse>>()
    fun initForm(subscriptionId: Int) = viewModelScope.launch {
        val jobSubscription = async { repository.subscriptionById(subscriptionId) }
        val jobPlaces = async { repository.getPlans() }
        val subscription = jobSubscription.await()
        val response = jobPlaces.await()
        places.value = response
        editSubscriptionForm.initForm(subscription)
    }

    fun makeFieldsEditable() {
        if (!isEditingForm) {
            isEditingForm = true
            editingIcon.value = R.drawable.baseline_check_24
            editSubscriptionForm.changeEditableStatus(true)
        } else {
            updateSubscriptionData()
        }
    }

    private fun updateSubscriptionData() {
        executeWithProgress {
            editSubscriptionForm.getUpdateSubscriptionBody()?.let {
                repository.updateSubscriptionData(it)
                uiState.value = BaseUiState(SubscriptionDetailUiState.SubscriptionUpdated)
            }

        }
    }
}

sealed class SubscriptionDetailUiState {
    object SubscriptionUpdated : SubscriptionDetailUiState()

}
