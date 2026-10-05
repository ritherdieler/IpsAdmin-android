package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import com.dscorp.ispadmin.domain.model.AccessMode
import com.dscorp.ispadmin.domain.model.RegistrationProgressCheckpoint

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

fun registrationProgressStepMessage(
    progressMessage: String?,
    checkpoints: List<RegistrationProgressCheckpoint> = emptyList(),
    accessMode: AccessMode = AccessMode.PPPOE_DYNAMIC,
): String {
    val active = checkpoints.firstOrNull { it.state == "RUNNING" || it.state == "WAITING" }
        ?: return progressMessage?.takeIf { it.isNotBlank() } ?: "Registrando…"
    val stageLabel = registrationProgressStageLabel(active.stage, accessMode)
    return when (active.state) {
        "RUNNING" -> when (active.stage) {
            "VALIDATE" -> "Validando registro…"
            "MIKROTIK" -> "Configurando acceso del cliente…"
            "OLT" -> "Autorizando la ONU…"
            "ACS_CONTACT" -> "Conectando con ACS…"
            "INTERNET" -> "Configurando Internet ${accessMode.registerLabel()}…"
            "WIFI" -> "Aplicando WiFi…"
            "WAN_CLEANUP" -> "Limpiando WAN…"
            "VERIFY" -> "Verificando aprovisionamiento…"
            else -> "Aprovisionando registro…"
        }
        "WAITING" -> "Esperando respuesta: $stageLabel…"
        else -> "Aprovisionando registro…"
    }
}

fun registrationProgressStageLabel(
    value: String,
    accessMode: AccessMode = AccessMode.PPPOE_DYNAMIC,
): String = when (value) {
    "VALIDATE" -> "Validación"
    "MIKROTIK" -> "Acceso del cliente"
    "OLT" -> "Autorización de la ONU"
    "ACS_CONTACT" -> "Conexión con ACS"
    "INTERNET" -> "Internet ${accessMode.registerLabel()}"
    "WIFI" -> "WiFi"
    "WAN_CLEANUP" -> "Limpieza de WAN"
    "VERIFY" -> "Verificación final"
    else -> "Etapa desconocida"
}

fun registrationProgressStateLabel(value: String): String = when (value) {
    "PENDING" -> "Pendiente"
    "RUNNING" -> "En curso"
    "WAITING" -> "Esperando respuesta"
    "SUCCEEDED" -> "Completado"
    "FAILED" -> "Requiere atención"
    "COMPENSATED" -> "Revertido"
    else -> "Estado desconocido"
}
