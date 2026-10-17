package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha

import android.app.Application
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
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h1600dp")
class FichaHistoryUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `history shows months, events and switches chips`() {
        var filter: HistoryFilter? = null
        compose.setContent {
            MaterialTheme {
                FichaHistoryTab(
                    months = listOf(
                        HistoryMonth("Set 2026", listOf(HistoryItem("15 set", "Corte automático por deuda", "Por recibo de ago impago", "Sistema", HistoryKind.NETWORK))),
                        HistoryMonth("Jul 2026", listOf(HistoryItem("12 jul", "Pago S/ 50.00 · Yape", "Recibo de jul 2026", "Rosa C.", HistoryKind.PAYMENT))),
                    ),
                    filter = HistoryFilter.ALL,
                    onFilter = { filter = it },
                )
            }
        }

        compose.onNodeWithText("Set 2026").assertExists()
        compose.onNodeWithText("15 set").assertExists()
        compose.onNodeWithText("Corte automático por deuda").assertExists()
        compose.onNodeWithText("Sistema").assertExists()
        compose.onNodeWithText("Pago S/ 50.00 · Yape").assertExists()
        compose.onNodeWithText("Cambios").performClick()

        assertEquals(HistoryFilter.CHANGES, filter)
    }

    @Test
    fun `an empty filter says so`() {
        compose.setContent { MaterialTheme { FichaHistoryTab(months = emptyList(), filter = HistoryFilter.TICKETS, onFilter = {}) } }

        compose.onNodeWithText("Sin eventos en este filtro.").assertExists()
    }
}
