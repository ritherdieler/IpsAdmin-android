package com.dscorp.ispadmin.presentation.ui.features.subscription.register.models

import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.model.OnuRegistrationOperation
import com.dscorp.ispadmin.domain.model.RegistrationProgressCheckpoint

data class RegisterSubscriptionState(
    val isLoading: Boolean = false,
    val isPreauthorizationRequestInProgress: Boolean = false,
    val isRegistering: Boolean = false,
    val isRefreshingOnuList: Boolean = false,
    val isLoadingLocation: Boolean = false,
    val isLoadingNearbyNapBoxes: Boolean = false,
    val cachedNapBoxList: List<NapBoxResponse> = emptyList(),
    val cachedPlanList: List<PlanResponse> = emptyList(),
    val currentUser: User? = null,
    
    val registerSubscriptionForm: RegisterSubscriptionFormState = RegisterSubscriptionFormState(),
    val orderId: Int? = null,
    val customerId: Int? = null,
    val identityLocked: Boolean = false,
    val customerServiceNumber: Int? = null,
    val tr069RetryLoading: Boolean = false,
    val showManualLocationMap: Boolean = false,
    val wizardStep: RegisterSubscriptionWizardStep = RegisterSubscriptionWizardStep.CLIENT_LOCATION,
    val preauthorizationEnabled: Boolean = false,
    val preauthorizationOperation: OnuRegistrationOperation? = null,
    val preauthorizationError: String? = null,
    val serviceTypeChosen: Boolean = false,
    val serviceTypeError: String? = null,
    val showCancelConfirmation: Boolean = false,
    val cancellationInProgress: Boolean = false,
    val registrationCancelled: Boolean = false,
    val registrationProgressMessage: String = "Registrando…",
    val registrationProgressCheckpoints: List<RegistrationProgressCheckpoint> = emptyList(),
    val submission: SubmissionState = SubmissionState.Idle,
    val dniWarning: String? = null,
    val submitBlockedReason: String? = null,
) {
    /** True for the fiber path (ONU authorized first) or while an operation is active; false for solo TV and wireless. */
    val usesPreauthorization: Boolean
        get() = preauthorizationEnabled && (
            preauthorizationOperation != null ||
                (serviceTypeChosen && registerSubscriptionForm.installationType == InstallationType.FIBER)
            )

    /** The ONU is authorized on the OLT but ACS has not confirmed it yet; the form stays usable. */
    val acsPending: Boolean
        get() = preauthorizationOperation?.let { it.phase == "WAITING_FOR_ACS" && it.state == "WAITING" } == true &&
            !registrationCancelled && !cancellationInProgress
}
