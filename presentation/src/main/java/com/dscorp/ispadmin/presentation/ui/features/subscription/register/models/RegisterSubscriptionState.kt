package com.dscorp.ispadmin.presentation.ui.features.subscription.register.models

import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.User

data class RegisterSubscriptionState(
    val isLoading: Boolean = false,
    val isRefreshingOnuList: Boolean = false,
    val isLoadingLocation: Boolean = false,
    val isLoadingNearbyNapBoxes: Boolean = false,
    val cachedNapBoxList: List<NapBoxResponse> = emptyList(),
    val cachedPlanList: List<PlanResponse> = emptyList(),
    val currentUser: User? = null,
    
    val registerSubscriptionForm: RegisterSubscriptionFormState = RegisterSubscriptionFormState(),
    val orderId: Int? = null
)
