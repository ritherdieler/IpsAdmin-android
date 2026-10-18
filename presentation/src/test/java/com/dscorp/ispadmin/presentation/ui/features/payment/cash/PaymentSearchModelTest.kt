package com.dscorp.ispadmin.presentation.ui.features.payment.cash

import com.dscorp.ispadmin.data.datasource.remote.CashPaymentResponse
import com.dscorp.ispadmin.data.datasource.remote.CashSearchResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

internal fun search(vararg receipts: com.dscorp.ispadmin.data.datasource.remote.CashReceiptResponse) =
    CashSearchResponse(from = "2026-09-19", to = "2026-10-18", receipts = receipts.toList())

internal fun ccori() = search(
    receipt(9, 620, "HILDA CCORI MAMANI", 50.0, "YAPE", "2026-10-18T11:30:00", "2026-10", payer = "Hilda Ccori M."),
    receipt(8, 1777, "ROSA RAMIREZ CCORI", 50.0, "EFECTIVO", "2026-10-18T11:05:00", "2026-10"),
    receipt(7, 1777, "ROSA RAMIREZ CCORI", 50.0, "EFECTIVO", "2026-10-17T16:20:00", "2026-09"),
    receipt(6, 1777, "ROSA RAMIREZ CCORI", 50.0, "YAPE", "2026-10-08T17:46:00", "2026-08", payer = "Rosa Ramírez C."),
)

internal val PAID_JULY = CashPaymentResponse(
    id = 46790, subscriptionId = 1842, subscriber = "JORGE RAMOS VILLANUEVA", amount = 50.0, method = "YAPE",
    payerName = "Jorge Ramos V.", collector = "Rosa Ccori", paidAt = "2026-07-12T16:05:00", serviceMonth = "2026-07",
    dueDate = "2026-07-15", discountAmount = null, discountReason = null, hasProof = true,
)

class PaymentSearchModelTest {
    private val today = LocalDate.of(2026, 10, 18)

    @Test
    fun `payments are grouped by day with who paid and when`() {
        val result = PaymentSearchResult.from(ccori(), PaymentPeriod.LAST_30, today)

        assertEquals(listOf("Hoy", "Ayer", "8 oct"), result.groups.map { it.label })
        val first = result.groups.first().hits.first()
        assertEquals(PaymentHit(9, "Hilda Ccori Mamani", "Yape · pagó: Hilda Ccori M. · 11:30", "S/ 50.00"), first)
        assertEquals("Efectivo · 11:05", result.groups.first().hits[1].detail)
        assertEquals("4 pagos en los últimos 30 días", result.footer)
        assertEquals("1 pago en los últimos 7 días", PaymentSearchResult.from(search(ccori().receipts.first()), PaymentPeriod.LAST_7, today).footer)
        assertEquals("Sin pagos en los últimos 90 días", PaymentSearchResult.from(search(), PaymentPeriod.LAST_90, today).footer)
    }

    @Test
    fun `matches are found ignoring case and accents`() {
        assertEquals(listOf(6..10), matchRanges("Hilda Ccori Mamani", listOf("ccori")))
        assertEquals(listOf(5..11), matchRanges("Rosa Ramírez C.", listOf("ramirez")))
        assertEquals(emptyList<IntRange>(), matchRanges("Rosa", emptyList()))
    }

    @Test
    fun `the detail reads like the receipt`() {
        val detail = PaymentDetail.from(PAID_JULY)

        assertEquals("Pago de jul 2026", detail.title)
        assertEquals("S/ 50.00", detail.amount)
        assertEquals("Jorge Ramos Villanueva · Cód. 1842", detail.holder)
        assertEquals(
            listOf(
                "Recibo" to "jul 2026",
                "Vencía" to "15 jul 2026",
                "Pagado" to "12 jul 2026 · 16:05",
                "Método" to "Yape",
                "Nombre en Yape" to "Jorge Ramos V.",
                "Descuento" to "—",
                "Registrado por" to "Rosa C.",
                "N.º" to "P-46790",
            ),
            detail.rows,
        )
        assertTrue(detail.hasProof)
        assertEquals("Yape", detail.method)
    }

    @Test
    fun `a discounted cash payment without due date skips what it lacks`() {
        val detail = PaymentDetail.from(
            PAID_JULY.copy(method = "EFECTIVO", payerName = null, dueDate = null, discountAmount = 5.0, discountReason = "Promoción", hasProof = false, collector = null),
        )

        assertEquals("S/ 5.00 · Promoción", detail.rows.toMap()["Descuento"])
        assertEquals("Sin registrar", detail.rows.toMap()["Registrado por"])
        assertFalse(detail.rows.toMap().containsKey("Vencía"))
        assertFalse(detail.rows.any { it.first.startsWith("Nombre en") })
        assertFalse(detail.hasProof)
    }
}
