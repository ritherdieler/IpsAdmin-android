package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha

import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus
import com.dscorp.ispadmin.domain.model.Payment
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date

class FichaHistoryModelTest {

    private fun at(year: Int, month: Int, day: Int, hour: Int = 10) =
        LocalDateTime.of(year, month, day, hour, 0).atZone(FICHA_ZONE).toInstant().toEpochMilli()

    private val logs = listOf(
        HistoryLog(at(2026, 9, 15, 6), "CUT_FOR_DEBT"),
        HistoryLog(at(2026, 4, 20), "CHANGE_PLAN", planName = "basico", planPrice = 50.0, previousPlanPrice = 70.0, responsibleName = "Sergio Carrillo"),
        HistoryLog(at(2025, 3, 12), "NEW_SUBSCRIPTION", planName = "internet70", responsibleName = "Marco Salinas"),
    )
    private val receipts = listOf(
        Payment(id = 7, paid = true, amountPaid = 50.0, amountToPay = 50.0, method = "Yape", paymentDate = at(2026, 7, 12),
            billingDate = LocalDate.of(2026, 7, 1).atStartOfDay(FICHA_ZONE).toInstant().toEpochMilli(), responsibleName = "Rosa C."),
        Payment(id = 9, paid = false, amountToPay = 50.0, billingDate = LocalDate.of(2026, 9, 1).atStartOfDay(FICHA_ZONE).toInstant().toEpochMilli()),
    )
    private val tickets = listOf(
        AssistanceTicketResponse(
            id = 3402, name = "JORGE", category = "Internet lento", description = "", status = AssistanceTicketStatus.CLOSED,
            priority = "Media", createdAt = Date(at(2026, 7, 2)), resolvedAt = Date(at(2026, 7, 3)), assignedTo = "diego huerta", sheetImageUrl = null,
        )
    )

    @Test
    fun `history merges changes, payments and tickets by month, newest first`() {
        val months = FichaHistory.from(logs, receipts, tickets).months(HistoryFilter.ALL)

        assertEquals(listOf("Set 2026", "Jul 2026", "Abr 2026", "Mar 2025"), months.map { it.title })
        assertEquals(
            HistoryItem("15 set", "Corte automático por deuda", null, "Sistema", HistoryKind.NETWORK),
            months[0].items.single(),
        )
        assertEquals(
            listOf(
                HistoryItem("12 jul", "Pago S/ 50.00 · Yape", "Recibo de jul 2026", "Rosa C.", HistoryKind.PAYMENT),
                HistoryItem("3 jul", "Ticket #3402 cerrado · Internet lento", "Prioridad Media · abierto el 2 jul", "Diego Huerta", HistoryKind.TICKET),
            ),
            months[1].items,
        )
        assertEquals(
            HistoryItem("20 abr", "Cambio de plan a Basico", "S/ 70.00 → S/ 50.00 al mes", "Sergio Carrillo", HistoryKind.CHANGE),
            months[2].items.single(),
        )
        assertEquals(
            HistoryItem("12 mar", "Alta del servicio · Internet70", null, "Marco Salinas", HistoryKind.CHANGE),
            months[3].items.single(),
        )
    }

    @Test
    fun `chips keep one kind of event`() {
        val history = FichaHistory.from(logs, receipts, tickets)

        assertEquals(listOf("Jul 2026"), history.months(HistoryFilter.PAYMENTS).map { it.title })
        assertEquals(listOf("Pago S/ 50.00 · Yape"), history.months(HistoryFilter.PAYMENTS).flatMap { it.items }.map { it.title })
        assertEquals(listOf("Set 2026"), history.months(HistoryFilter.NETWORK).map { it.title })
        assertEquals(listOf("Abr 2026", "Mar 2025"), history.months(HistoryFilter.CHANGES).map { it.title })
        assertEquals(
            listOf("Todo", "Pagos", "Tickets", "Red", "Cambios"),
            HistoryFilter.entries.map { it.label },
        )
    }
}
