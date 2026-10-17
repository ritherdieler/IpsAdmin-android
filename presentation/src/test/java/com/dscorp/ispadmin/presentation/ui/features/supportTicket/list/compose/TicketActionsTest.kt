package com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.model.User.UserType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h1600dp")
class TicketActionsTest {
    @get:Rule val compose = createComposeRule()

    private fun user(type: UserType) = User(id = 1, name = "Ana", type = type)

    private fun ticket(status: AssistanceTicketStatus) = AssistanceTicketResponse(
        id = 3609, name = "PEDRO SANCHEZ CHAVEZ", phone = "964818302", category = "Sin Conexión a Internet",
        description = "Sin internet desde ayer", status = status, priority = "Alta", place = "las colinas", sheetImageUrl = null,
    )

    @Test
    fun `each state has at most one primary action and the rest go to the menu`() {
        val pending = ticket(AssistanceTicketStatus.PENDING)
        assertEquals(TicketActions(TicketAction.TAKE, emptyList()), ticketActions(pending, user(UserType.TECHNICIAN)))
        assertEquals(TicketActions(TicketAction.TAKE, listOf(TicketAction.RESCHEDULE, TicketAction.CANCEL)), ticketActions(pending, user(UserType.ADMIN)))
        assertEquals(TicketActions(null, listOf(TicketAction.RESCHEDULE, TicketAction.CANCEL)), ticketActions(pending, user(UserType.SECRETARY)))

        val assigned = ticket(AssistanceTicketStatus.ASSIGNED)
        assertEquals(TicketActions(TicketAction.CLOSE, emptyList()), ticketActions(assigned, user(UserType.TECHNICIAN)))
        assertEquals(TicketActions(TicketAction.CLOSE, listOf(TicketAction.RESCHEDULE)), ticketActions(assigned, user(UserType.ADMIN)))

        assertNull(ticketActions(ticket(AssistanceTicketStatus.CLOSED), user(UserType.ADMIN)).primary)
    }

    @Test
    fun `the card shows one primary button and keeps the others in the menu`() {
        compose.setContent { MaterialTheme { TicketCard(ticket(AssistanceTicketStatus.PENDING), user(UserType.ADMIN)) } }

        compose.onNodeWithText("Tomar").assertExists()
        compose.onNodeWithText("Reagendar").assertDoesNotExist()
        compose.onNodeWithContentDescription("Más acciones del ticket").performClick()
        compose.onNodeWithText("Reagendar").assertExists()
        compose.onNodeWithText("Cancelar").assertExists()
    }

    @Test
    fun `dates are written in spanish whatever the device language`() {
        val date = Calendar.getInstance(TimeZone.getTimeZone("America/Lima")).apply {
            set(2026, Calendar.OCTOBER, 9, 12, 16, 0)
        }.time

        assertEquals("9 oct 2026 · 12:16", ticketDateLabel(date, TimeZone.getTimeZone("America/Lima")))
    }
}
