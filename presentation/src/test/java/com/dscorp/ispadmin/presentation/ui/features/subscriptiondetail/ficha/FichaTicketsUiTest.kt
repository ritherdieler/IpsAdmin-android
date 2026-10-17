package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h1600dp")
class FichaTicketsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `tickets tab opens a ticket and creates a new one`() {
        var opened: Int? = null
        var created = false
        compose.setContent {
            MaterialTheme {
                FichaTicketsTab(
                    FichaTickets(1, listOf(TicketRow(3402, "Internet lento", "#3402 · 2 jul 2026 · Diego Huerta", StatusBadge("Cerrado", BadgeTone.NEUTRAL)))),
                    onNewTicket = { created = true },
                    onOpenTicket = { opened = it },
                )
            }
        }

        compose.onNodeWithText("#3402 · 2 jul 2026 · Diego Huerta").assertExists()
        compose.onNodeWithText("Internet lento").performClick()
        compose.onNodeWithText("Nuevo ticket").performClick()

        assertEquals(3402, opened)
        assertTrue(created)
    }

    @Test
    fun `no tickets and a failed load read differently`() {
        compose.setContent { MaterialTheme { FichaTicketsTab(FichaTickets(0, emptyList()), onNewTicket = {}, onOpenTicket = {}) } }
        compose.onNodeWithText("Esta suscripción no tiene tickets.").assertExists()
    }

    @Test
    fun `tab label shows open tickets`() {
        compose.setContent { MaterialTheme { FichaTabs(FichaTab.RESUMEN, onSelect = {}, ticketsCount = 1) } }
        compose.onNodeWithText("Tickets 1").assertExists()
    }
}
