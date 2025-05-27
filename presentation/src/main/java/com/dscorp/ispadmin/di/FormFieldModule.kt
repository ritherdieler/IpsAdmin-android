package com.dscorp.ispadmin.di

import com.dscorp.ispadmin.presentation.ui.features.forms.subscription.EditSubscriptionDataForm
import com.dscorp.ispadmin.presentation.ui.features.forms.subscription.RegisterSubscriptionForm
import org.koin.dsl.module

val formFieldModule = module {
    // subscriptionFormField

    factory { EditSubscriptionDataForm() }
    factory { RegisterSubscriptionForm() }

}