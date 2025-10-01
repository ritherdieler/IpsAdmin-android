package com.dscorp.ispadmin.presentation.ui.features.subscription.register.models

import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.domain.model.User

data class RegisterSubscriptionState(
    val isLoading: Boolean = false,
    val isRefreshingOnuList: Boolean = false,
    val isLoadingLocation: Boolean = false,
    val isLoadingNearbyNapBoxes: Boolean = false,
    val error: String? = null,
    val registeredSubscription: Subscription? = null,
    
    // Estado de GPS y permisos (migrado desde Screen)
    val isGpsEnabled: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val shouldShowGpsDialog: Boolean = false,
    
    // Caché de datos (migrado desde variables privadas del ViewModel)
    val cachedNapBoxList: List<NapBoxResponse> = emptyList(),
    val cachedPlanList: List<PlanResponse> = emptyList(),
    val currentUser: User? = null,
    
    val registerSubscriptionForm: RegisterSubscriptionFormState = RegisterSubscriptionFormState(),
    val orderId: Int? = null
)
