package com.dscorp.ispadmin.navigation

import com.dscorp.ispadmin.domain.model.OnuRegistrationOperation
import com.dscorp.ispadmin.domain.model.User

private val SUBSCRIPTION_REGISTRAR_TYPES = setOf(
    User.UserType.ADMIN,
    User.UserType.ACCOUNTANT,
    User.UserType.TECHNICIAN,
)

internal fun User?.canRegisterSubscription(): Boolean = this?.type in SUBSCRIPTION_REGISTRAR_TYPES

// Launch redirect: reopen the register form only to resume an unfinished ONU registration.
internal fun shouldResumeOnuRegistration(user: User?, activeOperation: OnuRegistrationOperation?): Boolean =
    user.canRegisterSubscription() && activeOperation != null && activeOperation.subscriptionId == null
