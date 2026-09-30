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

fun registrationProgressStageLabel(value: String): String = when (value) {
    "VALIDATE" -> "Validación"
    "MIKROTIK" -> "Acceso del cliente"
    "OLT" -> "Autorización de la ONU"
    "OMCI" -> "WAN de gestión"
    "ACS_CONTACT" -> "Conexión con ACS"
    "INTERNET" -> "Internet PPPoE"
    "WIFI" -> "WiFi"
    "WAN_CLEANUP" -> "Limpieza de WAN"
    "VERIFY" -> "Verificación final"
    else -> value
}

fun registrationProgressStateLabel(value: String): String = when (value) {
    "PENDING" -> "Pendiente"
    "RUNNING" -> "En curso"
    "WAITING" -> "Esperando respuesta"
    "SUCCEEDED" -> "Completado"
    "FAILED" -> "Requiere atención"
    "COMPENSATED" -> "Revertido"
    "CANCEL_REQUESTED" -> "Cancelación solicitada"
    "CANCELLING" -> "Revirtiendo el registro"
    "CANCEL_FAILED" -> "Limpieza pendiente"
    "CANCELLED" -> "Registro cancelado"
    else -> value
}
