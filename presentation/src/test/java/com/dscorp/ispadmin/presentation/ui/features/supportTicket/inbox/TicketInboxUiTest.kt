package com.dscorp.ispadmin.presentation.ui.features.supportTicket.inbox

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h1600dp")
class TicketInboxUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `list reads like the design and opens tickets`() {
        val intents = mutableListOf<TicketInboxIntent>()
        var opened: Int? = null
        var created = false
        compose.setContent {
            MaterialTheme {
                TicketInboxContent(
                    state = TicketInboxUiState(
                        counts = mapOf(TicketTab.PENDING to 4L, TicketTab.IN_PROGRESS to 1L),
                        summary = "4 pendientes · prioridad alta primero",
                        rows = listOf(
                            TicketInboxRow(3609, StatusBadge("▲ Alta", BadgeTone.ERROR), "#3609", "hace 2 h", "Sin conexión a internet",
                                "Pedro Sánchez Chávez · Las Colinas", null, "Sin asignar", assigned = false),
                        ),
                    ),
                    onIntent = { intents += it },
                    onOpen = { opened = it },
                    onNew = { created = true },
                )
            }
        }

        compose.onNodeWithText("Pendientes").assertExists()
        compose.onNodeWithText("4").assertExists()
        compose.onNodeWithText("4 pendientes · prioridad alta primero").assertExists()
        compose.onNodeWithText("Pedro Sánchez Chávez · Las Colinas").assertExists()
        compose.onNodeWithText("Sin conexión a internet").performClick()
        compose.onNodeWithText("Prioridad alta").performClick()
        compose.onAllNodesWithText("Sin asignar").assertCountEquals(2)
        compose.onNodeWithText("En curso").performClick()
        compose.onNodeWithText("Nuevo ticket", useUnmergedTree = true).performClick()

        assertEquals(3609, opened)
        assertEquals(listOf(TicketInboxIntent.HighPriorityToggled, TicketInboxIntent.TabSelected(TicketTab.IN_PROGRESS)), intents)
        assertEquals(true, created)
    }

    @Test
    fun `an empty tab says so`() {
        compose.setContent { MaterialTheme { TicketInboxContent(TicketInboxUiState(summary = "0 en curso", tab = TicketTab.IN_PROGRESS), {}, {}, {}) } }
        compose.onNodeWithText("No hay tickets en esta pestaña.").assertExists()
    }
}
