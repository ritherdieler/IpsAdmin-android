package com.dscorp.ispadmin.domain.model

import com.dscorp.ispadmin.di.buildApiGson
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomerDetailJsonTest {
    @Test
    fun `customer detail reads the credit balance`() {
        val detail = buildApiGson().fromJson("""{"id":4,"creditBalance":20.1,"subscriptions":[]}""", CustomerDetail::class.java)

        assertEquals(20.1, detail.creditBalance, 0.0)
    }

    @Test
    fun `customer detail reads document, business identity and total debt`() {
        val detail = buildApiGson().fromJson(
            """{"id":4,"clientType":"BUSINESS","documentType":"RUC","ruc":"20123456789","businessName":"Acme","totalDebt":110.5}""",
            CustomerDetail::class.java,
        )

        assertEquals("BUSINESS", detail.clientType)
        assertEquals("RUC", detail.documentType)
        assertEquals("20123456789", detail.ruc)
        assertEquals("Acme", detail.businessName)
        assertEquals(110.5, detail.totalDebt, 0.0)
    }

    @Test
    fun `payment carries the charge type and the amount received`() {
        val payment = Payment(id = 1, chargeType = "INSTALLATION", amountReceived = 100.0)

        assertEquals("INSTALLATION", payment.chargeType)
        assertEquals(100.0, payment.amountReceived!!, 0.0)
    }
}
