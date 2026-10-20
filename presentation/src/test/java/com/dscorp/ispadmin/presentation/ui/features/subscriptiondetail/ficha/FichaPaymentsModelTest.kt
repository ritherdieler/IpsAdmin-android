package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha

import com.dscorp.ispadmin.domain.model.Payment
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class FichaPaymentsModelTest {

    private val today = LocalDate.of(2026, 10, 14)

    private fun month(m: Int) = LocalDate.of(2026, m, 1).atStartOfDay(FICHA_ZONE).toInstant().toEpochMilli()
    private fun at(day: LocalDateTime) = day.atZone(FICHA_ZONE).toInstant().toEpochMilli()

    private val receipts = listOf(
        Payment(id = 10, billingDate = month(10), amountToPay = 50.0, dueDate = "2026-10-15T00:00:00"),
        Payment(id = 9, billingDate = month(9), amountToPay = 50.0, dueDate = "2026-09-15T00:00:00"),
        Payment(id = 8, billingDate = month(8), amountToPay = 50.0, dueDate = "2026-08-15T00:00:00"),
        Payment(id = 7, billingDate = month(7), amountToPay = 50.0, amountPaid = 50.0, paid = true, method = "Yape",
            paymentDate = at(LocalDateTime.of(2026, 7, 12, 11, 42)), dueDate = "2026-07-15T00:00:00"),
        Payment(id = 5, billingDate = month(5), amountToPay = 50.0, amountPaid = 45.0, discountAmount = 5.0, paid = true, method = "Yape",
            paymentDate = at(LocalDateTime.of(2026, 5, 14, 9, 0)), dueDate = "2026-05-15T00:00:00"),
    )

    @Test
    fun `pending card counts overdue receipts and names their months`() {
        val tab = FichaPayments.from(receipts, totalDebt = 100.0, today = today)

        assertEquals(PendingCard("S/ 100.00", "2 recibos vencidos · ago y set 2026", "Cobrar S/ 100.00"), tab.pending)
        assertEquals("S/ 50.00 · 12 jul · Yape", tab.lastPayment)
        assertEquals("Vencen el 15 de cada mes", tab.dueRule)
    }

    @Test
    fun `receipts read newest first with their state`() {
        val rows = FichaPayments.from(receipts, totalDebt = 100.0, today = today).receipts

        assertEquals(
            listOf(
                ReceiptRow(10, "Oct 2026", "Vence mañana 15 oct", StatusBadge("Pendiente", BadgeTone.WARNING), "S/ 50.00", overdue = false),
                ReceiptRow(9, "Set 2026", "Venció 15 set", StatusBadge("Vencido", BadgeTone.ERROR), "S/ 50.00", overdue = true),
                ReceiptRow(8, "Ago 2026", "Venció 15 ago", StatusBadge("Vencido", BadgeTone.ERROR), "S/ 50.00", overdue = true),
                ReceiptRow(7, "Jul 2026", "Pagado 12 jul · Yape", StatusBadge("Pagado", BadgeTone.SUCCESS), "S/ 50.00", overdue = false),
                ReceiptRow(5, "May 2026", "Pagado 14 may · Yape · desc. S/ 5.00", StatusBadge("Pagado", BadgeTone.SUCCESS), "S/ 45.00", overdue = false),
            ),
            rows,
        )
    }

    @Test
    fun `without a Core total the unpaid receipts give the pending card`() {
        val tab = FichaPayments.from(receipts, totalDebt = 0.0, today = today)

        assertEquals(PendingCard("S/ 150.00", "2 recibos vencidos · ago y set 2026", "Cobrar S/ 150.00"), tab.pending)
    }

    @Test
    fun `an up to date subscription has no pending card`() {
        val tab = FichaPayments.from(receipts.filter { it.paid }, totalDebt = 0.0, today = today)

        assertNull(tab.pending)
        assertEquals("S/ 50.00 · 12 jul · Yape", tab.lastPayment)
    }
}
