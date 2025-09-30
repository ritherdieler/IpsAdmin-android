package com.dscorp.ispadmin.data.response

/**
 * Respuesta del endpoint de reactivación de servicio
 */
data class ReactivateServiceResponse(
    val message: String,
    val subscriptionId: Int
)
