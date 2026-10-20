package com.dscorp.ispadmin.data.repository

import com.dscorp.ispadmin.data.utils.ApiErrorBodyParser
import com.dscorp.ispadmin.domain.model.Payment
import com.dscorp.ispadmin.domain.model.PaymentMethodOption
import kotlinx.coroutines.CancellationException
import retrofit2.Response

/** Turns Core payment responses into a result or an exception carrying the Core message. */
object PaymentResponses {
    private const val REGISTER_FALLBACK = "Error al registrar el pago"

    /**
     * Active catalog of the current production Core (V66, without the online "card-app"). That Core has no
     * GET payment/methods (it reads "methods" as a payment id), so the app offers these instead.
     */
    val FALLBACK_METHODS = listOf(
        PaymentMethodOption("Efectivo"),
        PaymentMethodOption("Yape", requiresPayerName = true),
        PaymentMethodOption("Plin", requiresPayerName = true),
        PaymentMethodOption("Transferencia"),
        PaymentMethodOption("Depósito"),
        PaymentMethodOption("Tarjeta"),
    )

    fun registeredOrThrow(response: Response<Payment>): Payment {
        val body = response.body()
        if (response.isSuccessful && body != null) return body
        throw Exception(ApiErrorBodyParser.parse(response.errorBody()?.string(), REGISTER_FALLBACK))
    }

    /** The Core catalog when it answers; otherwise [FALLBACK_METHODS] so charging still offers methods. */
    suspend fun methodsOrFallback(fetch: suspend () -> Response<List<PaymentMethodOption>>): List<PaymentMethodOption> {
        val response = try {
            fetch()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return FALLBACK_METHODS
        }
        val body = response.body()
        return if (response.isSuccessful && body != null) body else FALLBACK_METHODS
    }
}
