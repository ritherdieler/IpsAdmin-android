package com.dscorp.ispadmin.presentation.ui.features.outlay

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.domain.model.Outlay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h1600dp")
class OutlayUiTest {
    @get:Rule val compose = createComposeRule()
    private val intents = mutableListOf<OutlayIntent>()

    @Test
    fun `the new outlay picks category and cost centre with chips and asks for the receipt photo`() {
        compose.setContent {
            MaterialTheme {
                RegisterOutLayForm(
                    outlay = Outlay(amount = "85", description = "Combustible", category = "Transporte", cost_center = "Técnico"),
                    photoList = emptyList(),
                    isLoading = false,
                    onIntent = { intents += it },
                    onImageClick = {},
                    today = LocalDate.of(2026, 10, 14),
                )
            }
        }

        compose.onNodeWithText("Hoy, 14 oct 2026").assertExists()
        compose.onNodeWithText("Falta la foto del comprobante").assertExists()
        compose.onNodeWithText("Registrar egreso").assertIsNotEnabled()
        compose.onNodeWithText("Suministros").performClick()
        compose.onNodeWithText("Administración").performClick()

        assertEquals(listOf(OutlayIntent.UpdateCategory("Suministros"), OutlayIntent.UpdateCostCenter("Administración")), intents)
    }

    @Test
    fun `the month list filters by category and offers a new outlay`() {
        var created = false
        var filter: String? = "unset"
        compose.setContent {
            MaterialTheme {
                OutlaysContent(OutlaysUiState(month = OutlayMonth.from(OCTOBER, null, LocalDate.of(2026, 10, 14))),
                    onFilter = { filter = it }, onNew = { created = true }, onRetry = {})
            }
        }

        compose.onNodeWithText("S/ 469.00").assertExists()
        compose.onNodeWithText("Combustible de la moto").assertExists()
        compose.onNodeWithText("Transporte").performClick()
        compose.onNodeWithTag(OutlayTestTags.NEW).performClick()

        assertEquals("Transporte", filter)
        assertTrue(created)
    }
}
