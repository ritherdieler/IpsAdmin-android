package com.dscorp.ispadmin.data.repository

import com.dscorp.ispadmin.domain.model.Payment
import com.dscorp.ispadmin.domain.model.PaymentMethodOption
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.Response

class PaymentResponsesTest {

    private fun error(code: Int, body: String) =
        Response.error<Payment>(code, body.toResponseBody("application/json".toMediaType()))

    @Test
    fun `register payment error shows the Core message`() {
        val thrown = assertThrows(Exception::class.java) {
            PaymentResponses.registeredOrThrow(error(400, """{"error":"Método de pago no válido"}"""))
        }
        assertEquals("Método de pago no válido", thrown.message)
    }

    @Test
    fun `concurrent modification shows the Core message`() {
        val detail = "El registro fue modificado por otra operación. Vuelve a cargarlo e inténtalo de nuevo."
        val thrown = assertThrows(Exception::class.java) {
            PaymentResponses.registeredOrThrow(error(409, """{"code":"CONCURRENT_MODIFICATION","message":"$detail"}"""))
        }
        assertEquals(detail, thrown.message)
    }

    @Test
    fun `payment methods come from the Core catalog when it answers`() = runTest {
        val catalog = listOf(PaymentMethodOption("Efectivo"), PaymentMethodOption("Yape", requiresPayerName = true))

        assertEquals(catalog, PaymentResponses.methodsOrFallback { Response.success(catalog) })
    }

    @Test
    fun `without GET payment methods the production catalog is used`() = runTest {
        val production = listOf(
            PaymentMethodOption("Efectivo"),
            PaymentMethodOption("Yape", requiresPayerName = true),
            PaymentMethodOption("Plin", requiresPayerName = true),
            PaymentMethodOption("Transferencia"),
            PaymentMethodOption("Depósito"),
            PaymentMethodOption("Tarjeta"),
        )
        // The current Core reads "methods" as a payment id and answers 400.
        val missing = Response.error<List<PaymentMethodOption>>(400, "".toResponseBody("application/json".toMediaType()))

        assertEquals(production, PaymentResponses.methodsOrFallback { missing })
        assertEquals(production, PaymentResponses.methodsOrFallback { throw java.io.IOException("timeout") })
    }

    @Test
    fun `error without body uses a readable fallback`() {
        val thrown = assertThrows(Exception::class.java) {
            PaymentResponses.registeredOrThrow(error(500, ""))
        }
        assertEquals("Error al registrar el pago", thrown.message)
    }
}
