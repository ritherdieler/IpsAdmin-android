package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

fun registrationProgressStepMessage(progressMessage: String?): String =
    progressMessage?.takeIf { it.isNotBlank() } ?: "Registrando…"
