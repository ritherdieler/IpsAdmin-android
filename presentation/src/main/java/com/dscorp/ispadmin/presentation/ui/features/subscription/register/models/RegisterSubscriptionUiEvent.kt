package com.dscorp.ispadmin.presentation.ui.features.subscription.register.models

import com.dscorp.ispadmin.domain.model.Subscription

sealed interface RegisterSubscriptionUiEvent {
    data class Error(val message: String) : RegisterSubscriptionUiEvent
    data class Success(val subscription: Subscription) : RegisterSubscriptionUiEvent
}
