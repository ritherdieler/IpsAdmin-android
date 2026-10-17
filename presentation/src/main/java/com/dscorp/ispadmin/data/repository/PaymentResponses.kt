package com.dscorp.ispadmin.data.repository

import com.dscorp.ispadmin.data.utils.ApiErrorBodyParser
import com.dscorp.ispadmin.domain.model.Payment
import retrofit2.Response

/** Turns Core payment responses into a result or an exception carrying the Core message. */
object PaymentResponses {
    private const val REGISTER_FALLBACK = "Error al registrar el pago"

    fun registeredOrThrow(response: Response<Payment>): Payment {
        val body = response.body()
        if (response.isSuccessful && body != null) return body
        throw Exception(ApiErrorBodyParser.parse(response.errorBody()?.string(), REGISTER_FALLBACK))
    }
}
