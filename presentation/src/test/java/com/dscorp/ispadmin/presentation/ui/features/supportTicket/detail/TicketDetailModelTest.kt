package com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail

import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus
import com.dscorp.ispadmin.di.buildApiGson
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.model.User.UserType
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.FICHA_ZONE
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.subscription
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose.TicketAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.util.Date

class TicketDetailModelTest {

    private val now = LocalDateTime.of(2026, 10, 14, 10, 42).atZone(FICHA_ZONE).toInstant()
    private fun at(day: Int, hour: Int, minute: Int) = Date.from(LocalDateTime.of(2026, 10, day, hour, minute).atZone(FICHA_ZONE).toInstant())

    private val pending = AssistanceTicketResponse(
        id = 3609, name = "PEDRO SANCHEZ CHAVEZ", phone = "964818302", category = "Sin Conexión a Internet",
        description = "No tiene internet desde ayer en la noche.", status = AssistanceTicketStatus.PENDING,
        priority = "Alta", createdAt = at(14, 8, 40), sheetImageUrl = null, subscriptionId = 1842,
    )
    private val cut = subscription(status = ServiceStatus.CUT_OFF, pendingInvoices = 1, totalDebt = 50.0, firstName = "PEDRO", lastName = "SANCHEZ CHAVEZ")
        .copy(phone = "964818302")
    private val admin = User(id = 1, name = "Sergio", type = UserType.ADMIN)

    @Test
    fun `the Core ticket fields the app needs are read, including reopened tickets`() {
        val ticket = buildApiGson().fromJson(
            """{"id":7,"name":"ANA","category":"Otros","description":"","status":"REOPEN","priority":"Baja",
               "assignedAt":"Oct 14, 2026 9:40:00 AM","subscriptionId":1842,"placeId":3,"isExternalCustomer":false}""",
            AssistanceTicketResponse::class.java,
        )

        assertEquals(AssistanceTicketStatus.REOPEN, ticket.status)
        assertEquals(1842, ticket.subscriptionId)
        assertEquals(3, ticket.placeId)
        assertEquals(false, ticket.isExternalCustomer)
    }

    @Test
    fun `a pending ticket of a cut subscriber reads like the design`() {
        val detail = TicketDetail.from(pending, cut, previous = emptyList(), user = admin, now = now)

        assertEquals("Ticket #3609", detail.title)
        assertEquals(listOf(StatusBadge("Pendiente", BadgeTone.WARNING), StatusBadge("▲ Alta", BadgeTone.ERROR)), detail.badges)
        assertEquals("Sin conexión a internet", detail.category)
        assertEquals("Creado hace 2 h", detail.meta)
        assertEquals(
            DebtNotice("Pedro está cortado por deuda: debe S/ 50.00. Cobrar restablece el servicio y podría resolver el ticket.", "Cobrar S/ 50.00"),
            detail.debtNotice,
        )
        assertEquals("Contacto: 964 818 302 · titular", detail.contact)
        assertEquals("Sin asignar", detail.assignment)
        assertEquals(TicketAction.TAKE, detail.primary)
        assertEquals("Pasa a En curso y queda a tu nombre.", detail.primaryHint)
        assertEquals(true, detail.canAssign)
    }

    @Test
    fun `a taken ticket says who and for how long and offers reschedule and close`() {
        val taken = pending.copy(
            status = AssistanceTicketStatus.ASSIGNED, assignedTo = "diego huerta", assignedAt = at(14, 9, 40),
            category = "Internet lento", priority = "Media", phone = "999888777",
        )
        val detail = TicketDetail.from(taken, cut.copy(serviceStatus = ServiceStatus.ACTIVE, totalDebt = 0.0, pendingInvoiceQuantity = 0), emptyList(), admin, now)

        assertEquals(listOf(StatusBadge("En curso", BadgeTone.INFO), StatusBadge("Media", BadgeTone.INFO)), detail.badges)
        assertEquals("Tomado por Diego Huerta a las 9:40 a. m. · 1 h 2 min", detail.meta)
        assertEquals("Diego Huerta · hoy 9:40 a. m.", detail.assignment)
        assertEquals("Contacto: 999 888 777", detail.contact)
        assertNull(detail.debtNotice)
        assertEquals(TicketAction.CLOSE, detail.primary)
        assertEquals(true, detail.canReschedule)
        assertEquals(false, detail.canAssign)
    }

