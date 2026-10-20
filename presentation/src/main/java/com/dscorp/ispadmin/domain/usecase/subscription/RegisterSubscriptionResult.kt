package com.dscorp.ispadmin.domain.usecase.subscription

import com.dscorp.ispadmin.domain.model.Subscription

sealed class RegisterSubscriptionResult {
    data class Registered(val subscription: Subscription) : RegisterSubscriptionResult()
}
