package com.dscorp.ispadmin.presentation.ui.features.home

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h1400dp")
class SecretaryHomeUiTest {
    @get:Rule val compose = createComposeRule()

    private val actions = mutableListOf<HomeAction>()

    private fun show(state: HomeUiState = HomeUiState(home = SecretaryHome.from(homeSummary()))) = compose.setContent {
        MaterialTheme { SecretaryHomeContent(state, userInitials = "RC", onAction = { actions += it }, onRetry = {}) }
    }

    @Test
    fun `the header shows today's collections and the search opens subscriptions`() {
        show()

        compose.onNodeWithText("Cobrado hoy · mié 14 oct").assertExists()
        compose.onNodeWithText("S/ 1,692.00").assertExists()
        compose.onNodeWithText("▲ S/ 140.00", substring = true).assertExists()
        compose.onNodeWithText("Buscar nombre, DNI, IP o código").performClick()
        compose.onNodeWithText("RC").performClick()

        assertEquals(listOf(HomeAction.Search, HomeAction.Profile), actions)
    }

    @Test
    fun `attention rows and owing subscribers lead to their screens`() {
        show()

        compose.onNodeWithText("4 tickets pendientes").performClick()
        compose.onNodeWithText("Jorge Ramos Villanueva").performClick()
        compose.onAllNodesWithText("Cobrar")[0].performClick()

        assertEquals(listOf(HomeAction.Tickets, HomeAction.OpenSubscription(1842), HomeAction.Charge(1842)), actions)
    }

    @Test
    fun `new opens the shortcuts sheet`() {
        show()

        compose.onNodeWithTag(HomeTestTags.NEW).performClick()
        compose.onNodeWithText("Buscar titular y cobrar").assertExists()
        compose.onNodeWithText("Ticket").performClick()

        assertEquals(listOf(HomeAction.NewTicket), actions)
    }

    @Test
    fun `a failed first load offers to retry`() {
        show(HomeUiState(error = "No se pudo actualizar el inicio. Revisa tu conexión y vuelve a intentar."))

        compose.onNodeWithText("Reintentar").assertExists()
    }
}
