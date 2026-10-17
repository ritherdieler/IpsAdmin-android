package com.dscorp.ispadmin.presentation.ui.features.main

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.navigation.MainDestination
import com.dscorp.ispadmin.navigation.ShellChrome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MainShellUiTest {
    @get:Rule val compose = createComposeRule()

    private val secretaryBar = listOf(
        MainDestination.HOME, MainDestination.SUBSCRIPTIONS, MainDestination.CASH, MainDestination.TICKETS, MainDestination.MORE,
    )

    private fun show(chrome: ShellChrome, onSelect: (MainDestination) -> Unit = {}, onBack: () -> Unit = {}) =
        compose.setContent {
            MaterialTheme {
                MainShell(chrome = chrome, onDestinationSelected = onSelect, onBack = onBack) { Text("contenido") }
            }
        }

    @Test
    fun `top level screen shows the role bar and no back arrow`() {
        show(ShellChrome("Suscripciones", showBack = false, showTopBar = true, bottomBar = secretaryBar, selected = MainDestination.SUBSCRIPTIONS))

        compose.onNodeWithTag(MainShellTestTags.BOTTOM_BAR).assertExists()
        compose.onNodeWithTag(MainShellTestTags.item(MainDestination.SUBSCRIPTIONS)).assertIsSelected()
        compose.onNodeWithTag(MainShellTestTags.item(MainDestination.CASH)).assertIsNotSelected()
        compose.onNodeWithTag(MainShellTestTags.TITLE).assertExists()
        compose.onNodeWithContentDescription("Atrás").assertDoesNotExist()
        compose.onNodeWithText("contenido").assertExists()
    }

    @Test
    fun `tapping a destination selects it`() {
        var selected: MainDestination? = null
        show(
            ShellChrome("Inicio", showBack = false, showTopBar = true, bottomBar = secretaryBar, selected = MainDestination.HOME),
            onSelect = { selected = it },
        )

        compose.onNodeWithTag(MainShellTestTags.item(MainDestination.CASH)).performClick()

        assertEquals(MainDestination.CASH, selected)
    }

    @Test
    fun `secondary screen shows a back arrow and hides the bar`() {
        var backPressed = false
        show(
            ShellChrome("Ficha de suscripción", showBack = true, showTopBar = true, bottomBar = emptyList(), selected = null),
            onBack = { backPressed = true },
        )

        compose.onNodeWithTag(MainShellTestTags.BOTTOM_BAR).assertDoesNotExist()
        compose.onNodeWithText("Ficha de suscripción").assertExists()
        compose.onNodeWithContentDescription("Atrás").performClick()
        assertTrue(backPressed)
    }

    @Test
    fun `screens with their own header hide the top bar`() {
        show(ShellChrome("Inicio", showBack = false, showTopBar = false, bottomBar = secretaryBar, selected = MainDestination.HOME))

        compose.onNodeWithTag(MainShellTestTags.TITLE).assertDoesNotExist()
        compose.onNodeWithTag(MainShellTestTags.BOTTOM_BAR).assertExists()
    }
}
