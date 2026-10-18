package com.dscorp.ispadmin.presentation.ui.features.home

import com.dscorp.ispadmin.data.model.InstallationOrderStatus
import com.dscorp.ispadmin.data.model.UserDto
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SalesAgendaModelTest {
    private val diego = UserDto(id = 9, name = "Diego", lastName = "Huerta")
    private val orders = listOf(
        order(6, technician = diego, scheduled = NOW.plusDays(1).withHour(9)),
        order(7, status = InstallationOrderStatus.SOLICITADO, created = NOW.minusDays(2).withHour(8), first = "Norma", last = "Quispe Rios",
            address = "Jr. Lima 120", place = "santa anita"),
        order(8, status = InstallationOrderStatus.CERRADO, technician = diego, scheduled = NOW.minusDays(1)),
        order(9, status = InstallationOrderStatus.CANCELADO),
    )
    private val tickets = listOf(
        ticket(3612, category = "Instalación de Internet", status = AssistanceTicketStatus.ASSIGNED, assignedTo = "Marco Salinas",
            name = "LUCIA MENDOZA ROJAS", place = "la villa", address = "Calle Las Flores 88", scheduled = NOW.withHour(10).withMinute(30)),
        ticket(3598, category = "Instalación de Internet", created = NOW.minusDays(2).withHour(9), name = "ANA PEREZ", place = "santa anita"),
        ticket(3609, category = "Sin Conexión a Internet"),
        ticket(3500, category = "Instalación de Internet", status = AssistanceTicketStatus.CLOSED, closed = NOW.minusDays(2)),
        ticket(3400, category = "Instalación de Internet", status = AssistanceTicketStatus.CLOSED, closed = NOW.minusDays(5)), // last week
    )

    private fun agenda(filter: AgendaFilter = AgendaFilter.ALL) =
        SalesAgenda.from(orders, tickets, NOW.atZone(LIMA).toInstant(), filter)

    @Test
    fun `scheduled installations come first by time and the unassigned ones after, oldest first`() {
        val items = agenda().items

        assertEquals(listOf("Ticket #3612", "Orden #6", "Orden #7", "Ticket #3598"), items.map { it.reference })
        assertEquals(
            AgendaItem(TodayTarget.Ticket(3612), "Hoy 10:30", "Ticket #3612", "Lucia Mendoza Rojas", "Calle Las Flores 88 · La Villa", "Marco Salinas", scheduled = true),
            items[0],
        )
        assertEquals("Jue 15 oct 09:00", items[1].whenLabel)
        assertEquals("Diego Huerta", items[1].detail)
        assertEquals(
            AgendaItem(TodayTarget.Order(7), "Por asignar", "Orden #7", "Norma Quispe Rios", "Jr. Lima 120 · Santa Anita", "Sin técnico ni fecha · creado el 12 oct", scheduled = false),
            items[2],
        )
    }

    @Test
    fun `filters count unassigned and scheduled installations`() {
        val all = agenda()

        assertEquals("Por asignar · 2", all.unassignedLabel)
        assertEquals("Programadas · 2", all.scheduledLabel)
        assertEquals("4 instalaciones · tickets y órdenes, la más próxima primero", all.summary)
        assertEquals(listOf("Orden #7", "Ticket #3598"), agenda(AgendaFilter.UNASSIGNED).items.map { it.reference })
        assertEquals(listOf("Ticket #3612", "Orden #6"), agenda(AgendaFilter.SCHEDULED).items.map { it.reference })
    }

    @Test
    fun `done this week counts closed orders and installation tickets since monday`() {
        assertEquals("Realizadas esta semana (2)", agenda().doneLabel)
    }
}
