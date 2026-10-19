package com.dscorp.ispadmin.presentation.designsystem.components

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ScreenStatesTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `the error state says what failed and retries`() {
        var retried = 0
        compose.setContent {
            MaterialTheme { ErrorState("No se pudieron cargar los tickets", "Revisa tu conexión y vuelve a intentar.", onRetry = { retried++ }, lastAttempt = "10:42") }
        }

        compose.onNodeWithText("No se pudieron cargar los tickets").assertExists()
        compose.onNodeWithText("Último intento 10:42").assertExists()
        compose.onNodeWithText("Reintentar").performClick()
        assertEquals(1, retried)
    }

    @Test
    fun `the empty state can point somewhere else`() {
        var acted = false
        compose.setContent {
            MaterialTheme { EmptyState(Icons.Rounded.ConfirmationNumber, "No hay tickets en curso", "Los tickets que tomes aparecen aquí.", "Ver pendientes (4)") { acted = true } }
        }

        compose.onNodeWithText("Los tickets que tomes aparecen aquí.").assertExists()
        compose.onNodeWithText("Ver pendientes (4)").performClick()
        assertEquals(true, acted)
    }

    @Test
    fun `the stale banner keeps the data and offers to retry`() {
        var retried = false
        compose.setContent { MaterialTheme { StaleBanner("No se pudo actualizar", onRetry = { retried = true }) } }

        compose.onNodeWithText("No se pudo actualizar").assertExists()
        compose.onNodeWithText("Reintentar").performClick()
        assertEquals(true, retried)
    }
}
