package com.dscorp.ispadmin.presentation.ui.features.supportTicket.create

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionFastSearchResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h2000dp")
class CreateTicketUiTest {
    @get:Rule val compose = createComposeRule()

    private val client = CreateSupportTicketUiState(
        selectedSubscription = SubscriptionFastSearchResponse(id = 10, fullName = "Gloria Ticona Apaza"),
        hasLoadedClientDetails = true,
        clientName = "GLORIA TICONA APAZA",
        clientPlan = "Duo basico 80",
        clientLocation = "la villa",
        clientStatus = ServiceStatus.ACTIVE,
        phone = "976333444",
        technicians = listOf(7 to "Marco Salinas"),
    )

    private var category: String? = null
    private var cleared = false
    private var assignee: Int? = null
    private var created = false

    private fun show(state: CreateSupportTicketUiState) = compose.setContent {
        MaterialTheme {
            CreateSupportTicketScreen(
                uiState = state,
                onPhoneChange = {},
                onCategoryChange = { category = it },
                onDescriptionChange = {},
                onIsClientChange = {},
                onPlaceSelected = {},
                onSubscriptionSelected = { if (it == null) cleared = true },
                onSearchTextChange = {},
                onCustomerNameChange = {},
                onAssigneeChange = { assignee = it },
                onCreateTicket = { created = true },
                onDismissError = {},
                onNavigateBack = {},
            )
        }
    }

    @Test
    fun `category labels read in sentence case and keep TV and ONU`() {
        assertEquals("No tiene señal de TV cable", categoryLabel("No tiene señal de Tv Cable"))
        assertEquals("Cambio de ONU", categoryLabel("Cambio de Onu"))
        assertEquals("Alineamiento de antena CPE", categoryLabel("Alineamiento de antena CPE"))
    }

    @Test
    fun `the chosen subscriber shows as a card that can be changed`() {
        show(client)

        compose.onNodeWithText("✓ Abonado").assertExists()
        compose.onNodeWithText("Gloria Ticona Apaza").assertExists()
        compose.onNodeWithText("Duo basico 80 · La Villa").assertExists()
        compose.onNodeWithText("Activo").assertExists()
        compose.onNodeWithText("Del titular. Puedes cambiarlo.").assertExists()
        compose.onNodeWithText("Cambiar").performClick()

        assertTrue(cleared)
    }

    @Test
    fun `the most used categories show their priority and the rest live in a sheet`() {
        show(client)

        compose.onNodeWithText("Sin conexión a internet").assertExists()
        compose.onNodeWithText("Cambio de router").assertDoesNotExist()
        compose.onNodeWithText("Ver las 16 categorías").performScrollTo().performClick()
        compose.onNodeWithTag(CreateTicketTestTags.CATEGORY_SHEET).assertExists()
        compose.onNodeWithText("Cambio de router").performScrollTo().performClick()

        assertEquals("Cambio de Router", category)
    }

    @Test
    fun `a chosen category explains where the priority comes from and the description counts characters`() {
        show(client.copy(category = "Sin Conexión a Internet", description = "Sin internet"))

        compose.onNodeWithText("Prioridad:").assertExists()
        compose.onNodeWithText("· según la categoría").assertExists()
        compose.onNodeWithText("12/300").assertExists()
    }

    @Test
    fun `assigning is optional and create sends the form`() {
        show(client)

        compose.onNodeWithText("Sin asignar").performScrollTo().performClick()
        compose.onNodeWithText("Marco Salinas").performClick()
        compose.onNodeWithText("Crear ticket").performClick()

        assertEquals(7, assignee)
        assertTrue(created)
    }
}
