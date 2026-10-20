package com.dscorp.ispadmin.presentation.ui.features.home

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.data.model.UserDto
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus
import com.dscorp.ispadmin.domain.model.User
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h1600dp")
class FieldUiTest {
    @get:Rule val compose = createComposeRule()

    private val actions = mutableListOf<FieldAction>()
    private val instant = NOW.atZone(LIMA).toInstant()
    private val me = User(id = 7, name = "Marco", lastName = "Salinas", type = User.UserType.TECHNICIAN)

    private val today = TechnicianToday.from(
        me,
        listOf(
            ticket(3612, category = "Instalación de Internet", status = AssistanceTicketStatus.ASSIGNED, assignedTo = "Marco Salinas",
                name = "LUCIA MENDOZA ROJAS", place = "la villa", address = "Calle Las Flores 88", scheduled = NOW.withHour(10).withMinute(30)),
            ticket(3609, priority = "Alta"),
        ),
        listOf(ticket(3500, status = AssistanceTicketStatus.CLOSED, assignedTo = "Marco Salinas", closed = NOW.minusHours(1), name = "ANA PEREZ")),
        listOf(order(6, technician = UserDto(id = 7), scheduled = NOW.withHour(15))),
        0, instant,
    )

    @Test
    fun `today shows the current job with its actions and the pending list`() {
        compose.setContent { MaterialTheme { TechnicianTodayContent(FieldUiState(today = today, initials = "MS"), onAction = { actions += it }, onRetry = {}) } }

        compose.onNodeWithText("Hoy").assertExists()
        compose.onNodeWithText("mié 14 oct · 2 trabajos").assertExists()
        compose.onNodeWithText("Todo enviado", substring = true).assertDoesNotExist()
        compose.onNodeWithText("sin enviar", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Cómo llegar").performClick()
        compose.onNodeWithText("Registrar suscripción").performClick()
        compose.onNodeWithText("Sin conexión a internet").performClick()

        assertEquals(
            listOf(
                FieldAction.Directions("Calle Las Flores 88, La Villa"),
                FieldAction.Register(TodayTarget.Ticket(3612)),
                FieldAction.Open(TodayTarget.Ticket(3609)),
            ),
            actions,
        )
    }

    @Test
    fun `the uploads banner opens the pending screen`() {
        val withPending = today.copy(uploads = UploadBanner("2 registros sin enviar", "toca para enviarlos"))
        compose.setContent { MaterialTheme { TechnicianTodayContent(FieldUiState(today = withPending, initials = "MS"), onAction = { actions += it }, onRetry = {}) } }

        compose.onNodeWithText("2 registros sin enviar · toca para enviarlos").performClick()

        assertEquals(listOf<FieldAction>(FieldAction.PendingUploads), actions)
    }

    @Test
    fun `done today expands`() {
        compose.setContent { MaterialTheme { TechnicianTodayContent(FieldUiState(today = today, initials = "MS"), onAction = { actions += it }, onRetry = {}) } }

        compose.onNodeWithText("Ana Perez · Las Colinas").assertDoesNotExist()
        compose.onNodeWithText("Hechos hoy (1)").performClick()
        compose.onNodeWithText("Ana Perez · Las Colinas").assertExists()
    }

    @Test
    fun `sales agenda filters and creates orders`() {
        val agenda = SalesAgenda.from(
            listOf(order(7)),
            listOf(ticket(3598, category = "Instalación de Internet", name = "ANA PEREZ")),
            instant, AgendaFilter.ALL,
        )
        compose.setContent {
            MaterialTheme { SalesAgendaContent(FieldUiState(agenda = agenda, initials = "KV"), onFilter = { actions += FieldAction.Filtered(it) }, onAction = { actions += it }, onRetry = {}) }
        }

        compose.onNodeWithText("Instalaciones").assertExists()
        compose.onNodeWithText("Por asignar · 2").performClick()
        compose.onNodeWithText("Ana Perez").performClick()
        compose.onNodeWithTag(FieldTestTags.NEW_ORDER).performClick()

        assertEquals(
            listOf(FieldAction.Filtered(AgendaFilter.UNASSIGNED), FieldAction.Open(TodayTarget.Ticket(3598)), FieldAction.NewOrder),
            actions,
        )
    }
}
