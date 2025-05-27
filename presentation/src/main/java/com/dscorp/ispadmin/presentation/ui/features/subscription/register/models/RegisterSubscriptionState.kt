package com.dscorp.ispadmin.presentation.ui.features.subscription.register.models

import com.dscorp.ispadmin.domain.model.Subscription

data class RegisterSubscriptionState(
    val isLoading: Boolean = false,
    val isRefreshingOnuList: Boolean = false,  // Nueva propiedad
    val error: String? = null,
    val registeredSubscription: Subscription? = null,
    val registerSubscriptionForm: RegisterSubscriptionFormState = RegisterSubscriptionFormState(

        //        firstName = "Sergio",
//        lastName = "carrillo",
//        dni = "12345678",
//        address = "Av. Siempre Viva 123",
//        phone = "123456789",
    ),
    val orderId: Int?=null
)
