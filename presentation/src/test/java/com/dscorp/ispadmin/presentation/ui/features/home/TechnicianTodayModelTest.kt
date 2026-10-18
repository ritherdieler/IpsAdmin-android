package com.dscorp.ispadmin.presentation.ui.features.home

import com.dscorp.ispadmin.data.model.InstallationOrderStatus
import com.dscorp.ispadmin.data.model.UserDto
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TechnicianTodayModelTest {
    private val me = User(id = 7, name = "Marco", lastName = "Salinas", type = User.UserType.TECHNICIAN)
    private val marco = UserDto(id = 7, name = "Marco", lastName = "Salinas")

    private val install = ticket(
        3612, category = "Instalación de Internet", status = AssistanceTicketStatus.ASSIGNED, name = "LUCIA MENDOZA ROJAS",
        place = "la villa", address = "Calle Las Flores 88", assignedTo = "Marco Salinas", scheduled = NOW.withHour(10).withMinute(30),
    )
    private val open = listOf(
        install,
        ticket(3609, priority = "Alta", created = NOW.minusHours(2)),
        ticket(3606, category = "Internet Lento", status = AssistanceTicketStatus.IN_PROGRESS, assignedTo = "Diego Huerta",
            name = "ELENA FLORES RIOS", place = "la merced"),
        ticket(3611, category = "No tiene señal de Tv Cable", priority = "Baja", created = NOW.minusHours(1),
            name = "GLORIA TICONA APAZA", place = "9 de octubre"),
    )
    private val orders = listOf(
        order(6, technician = marco, scheduled = NOW.withHour(15).withMinute(0)),
        order(7, technician = marco, scheduled = NOW.plusDays(1)), // tomorrow
        order(8, status = InstallationOrderStatus.CERRADO, technician = marco, scheduled = NOW.withHour(8)),
    )

    private fun today(
        tickets: List<com.dscorp.ispadmin.data.response.AssistanceTicketResponse> = open,
        closed: List<com.dscorp.ispadmin.data.response.AssistanceTicketResponse> = emptyList(),
        pendingUploads: Int = 0,
    ) = TechnicianToday.from(me, tickets, closed, orders, pendingUploads, NOW.atZone(LIMA).toInstant())

    @Test
    fun `my jobs for today are counted and the nearest one is now`() {
        val today = today()

        assertEquals("mié 14 oct · 2 trabajos", today.subtitle)
        val now = today.now!!
        assertEquals(TodayTarget.Ticket(3612), now.target)
        assertEquals("Ticket #3612", now.label)
        assertEquals("10:30", now.time)
        assertEquals(StatusBadge("Media", BadgeTone.INFO), now.priority)
        assertEquals("Instalación de internet", now.title)
        assertEquals("Lucia Mendoza Rojas", now.name)
        assertEquals("Calle Las Flores 88 · La Villa", now.address)
        assertTrue(now.registers)
    }

    @Test
    fun `pending lists my later jobs first and then open tickets by priority`() {
        val rows = today().pending

        assertEquals(listOf("Orden #6", "#3609", "#3606", "#3611"), rows.map { it.number })
        assertEquals(TodayRow(TodayTarget.Order(6), "Orden #6", null, "15:00", null, "Instalación de internet", "Tomas Vega Salas · 9 De Octubre"), rows[0])
        assertEquals("hace 2 h", rows[1].trailing)
        assertEquals(StatusBadge("En curso · Diego Huerta", BadgeTone.INFO), rows[2].trailingBadge)
        assertEquals("Sin conexión a internet", rows[1].title)
        assertEquals("Pedro Sanchez Chavez · Las Colinas", rows[1].subtitle)
    }

    @Test
    fun `a support ticket as the current job opens the ticket instead of registering`() {
        val now = today(tickets = listOf(ticket(3700, status = AssistanceTicketStatus.ASSIGNED, assignedTo = "marco salinas", scheduled = NOW))).now!!

        assertFalse(now.registers)
        assertEquals("Sin conexión a internet", now.title)
    }

    @Test
    fun `done today counts the tickets I closed today`() {
        val closed = listOf(
            ticket(3500, status = AssistanceTicketStatus.CLOSED, assignedTo = "Marco Salinas", closed = NOW.minusHours(1)),
            ticket(3501, status = AssistanceTicketStatus.CLOSED, assignedTo = "Marco Salinas", closed = NOW.minusDays(1)),
            ticket(3502, status = AssistanceTicketStatus.CLOSED, assignedTo = "Diego Huerta", closed = NOW.minusHours(1)),
        )

        val done = today(closed = closed).done

        assertEquals(listOf("#3500"), done.map { it.number })
    }

    @Test
    fun `uploads banner says whether everything was sent`() {
        assertEquals(UploadBanner("Todo enviado", "sin pendientes de envío", allSent = true), today().uploads)
        assertEquals(UploadBanner("2 registros sin enviar", "se envían al volver la conexión", allSent = false), today(pendingUploads = 2).uploads)
    }

    @Test
    fun `a day without jobs says so`() {
        val today = TechnicianToday.from(me, listOf(ticket(3609)), emptyList(), emptyList(), 0, NOW.atZone(LIMA).toInstant())

        assertEquals("mié 14 oct · sin trabajos asignados", today.subtitle)
        assertNull(today.now)
    }
}
