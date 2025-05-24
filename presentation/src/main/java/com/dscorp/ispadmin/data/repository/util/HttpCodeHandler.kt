package com.dscorp.ispadmin.data.repository.util

import retrofit2.Response

 fun <T> handleResponse(response: Response<T>, resourceName: String): T {
    val code = response.code()
    return when {
        // 2xx - Éxito
        code in 200..299 -> response.body()
            ?: throw Exception("Respuesta vacía de $resourceName")

        // 3xx - Redirección (normalmente no deberían ocurrir)
        code in 300..399 -> throw Exception("$resourceName - Redirección no manejada: $code")

        // 4xx - Errores de cliente
        code == 400 -> throw Exception("$resourceName - Petición incorrecta")
        code == 401 -> throw Exception("$resourceName - No autorizado")
        code == 403 -> throw Exception("$resourceName - Prohibido")
        code == 404 -> throw Exception("$resourceName - No encontrado")
        code == 409 -> throw Exception("$resourceName - Conflicto con el estado actual")
        code in 400..499 -> throw Exception("$resourceName - Error de cliente: $code")

        // 5xx - Errores de servidor
        code in 500..599 -> throw Exception("$resourceName - Error de servidor: $code")

        // Otros códigos no estándar
        else -> throw Exception("$resourceName - Código de respuesta inesperado: $code")
    }
}