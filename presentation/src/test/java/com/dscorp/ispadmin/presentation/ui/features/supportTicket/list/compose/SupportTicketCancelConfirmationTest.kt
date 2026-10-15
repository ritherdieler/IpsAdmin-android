package com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus
import com.dscorp.ispadmin.domain.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h1600dp")
class SupportTicketCancelConfirmationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val cancelled = mutableListOf<AssistanceTicketResponse>()

    @Test
    fun `cancel asks for confirmation before cancelling a pending ticket`() {
        setScreen()

        composeRule.onNodeWithText("Cancelar").performClick()

        assertTrue(cancelled.isEmpty())
        composeRule.onNodeWithText("¿Cancelar ticket #3213?").assertExists()
    }

    @Test
    fun `confirming cancels the ticket once`() {
        setScreen()

        composeRule.onNodeWithText("Cancelar").performClick()
        composeRule.onNodeWithText("Sí, cancelar").performClick()

        assertEquals(listOf(3213), cancelled.map { it.id })
        composeRule.onNodeWithText("¿Cancelar ticket #3213?").assertDoesNotExist()
    }

    @Test
    fun `dismissing the confirmation keeps the ticket`() {
        setScreen()

        composeRule.onNodeWithText("Cancelar").performClick()
        composeRule.onNodeWithText("No").performClick()

        assertTrue(cancelled.isEmpty())
        composeRule.onNodeWithText("¿Cancelar ticket #3213?").assertDoesNotExist()
    }

    private fun setScreen() {
        composeRule.setContent {
            MaterialTheme {
                SupportTicketListScreen(
                    uiState = SupportTicketListUiState(
                        user = User(id = 1, name = "Ana", lastName = "Secretaria", type = User.UserType.SECRETARY),
                        pendingTickets = listOf(pendingTicket()),
                    ),
                    onTabChange = {},
                    onDateFilterChange = {},
                    onSortOptionChange = {},
                    onTakeTicket = {},
                    onCloseUnattendedTicket = { cancelled += it },
                    onCloseTicket = { _, _ -> },
                    onTicketCardClick = {},
                    onRefresh = {},
                    onDismissError = {},
                    onRescheduleTicket = { _, _ -> },
                    onDismissSuccessMessage = {},
                )
            }
        }
    }

    private fun pendingTicket() = AssistanceTicketResponse(
        id = 3213,
        name = "PATRICIA CARILLO CUITANA",
        phone = "946734696",
        category = "Sin Conexión a Internet",
        description = "Sin servicio",
        status = AssistanceTicketStatus.PENDING,
        priority = "Alta",
        place = "la villa",
        sheetImageUrl = null
    )
}
