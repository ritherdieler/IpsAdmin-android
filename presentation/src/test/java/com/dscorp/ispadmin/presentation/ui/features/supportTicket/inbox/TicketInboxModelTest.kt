package com.dscorp.ispadmin.presentation.ui.features.supportTicket.inbox

import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.FICHA_ZONE
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.util.Date

class TicketInboxModelTest {

    private val now = LocalDateTime.of(2026, 10, 14, 11, 0).atZone(FICHA_ZONE).toInstant()
    private fun at(day: Int, hour: Int, minute: Int = 0) = Date.from(LocalDateTime.of(2026, 10, day, hour, minute).atZone(FICHA_ZONE).toInstant())

    private fun ticket(id: Int, priority: String, created: Date, category: String = "Otros", assignedTo: String? = null,
                       scheduledAt: Date? = null, place: String = "la villa", external: Boolean = false) = AssistanceTicketResponse(
        id = id, name = "PEDRO SANCHEZ", category = category, description = "", status = AssistanceTicketStatus.PENDING,
        priority = priority, createdAt = created, scheduledAt = scheduledAt, assignedTo = assignedTo, place = place,
        sheetImageUrl = null, isExternalCustomer = external,
    )

    private val tickets = listOf(
        ticket(3611, "Baja", at(14, 6), category = "No tiene señal de TV cable", assignedTo = "Julio Paredes", scheduledAt = at(15, 9)),
        ticket(3609, "Alta", at(14, 9), category = "Sin Conexión a Internet", place = "las colinas"),
        ticket(3612, "Media", at(13, 16, 20), category = "Instalación de internet", assignedTo = "marco salinas", scheduledAt = at(14, 10, 30), external = true),
        ticket(3610, "Baja", at(14, 8), category = "Evaluar factibilidad de servicio", place = "san miguel", external = true),
    )

    @Test
    fun `high priority first, then the newest, as compact rows`() {
        val rows = ticketRows(tickets, TicketFilters(), now)

        assertEquals(listOf(3609, 3612, 3610, 3611), rows.map { it.id })
        assertEquals(
            TicketInboxRow(3609, StatusBadge("▲ Alta", BadgeTone.ERROR), "#3609", "hace 2 h", "Sin conexión a internet",
                "Pedro Sanchez · Las Colinas", tag = null, assignment = "Sin asignar", assigned = false),
            rows[0],
        )
        assertEquals("ayer 16:20", rows[1].time)
        assertEquals(StatusBadge("No abonado", BadgeTone.INFO), rows[1].tag)
        assertEquals("Marco Salinas · hoy 10:30", rows[1].assignment)
        assertEquals("Julio Paredes · jue 15, 09:00", rows[3].assignment)
    }

    @Test
    fun `chips narrow the list and categories and places come from the tickets`() {
        assertEquals(listOf(3609), ticketRows(tickets, TicketFilters(highPriority = true), now).map { it.id })
        assertEquals(listOf(3609, 3610), ticketRows(tickets, TicketFilters(unassigned = true), now).map { it.id })
        assertEquals(listOf(3612), ticketRows(tickets, TicketFilters(category = "Instalación de internet"), now).map { it.id })
        assertEquals(listOf(3612, 3611), ticketRows(tickets, TicketFilters(place = "La Villa"), now).map { it.id })
        assertEquals(listOf("Evaluar factibilidad de servicio", "Instalación de internet", "No tiene señal de TV cable", "Sin conexión a internet"), categoryOptions(tickets))
        assertEquals(listOf("La Villa", "Las Colinas", "San Miguel"), placeOptions(tickets))
    }

    @Test
    fun `summary says how many and how they are sorted`() {
        assertEquals("4 pendientes · prioridad alta primero", inboxSummary(TicketTab.PENDING, 4))
        assertEquals("1 pendiente · prioridad alta primero", inboxSummary(TicketTab.PENDING, 1))
        assertEquals("1 en curso", inboxSummary(TicketTab.IN_PROGRESS, 1))
        assertEquals("63 cerrados este mes", inboxSummary(TicketTab.CLOSED, 63))
        assertEquals(listOf("Pendientes", "En curso", "Cerrados"), TicketTab.entries.map { it.label })
    }

    @Test
    fun `tab counts add reopened to pending and in progress to taken`() {
        val counts = tabCounts(mapOf("PENDING" to 3L, "REOPEN" to 1L, "ASSIGNED" to 1L, "IN_PROGRESS" to 2L, "CLOSED" to 900L))

        assertEquals(mapOf(TicketTab.PENDING to 4L, TicketTab.IN_PROGRESS to 3L), counts)
    }
}
