package com.dscorp.ispadmin.presentation.ui.features.subscription.register.models

import com.dscorp.ispadmin.domain.model.Subscription

sealed interface SubmissionState {
    data object Idle : SubmissionState
    data object Submitting : SubmissionState
    data object Reconciling : SubmissionState
    data class Uncertain(val message: String) : SubmissionState
    data class Provisioning(val subscriptionId: Int) : SubmissionState
    data class Completed(val subscription: Subscription) : SubmissionState
    data class NeedsAttention(val subscription: Subscription, val message: String) : SubmissionState
    data class ProvisioningUnknown(val subscription: Subscription, val message: String) : SubmissionState
}

fun SubmissionState.resultSubscription(): Subscription? = when (this) {
    is SubmissionState.Completed -> subscription
    is SubmissionState.NeedsAttention -> subscription
    is SubmissionState.ProvisioningUnknown -> subscription
    else -> null
}

fun SubmissionState.resultNotice(): String? = when (this) {
    is SubmissionState.NeedsAttention -> message
    is SubmissionState.ProvisioningUnknown -> message
    else -> null
}
