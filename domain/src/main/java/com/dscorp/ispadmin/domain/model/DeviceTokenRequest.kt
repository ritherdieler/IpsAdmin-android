package com.dscorp.ispadmin.domain.model

/**
 * Modelo para enviar el token del dispositivo al servidor
 */
data class DeviceTokenRequest(
    val userId: Int,
    val deviceToken: String
)