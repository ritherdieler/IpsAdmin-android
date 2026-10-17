package com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.InfoRow
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.TicketRow
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose.TicketAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h2000dp")
class TicketDetailUiTest {
    @get:Rule val compose = createComposeRule()

    private val pending = TicketDetail(
        id = 3609, title = "Ticket #3609",
        badges = listOf(StatusBadge("Pendiente", BadgeTone.WARNING), StatusBadge("▲ Alta", BadgeTone.ERROR)),
        category = "Sin conexión a internet", meta = "Creado hace 2 h",
        debtNotice = DebtNotice("Pedro está cortado por deuda: debe S/ 50.00. Cobrar restablece el servicio y podría resolver el ticket.", "Cobrar S/ 50.00"),
        subscriber = SubscriberCard("PS", "Pedro Sánchez Chávez", "Basico · Las Colinas", StatusBadge("Cortado", BadgeTone.WARNING), "S/ 50.00", owes = true),
        description = "No tiene internet desde ayer.", contact = "Contacto: 964 818 302 · titular", assignment = "Sin asignar",
        previous = listOf(TicketRow(3471, "Internet lento", "#3471 · 22 ago · Marco Salinas", StatusBadge("Cerrado", BadgeTone.NEUTRAL))),
        primary = TicketAction.TAKE, primaryHint = "Pasa a En curso y queda a tu nombre.",
        canAssign = true, canReschedule = true, canCancel = true,
    )
    private val intents = mutableListOf<TicketDetailIntent>()
    private val actions = mutableListOf<String>()

    private fun show(state: TicketDetailUiState) = compose.setContent {
        MaterialTheme {
            TicketDetailContent(
                state = state,
                onIntent = { intents += it },
                onBack = { actions += "back" },
                onCharge = { actions += "charge" },
                onOpenSubscription = { actions += "subscription" },
                onCall = { actions += "call" },
                onWhatsApp = { actions += "whatsapp" },
                onClose = { actions += "close" },
                onOpenTicket = { actions += "ticket $it" },
            )
        }
    }

    @Test
    fun `pending ticket reads like the design and takes it`() {
        show(TicketDetailUiState(detail = pending, subscriptionId = 1842))

        compose.onNodeWithText("Ticket #3609").assertExists()
        compose.onNodeWithText("Sin conexión a internet").assertExists()
        compose.onNodeWithText("Creado hace 2 h").assertExists()
        compose.onNodeWithText("Pedro Sánchez Chávez").assertExists()
        compose.onNodeWithText("Contacto: 964 818 302 · titular").assertExists()
        compose.onNodeWithText("Tickets anteriores").assertExists()
        compose.onNodeWithText("Pasa a En curso y queda a tu nombre.").assertExists()

        compose.onNodeWithText("Cobrar S/ 50.00").performClick()
        compose.onNodeWithText("Llamar").performClick()
        compose.onNodeWithText("WhatsApp").performClick()
        compose.onNodeWithText("Internet lento").performClick()
        compose.onNodeWithText("Asignar").performClick()
        compose.onNodeWithText("Tomar ticket").performClick()

        assertEquals(listOf("charge", "call", "whatsapp", "ticket 3471"), actions)
        assertEquals(listOf(TicketDetailIntent.OpenAssign, TicketDetailIntent.Take), intents)
    }

    @Test
    fun `taken ticket shows the diagnosis and closes or reschedules`() {
        show(
            TicketDetailUiState(
                detail = pending.copy(
                    badges = listOf(StatusBadge("En curso", BadgeTone.INFO)), meta = "Tomado por Diego Huerta a las 09:40 · 1 h 2 min",
                    debtNotice = null, primary = TicketAction.CLOSE, primaryHint = null, canAssign = false,
                ),
                subscriptionId = 1842,
                diagnosis = TicketDiagnosis(listOf(InfoRow("Potencia", "−24.9 dBm · aceptable")), unavailable = false),
            )
        )

        compose.onNodeWithText("Diagnóstico rápido").assertExists()
        compose.onNodeWithText("−24.9 dBm · aceptable").assertExists()
        compose.onNodeWithText("Reiniciar ONU").performClick()
        compose.onAllNodesWithText("Reiniciar")[0].performClick()
        compose.onNodeWithText("Resolver y cerrar").performClick()
        compose.onNodeWithText("Reagendar").performClick()
        compose.onNodeWithText("Reagendar #3609").assertExists()

        assertEquals(listOf(TicketDetailIntent.RebootOnu), intents)
        assertEquals(listOf("close"), actions)
    }

    @Test
    fun `assigning opens a sheet with the technicians`() {
        show(TicketDetailUiState(detail = pending, technicians = listOf(TechnicianOption(7, "Marco Salinas"))))

        compose.onNodeWithText("Asignar a").assertExists()
        compose.onNodeWithText("Marco Salinas").assertExists()
    }

    @Test
    fun `choosing a technician assigns the ticket`() {
        compose.setContent { MaterialTheme { TechnicianList(listOf(TechnicianOption(7, "Marco Salinas"))) { intents += TicketDetailIntent.Assign(it) } } }

        compose.onNodeWithText("Marco Salinas").performClick()

        assertTrue(TicketDetailIntent.Assign(7) in intents)
    }

    @Test
    fun `reschedule offers days and hours and says when`() {
        val now = LocalDateTime.of(2026, 10, 14, 10, 42)
        val options = rescheduleDays(now.toLocalDate())

        assertEquals(listOf("Hoy", "Mañana jue 15", "Vie 16 oct"), options.map { it.label })
        assertEquals(listOf("09:00", "11:00", "14:00", "16:00"), RESCHEDULE_HOURS.map { it.label })
        assertEquals("Reagendar para jue 15 · 11:00", rescheduleLabel(options[1].day, RESCHEDULE_HOURS[1].time, now.toLocalDate()))
    }
}
