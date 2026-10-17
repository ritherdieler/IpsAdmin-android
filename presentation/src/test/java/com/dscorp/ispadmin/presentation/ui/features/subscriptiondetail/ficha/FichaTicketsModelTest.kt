package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha

import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.util.Date

class FichaTicketsModelTest {

    private fun ticket(id: Int, status: AssistanceTicketStatus, category: String, created: LocalDateTime, assignedTo: String? = null) =
        AssistanceTicketResponse(
            id = id, name = "JORGE RAMOS", category = category, description = "", status = status, priority = "Media",
            createdAt = Date.from(created.atZone(FICHA_ZONE).toInstant()), assignedTo = assignedTo, sheetImageUrl = null,
        )

    @Test
    fun `tickets read newest first with their state and count the open ones`() {
        val tickets = FichaTickets.from(
            listOf(
                ticket(3609, AssistanceTicketStatus.PENDING, "Sin Conexión a Internet", LocalDateTime.of(2026, 10, 14, 9, 0)),
                ticket(3402, AssistanceTicketStatus.CLOSED, "Internet lento", LocalDateTime.of(2026, 7, 2, 16, 20), assignedTo = "Diego Huerta"),
                ticket(3500, AssistanceTicketStatus.IN_PROGRESS, "Cambio de clave Wi-Fi", LocalDateTime.of(2026, 9, 1, 8, 0), assignedTo = "Marco Salinas"),
            )
        )

        assertEquals(2, tickets.openCount)
        assertEquals(
            listOf(
                TicketRow(3609, "Sin Conexión a Internet", "#3609 · 14 oct 2026", StatusBadge("Pendiente", BadgeTone.WARNING)),
                TicketRow(3500, "Cambio de clave Wi-Fi", "#3500 · 1 set 2026 · Marco Salinas", StatusBadge("En curso", BadgeTone.INFO)),
                TicketRow(3402, "Internet lento", "#3402 · 2 jul 2026 · Diego Huerta", StatusBadge("Cerrado", BadgeTone.NEUTRAL)),
            ),
            tickets.rows,
        )
    }
}