    @Test
    fun `previous tickets leave out the open one and a non subscriber has no card`() {
        val older = pending.copy(id = 3471, status = AssistanceTicketStatus.CLOSED, category = "Internet lento", assignedTo = "Marco Salinas")
        val detail = TicketDetail.from(pending, cut, previous = listOf(pending, older), user = admin, now = now)
        assertEquals(listOf(3471), detail.previous.map { it.id })

        val external = TicketDetail.from(pending.copy(subscriptionId = null, isExternalCustomer = true), null, emptyList(), admin, now)
        assertNull(external.subscriber)
        assertEquals(listOf(StatusBadge("Pendiente", BadgeTone.WARNING), StatusBadge("▲ Alta", BadgeTone.ERROR), StatusBadge("No abonado", BadgeTone.INFO)), external.badges)
    }

    @Test
    fun `relative times read in plain words`() {
        assertEquals("hace un momento", relativeLabel(now.minusSeconds(20), now))
        assertEquals("hace 25 min", relativeLabel(now.minusSeconds(25 * 60), now))
        assertEquals("ayer 4:20 p. m.", relativeLabel(at(13, 16, 20).toInstant(), now))
        assertEquals("9 oct 12:16 p. m.", relativeLabel(at(9, 12, 16).toInstant(), now))
        assertEquals("1 h 2 min", elapsedLabel(Instant.ofEpochMilli(at(14, 9, 40).time), now))
        assertEquals("35 min", elapsedLabel(now.minusSeconds(35 * 60), now))
    }

    @Test
    fun `a Mi GigaFiber ticket gets the App badge, the slot and only what the customer can read`() {
        val app = pending.copy(origin = "APP", preferredSlot = "MORNING")
        val thread = com.dscorp.ispadmin.data.response.TicketThreadResponse(
            ticketId = 3609, origin = "APP", slot = "AFTERNOON", preferredDate = "2026-11-10",
            comments = listOf(
                com.dscorp.ispadmin.data.response.TicketThreadCommentResponse(1, "CUSTOMER", "COMMENT", null, "Se corta en la noche", true, "2026-11-02T19:40:00-05:00"),
                com.dscorp.ispadmin.data.response.TicketThreadCommentResponse(2, "STAFF", "COMMENT", "Luis Pérez", "Nota interna", false, "2026-11-03T09:00:00-05:00"),
                com.dscorp.ispadmin.data.response.TicketThreadCommentResponse(3, "STAFF", "COMMENT", "Luis Pérez", "Te visitamos mañana", true, "2026-11-03T10:15:00-05:00"),
            ),
        )

        val detail = TicketDetail.from(app, cut, emptyList(), admin, now, thread)

        assertEquals(StatusBadge("App", BadgeTone.INFO), detail.badges.last())
        assertEquals("Prefiere la tarde (13 a 18 h) · mar 10 nov", detail.customer?.slot)
        assertEquals(
            listOf(
                TicketCustomerComment("Cliente", "Se corta en la noche", "lun 2 nov, 7:40 p. m.", fromCustomer = true),
                TicketCustomerComment("Luis Pérez", "Te visitamos mañana", "mar 3 nov, 10:15 a. m.", fromCustomer = false),
            ),
            detail.customer?.comments,
        )
    }

    @Test
    fun `without the thread the slot still comes from the ticket, and staff tickets have no customer side`() {
        val app = pending.copy(origin = "APP", preferredSlot = "ANY")

        assertEquals("Cualquier horario", TicketDetail.from(app, cut, emptyList(), admin, now).customer?.slot)
        assertNull(TicketDetail.from(pending, cut, emptyList(), admin, now).customer)
    }

    @Test
    fun `the app fields of the Core ticket are read`() {
        val ticket = buildApiGson().fromJson(
            """{"id":7,"name":"ANA","category":"Otros","description":"","status":"PENDING","priority":"Baja",
               "origin":"APP","preferredSlot":"AFTERNOON","preferredDate":"2026-11-10"}""",
            AssistanceTicketResponse::class.java,
        )

        assertEquals("APP", ticket.origin)
        assertEquals("AFTERNOON", ticket.preferredSlot)
        assertEquals("2026-11-10", ticket.preferredDate)
    }
}
