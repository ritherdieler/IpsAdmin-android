package com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus
import com.dscorp.ispadmin.domain.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h1600dp")
class CloseTicketTest {
    @get:Rule val compose = createComposeRule()

    private val dispatcher = StandardTestDispatcher()
    private val gateway = mockk<CloseTicketGateway>()
    private val ticket = AssistanceTicketResponse(
        id = 3606, name = "ELENA FLORES RIOS", category = "Internet lento", description = "", status = AssistanceTicketStatus.ASSIGNED,
        priority = "Media", sheetImageUrl = null, place = "la merced",
    )

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `closing needs the signed order photo and then closes the ticket`() = runTest(dispatcher) {
        every { gateway.currentUser() } returns User(id = 4, name = "Diego")
        coEvery { gateway.ticket(3606) } returns ticket
        val photo = File.createTempFile("orden", ".jpg")
        coEvery { gateway.close(3606, 4, photo) } returns Unit
        val viewModel = CloseTicketViewModel(gateway, mainImmediate = dispatcher)
        val events = mutableListOf<CloseTicketEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events += it } }

        viewModel.onIntent(CloseTicketIntent.Load(3606))
        advanceUntilIdle()
        assertEquals("Internet lento · Elena Flores Rios · La Merced", viewModel.uiState.value.subtitle)
        viewModel.onIntent(CloseTicketIntent.Submit)
        advanceUntilIdle()
        coVerify(exactly = 0) { gateway.close(any(), any(), any()) }

        viewModel.onIntent(CloseTicketIntent.PhotoTaken(photo))
        viewModel.onIntent(CloseTicketIntent.Submit)
        advanceUntilIdle()

        assertEquals(listOf(CloseTicketEvent.Closed("Ticket #3606 cerrado")), events)
    }

    @Test
    fun `the close button waits for the photo`() {
        var taken = false
        compose.setContent {
            MaterialTheme {
                CloseTicketContent(
                    CloseTicketUiState(ticketId = 3606, subtitle = "Internet lento · Elena Flores Rios"),
                    onIntent = {}, onTakePhoto = { taken = true }, onBack = {},
                )
            }
        }

        compose.onNodeWithText("Cerrar ticket #3606").assertExists()
        compose.onNodeWithText("Orden firmada *").assertExists()
        compose.onNodeWithText("Cerrar ticket").assertIsNotEnabled()
        compose.onNodeWithText("Tomar foto").performClick()
        assertEquals(true, taken)
    }

    @Test
    fun `with the photo the ticket can be closed`() {
        compose.setContent {
            MaterialTheme {
                CloseTicketContent(
                    CloseTicketUiState(ticketId = 3606, subtitle = "", photo = File("orden.jpg"), photoTakenAt = "10:46"),
                    onIntent = {}, onTakePhoto = {}, onBack = {},
                )
            }
        }

        compose.onNodeWithText("Foto lista").assertExists()
        compose.onNodeWithText("Tomada 10:46").assertExists()
        compose.onNodeWithText("Repetir").assertExists()
        compose.onNodeWithText("Cerrar ticket").assertIsEnabled()
    }
}
