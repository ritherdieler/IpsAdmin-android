package com.dscorp.ispadmin.data.repository

import com.dscorp.ispadmin.domain.model.Payment
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
    fun `error without body uses a readable fallback`() {
        val thrown = assertThrows(Exception::class.java) {
            PaymentResponses.registeredOrThrow(error(500, ""))
        }
        assertEquals("Error al registrar el pago", thrown.message)
    }
}
