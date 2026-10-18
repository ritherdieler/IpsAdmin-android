package com.dscorp.ispadmin.presentation.ui.features.home

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.dscorp.ispadmin.domain.model.financial.FinancialReport
import com.dscorp.ispadmin.domain.model.financial.MovementSummary
import com.dscorp.ispadmin.presentation.ui.features.financial.FinancialContent
import com.dscorp.ispadmin.presentation.ui.features.financial.FinancialUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h2400dp")
class AdminHomeUiTest {
    @get:Rule val compose = createComposeRule()

    private val actions = mutableListOf<AdminHomeAction>()

    private fun show() = compose.setContent {
        MaterialTheme {
            FinancialContent(
                FinancialUiState(
                    serviceMonth = "2026-09", movementsMonth = "2026-10",
                    report = FinancialReport(id = "cut-1", cutoff = "2026-10-14T10:00:00-05:00",
                        movements = MovementSummary("50.00".toBigDecimal(), null, null, "0".toBigDecimal())),
                ),
                {}, {},
                home = AdminHome.from(adminSummary()),
                onHomeAction = { actions += it },
            )
        }
    }

    @Test
    fun `the home summary sits on top and the financial report stays below`() {
        show()

        compose.onNodeWithText("Corte 15 oct · actualizado 10:42").assertExists()
        compose.onNodeWithText("Cobrado en oct (al día 14)").assertExists()
        compose.onNodeWithText("de 916 activos").assertExists()
        compose.onNodeWithText("Cobros por día · oct").assertExists()
        compose.onNodeWithTag("financial_list").performScrollToNode(hasTestTag("financial_received"))
        compose.onNodeWithTag("financial_received").assertExists()
        compose.onNodeWithTag("financial_cut").assertExists()
    }

    @Test
    fun `a summary arriving after the report is shown at the top`() {
        var home by mutableStateOf<AdminHome?>(null)
        compose.setContent {
            MaterialTheme {
                FinancialContent(
                    FinancialUiState(serviceMonth = "2026-09", movementsMonth = "2026-10",
                        report = FinancialReport(id = "cut-1", movements = MovementSummary("50.00".toBigDecimal(), null, null, "0".toBigDecimal()))),
                    {}, {}, home = home,
                )
            }
        }
        compose.waitForIdle()
        home = AdminHome.from(adminSummary())

        compose.onNodeWithText("Cobrado en oct (al día 14)").assertIsDisplayed()
    }

    @Test
    fun `tiles and see cash lead to their screens`() {
        show()

        compose.onNodeWithText("Tickets pendientes").performClick()
        compose.onNodeWithText("Ver caja").performClick()

        assertEquals(listOf(AdminHomeAction.Tile(AdminTileKind.TICKETS), AdminHomeAction.Cash), actions)
    }
}
