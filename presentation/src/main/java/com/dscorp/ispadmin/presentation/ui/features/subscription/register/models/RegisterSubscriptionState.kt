package com.dscorp.ispadmin.presentation.ui.features.subscription.register.models

import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.model.OnuRegistrationOperation
import com.dscorp.ispadmin.domain.model.RegistrationProgressCheckpoint

data class RegisterSubscriptionState(
    val isLoading: Boolean = false,
    val isRegistering: Boolean = false,
    val isRefreshingOnuList: Boolean = false,
    val isLoadingLocation: Boolean = false,
    val isLoadingNearbyNapBoxes: Boolean = false,
    val cachedNapBoxList: List<NapBoxResponse> = emptyList(),
    val cachedPlanList: List<PlanResponse> = emptyList(),
    val currentUser: User? = null,
    
    val registerSubscriptionForm: RegisterSubscriptionFormState = RegisterSubscriptionFormState(),
    val orderId: Int? = null,
    val isOfflineMode: Boolean = false,
    val tr069RetryLoading: Boolean = false,
    val showManualLocationMap: Boolean = false,
    val wizardStep: RegisterSubscriptionWizardStep = RegisterSubscriptionWizardStep.CLIENT_LOCATION,
    val preauthorizationEnabled: Boolean = false,
    val preauthorizationOperation: OnuRegistrationOperation? = null,
    val preauthorizationError: String? = null,
    val showCancelConfirmation: Boolean = false,
    val cancellationInProgress: Boolean = false,
    val registrationCancelled: Boolean = false,
    val registrationProgressMessage: String = "Registrando…",
    val registrationProgressCheckpoints: List<RegistrationProgressCheckpoint> = emptyList(),
)
