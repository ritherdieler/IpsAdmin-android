package com.dscorp.ispadmin.presentation.ui.features.dashboard

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertTextEquals
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.domain.model.CancellationResumeDto
import com.dscorp.ispadmin.domain.model.DashBoardDataResponse
import com.dscorp.ispadmin.domain.model.EconomicResume
import com.dscorp.ispadmin.domain.model.MonthlyDetail
import com.dscorp.ispadmin.domain.model.MonthlyGrossRevenueResume
import com.dscorp.ispadmin.domain.model.SubscriptionLogSummary
import com.dscorp.ispadmin.domain.model.SubscriptionsResumeStatics
import com.dscorp.ispadmin.domain.model.dashboard.ClientLifecycle
import com.dscorp.ispadmin.domain.model.dashboard.ClientsDashboard
import com.dscorp.ispadmin.domain.model.dashboard.TicketSummary
import com.dscorp.ispadmin.domain.model.dashboard.ZonePerformance
import com.dscorp.ispadmin.domain.model.financial.BillingSummary
import com.dscorp.ispadmin.domain.model.financial.FinancialGroup
import com.dscorp.ispadmin.domain.model.financial.FinancialReport
import com.dscorp.ispadmin.domain.model.financial.MovementSummary
import com.dscorp.ispadmin.domain.model.financial.ServiceHistory
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
import java.time.YearMonth
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DashboardDesignUiTest {
    @get:Rule val compose = createComposeRule()

    private val categories = listOf("Sin internet" to 248, "Otros" to 247, "Lento" to 139, "Instalación" to 102,
        "Sin señal TV" to 28, "Contraseña" to 19, "Evaluar factibilidad" to 1)
    private val tickets = TicketSummary(863, 9, 0, 0, 0, 18.4, 0.0, categories, listOf(0, 339, 19), openHighPriority = 3)
    private val zones = (1..7).map { ZonePerformance("Zona $it", 300 - it * 10, 1_000.0 * it, 5.0) }
    private val lifecycle = ClientLifecycle(47.0, 412.0,
        listOf("basico" to 41, "wireless 50" to 31, "duo 80" to 21, "cable" to 8, "Internet70" to 6, "Cabinas" to 1), emptyList())

    private fun showClients() = compose.setContent {
        MaterialTheme { ClientsContent(ClientsUiState(dashboard = ClientsDashboard(null, tickets, null, zones, lifecycle)), {}, {}) }
    }

    @Test fun `clients hero flags open tickets of high priority`() {
        showClients()

        compose.onNodeWithText("3 de prioridad alta", substring = true).assertExists()
    }

    @Test fun `ticket categories show the largest ones and fold the rest`() {
        showClients()

        compose.onNodeWithTag("clients_list").performScrollToNode(hasTestTag("clients_tickets_category"))
        compose.onNodeWithText("Contraseña").assertDoesNotExist()
        compose.onNodeWithText("Evaluar factibilidad").assertDoesNotExist()
        // The Core already has an "Otros" category; the folded rest (19 + 1) joins it at the end.
        compose.onNodeWithText("267").assertExists()
    }

    @Test fun `zone table keeps the largest zones`() {
        showClients()

        compose.onNodeWithTag("clients_list").performScrollToNode(hasTestTag("clients_zones"))
        compose.onNodeWithText("Zona 5").assertExists()
        compose.onNodeWithText("Zona 7").assertDoesNotExist()
    }

    @Test fun `cancellations by plan fold small plans into others`() {
        showClients()

        compose.onNodeWithTag("clients_list").performScrollToNode(hasTestTag("clients_cancel_by_plan"))
        compose.onNodeWithText("Cabinas").assertDoesNotExist()
        compose.onNodeWithText("Otros").assertExists()
    }

    private val currentMonth = YearMonth.now(ZoneId.of("America/Lima"))

    private fun log(action: String, current: Int) = action to SubscriptionLogSummary(action, current + 5,
        listOf(MonthlyDetail(5, currentMonth.minusMonths(1).toString()), MonthlyDetail(current, currentMonth.toString())))

    private val legacy = DashBoardDataResponse(
        economicResume = EconomicResume(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        activeSubscriptions = 550,
        subscriptionsResume = SubscriptionsResumeStatics(0, 7, 0),
        cancellationsResume = CancellationResumeDto(2, 0),
        paymentResume = emptyMap(),
        subscriptionsHistoryStatics = emptyList(),
        monthlyCollects = emptyList(),
        grossRevenueHistoryStatics = listOf(
            MonthlyGrossRevenueResume(48_000.0, currentMonth.minusMonths(2).toString() + "-01"),
            MonthlyGrossRevenueResume(49_000.0, currentMonth.minusMonths(1).toString() + "-01"),
            MonthlyGrossRevenueResume(0.0, currentMonth.toString() + "-01"),
        ),
        reconnections = 8,
        subscriptionLogSummary = mapOf(
            log("NEW_SUBSCRIPTION", 7), log("RECONNECT_CANCELLED_SUBSCRIPTION", 8), log("TOTAL_CANCEL_SUBSCRIPTION", 2),
        ),
    )

    @Test fun `commercial hero shows the net subscriber change of the month`() {
        compose.setContent { MaterialTheme { CommercialContent(DashboardState(dashboardData = legacy), {}, {}) } }

        compose.onNodeWithText("▲ 13 netos", substring = true).assertExists()
    }

    @Test fun `monthly evolution cards lead with the value of the month`() {
        compose.setContent { MaterialTheme { CommercialContent(DashboardState(dashboardData = legacy), {}, {}) } }

        val tag = "commercial_trend_${R.string.commercial_cancellations}"
        compose.onNodeWithTag("commercial_list").performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag("${tag}_value", useUnmergedTree = true).assertExists()
    }

    private val history = listOf("2026-05", "2026-06", "2026-07", "2026-08", "2026-09", "2026-10").map {
        ServiceHistory(it, BillingSummary(BigDecimal("100"), BigDecimal.ZERO, BigDecimal("100"), BigDecimal("80"), BigDecimal("20"), BigDecimal("80")))
    }
    private val report = FinancialReport(
        id = "cut-1",
        movements = MovementSummary(BigDecimal("18708"), BigDecimal.ZERO, BigDecimal("18708"), BigDecimal("1530")),
        receiptsByMethod = listOf(FinancialGroup("Yape", BigDecimal("300"), 3), FinancialGroup("Efectivo", BigDecimal("100"), 1)),
        history = history,
    )

    private fun showMovements() = compose.setContent {
        MaterialTheme {
            FinancialContent(FinancialUiState(serviceMonth = "2026-09", movementsMonth = "2026-10", report = report), {}, {}, legacy = legacy)
        }
    }

    @Test fun `movements view shows net billed against collected by service month`() {
        showMovements()

        compose.onNodeWithTag("financial_list").performScrollToNode(hasTestTag("financial_trend_chart"))
        compose.onNodeWithTag("financial_trend_chart").assertExists()
    }

    @Test fun `payment methods show each method share of the money received`() {
        showMovements()

        compose.onNodeWithTag("financial_list").performScrollToNode(hasTestTag("financial_method_chart"))
        compose.onNodeWithText("75.0%", substring = true).assertExists()
    }

    @Test fun `recovery of previous services moves out of the tile grid into the service breakdown`() {
        showMovements()

        compose.onNodeWithTag("financial_recovery").assertDoesNotExist()
        compose.onNodeWithTag("financial_list").performScrollToNode(hasText("Recuperación de servicios anteriores", substring = true))
    }

    @Test fun `gross revenue history summarises the last billed month`() {
        showMovements()

        compose.onNodeWithTag("financial_list").performScrollToNode(hasTestTag("financial_gross_revenue_chart"))
        compose.onNodeWithText("49,000", substring = true).assertExists()
    }

    private val trendTickets = tickets.copy(openWeekAgo = 14, monthly = listOf(
        com.dscorp.ispadmin.domain.model.dashboard.TicketMonthQuality(YearMonth.of(2026, 9), 20.5, 1.0),
        com.dscorp.ispadmin.domain.model.dashboard.TicketMonthQuality(YearMonth.of(2026, 10), 18.4, 1.0),
    ))

    @Test fun `clients hero compares open tickets with a week ago`() {
        compose.setContent { MaterialTheme { ClientsContent(ClientsUiState(dashboard = ClientsDashboard(null, trendTickets, null, null, null)), {}, {}) } }

        compose.onNodeWithText("▼ 5").assertExists()
        compose.onNodeWithText("vs semana pasada").assertExists()
    }

    @Test fun `resolution time shows the month value and its change against the previous month`() {
        compose.setContent { MaterialTheme { ClientsContent(ClientsUiState(dashboard = ClientsDashboard(null, trendTickets, null, null, null)), {}, {}) } }

        compose.onNodeWithTag("clients_resolution_time_value", useUnmergedTree = true).assertTextEquals("18.4 h")
        compose.onNodeWithText("▼ 2.1 h").assertExists()
    }

    @Test fun `migrations flag how many moved to a more expensive plan`() {
        val withUpgrades = DashBoardDataResponse(legacy.economicResume, legacy.activeSubscriptions, legacy.subscriptionsResume,
            legacy.cancellationsResume, legacy.paymentResume, legacy.subscriptionsHistoryStatics, legacy.monthlyCollects,
            legacy.grossRevenueHistoryStatics, legacy.reconnections, subscriptionLogSummary = legacy.subscriptionLogSummary,
            planUpgradesThisMonth = 2)
        compose.setContent { MaterialTheme { CommercialContent(DashboardState(dashboardData = withUpgrades), {}, {}) } }

        compose.onNodeWithText("⇄ Plan superior: 2").assertExists()
    }

    @Test fun `the yearly period widens the commercial charts to twelve months`() {
        compose.setContent { MaterialTheme { CommercialContent(DashboardState(dashboardData = legacy), {}, {}) } }

        compose.onNodeWithTag("commercial_period_TWELVE_MONTHS").performClick()

        compose.onNodeWithTag("commercial_list").performScrollToNode(hasText("Altas y bajas · últimos 12 meses"))
    }

    @Test fun `money received is compared with the previous month in the hero`() {
        val previous = report.copy(id = "cut-sep", movements = MovementSummary(BigDecimal("10000"), BigDecimal.ZERO, BigDecimal("10000"), BigDecimal.ZERO, BigDecimal("10000")))
        compose.setContent {
            MaterialTheme {
                FinancialContent(FinancialUiState(serviceMonth = "2026-08", movementsMonth = "2026-09", report = report, previousReport = previous), {}, {})
            }
        }

        compose.onNodeWithText("vs ago", substring = true).assertExists()
    }
}
