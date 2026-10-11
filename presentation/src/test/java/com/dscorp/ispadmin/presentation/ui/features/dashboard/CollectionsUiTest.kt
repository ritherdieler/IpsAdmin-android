package com.dscorp.ispadmin.presentation.ui.features.dashboard

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.dscorp.ispadmin.domain.model.dashboard.ClientsDashboard
import com.dscorp.ispadmin.domain.model.dashboard.CollectionSummary
import com.dscorp.ispadmin.domain.model.financial.FinancialReport
import com.dscorp.ispadmin.presentation.ui.features.dashboard.clients.ClientsContent
import com.dscorp.ispadmin.presentation.ui.features.dashboard.clients.ClientsUiState
import com.dscorp.ispadmin.presentation.ui.features.financial.FinancialContent
import com.dscorp.ispadmin.presentation.ui.features.financial.FinancialUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CollectionsUiTest {
    @get:Rule val compose = createComposeRule()

    private val summary = CollectionSummary(7, BigDecimal("6840.00"), 214, 9, 3, 90, 72.0, 46, 39, BigDecimal("9412.00"), 287, BigDecimal("32.79"))

    @Test fun `movements view shows collection follow-up`() {
        compose.setContent {
            MaterialTheme {
                FinancialContent(FinancialUiState(serviceMonth = "2026-09", movementsMonth = "2026-10", report = FinancialReport(id = "cut-1")),
                    {}, {}, collections = summary)
            }
        }

        compose.onNodeWithTag("financial_list").performScrollToNode(hasTestTag("financial_collections"))
        compose.onNodeWithText("214 facturas", substring = true).assertExists()
        compose.onNodeWithText("3 vencen hoy", substring = true).assertExists()
        compose.onNodeWithText("72.0% cumplidas", substring = true).assertExists()
    }

    @Test fun `clients tab shows overdue debt per delinquent subscription`() {
        val dashboard = ClientsDashboard(null, null, null, null, null)
        compose.setContent { MaterialTheme { ClientsContent(ClientsUiState(dashboard = dashboard), {}, {}, collections = summary) } }

        compose.onNodeWithTag("clients_debt_per_delinquent_value", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("287 clientes morosos", substring = true).assertExists()
    }
}
