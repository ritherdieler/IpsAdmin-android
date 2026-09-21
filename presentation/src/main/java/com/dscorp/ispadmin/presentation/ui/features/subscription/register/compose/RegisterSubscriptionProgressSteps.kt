package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

enum class RegisterScreenBusyMode {
    NONE,
    CATALOG,
    REGISTERING,
}

fun registerScreenBusyMode(isLoading: Boolean, isRegistering: Boolean): RegisterScreenBusyMode =
    when {
        isRegistering -> RegisterScreenBusyMode.REGISTERING
        isLoading -> RegisterScreenBusyMode.CATALOG
        else -> RegisterScreenBusyMode.NONE
    }

fun registrationProgressStepMessage(progressMessage: String?): String =
    progressMessage?.takeIf { it.isNotBlank() } ?: "Registrando…"
