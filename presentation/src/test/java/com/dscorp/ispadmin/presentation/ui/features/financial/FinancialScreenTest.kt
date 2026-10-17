package com.dscorp.ispadmin.presentation.ui.features.financial

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.dscorp.ispadmin.domain.model.financial.*
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34], application=Application::class)
class FinancialScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun `refresh error keeps displayed cut and retry emits load`() {
        val intents = mutableListOf<FinancialIntent>()
        compose.setContent { MaterialTheme { FinancialContent(FinancialUiState(failed=true,
            serviceMonth="2026-09", movementsMonth="2026-10", report=FinancialReport(id="cut-1", cutoff="2026-10-09T12:00:00-05:00")), intents::add, {}) } }
        compose.onNodeWithTag("financial_cut").assertExists()
        compose.onNodeWithTag("financial_retry").performClick()
        assertEquals(FinancialIntent.Load, intents.last())
    }
    @Test fun `cash indicator opens receipt records from same cut`() {
        val receipt = ReceiptInput(22, "50.00".toBigDecimal(), "2026-10-08T12:00:00", "YAPE", "Ana", emptyList())
        compose.setContent { MaterialTheme { FinancialContent(FinancialUiState(serviceMonth="2026-09", movementsMonth="2026-10",
            report=FinancialReport(id="cut-2", movements=MovementSummary("50.00".toBigDecimal(), null,null,"0".toBigDecimal()),
                receipts=listOf(FinancialReceipt(receipt, listOf("2026-09"))))), {}, {}) } }
        compose.onNodeWithTag("financial_received").performClick()
        compose.onNodeWithTag("financial_record_receipt_22").assertExists()
        compose.onNodeWithText("YAPE · Ana", substring=true).assertExists()
    }

    @Test fun `billing view exposes chart values to accessibility services`() {
        val summary = BillingSummary(
            gross = "100.00".toBigDecimal(), adjustments = "0.00".toBigDecimal(),
            net = "100.00".toBigDecimal(), collected = "80.00".toBigDecimal(),
            pending = "20.00".toBigDecimal(), effectivenessPercent = "80.00".toBigDecimal()
        )
        compose.setContent {
            MaterialTheme {
                FinancialContent(
                    FinancialUiState(
                        serviceMonth = "2026-09", movementsMonth = "2026-10",
                        billingView = true,
                        report = FinancialReport(
                            id = "cut-chart",
                            history = listOf(ServiceHistory("2026-09", summary))
                        )
                    ),
                    {}, {}
                )
            }
        }

        compose.onNodeWithTag("financial_list").performScrollToNode(hasTestTag("financial_history_chart"))
        compose.onNodeWithTag("financial_history_chart")
            .assertContentDescriptionContains("2026-09", substring = true)
            .assertContentDescriptionContains("100.00", substring = true)
            .assertContentDescriptionContains("80.00", substring = true)
            .assertContentDescriptionContains("20.00", substring = true)
    }

    @Test fun `billing labels identify the service month and collection cutoff`() {
        compose.setContent {
            MaterialTheme {
                FinancialContent(
                    FinancialUiState(serviceMonth = "2026-09", movementsMonth = "2026-10", billingView = true,
                        report = FinancialReport(id = "cut-billing-labels", billing = BillingSummary(
                            BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal("100.00"), BigDecimal("80.00"),
                            BigDecimal("20.00"), BigDecimal("80.00")))),
                    {}, {}
                )
            }
        }

        compose.onNodeWithText("Cobrado aplicado a facturas del mes de servicio hasta el corte").assertExists()
        compose.onNodeWithText("Mes de servicio: 2026-09").assertExists()
        compose.onNodeWithText("Mes de movimientos: 2026-10").assertDoesNotExist()
    }

    @Test fun `billing view distinguishes invoices without subscription links from zero subscriptions`() {
        val unknownInvoice = FinancialInvoice(
            invoice = InvoiceInput(1, null, null, null, "2026-09", "EXPLICIT", null, null, false,
                BigDecimal("65000.00"), BigDecimal.ZERO, null, "OPEN", null, "PEN"),
            net = BigDecimal("65000.00"), collected = BigDecimal.ZERO, pending = BigDecimal("65000.00"),
            difference = null, agingBucket = "UNCONFIRMED"
        )
        val report = FinancialReport(
            id = "cut-unlinked-debt",
            invoices = listOf(unknownInvoice),
            aging = listOf(FinancialGroup("UNCONFIRMED", BigDecimal("65000.00"), 0)),
            zones = listOf(ZoneDebt(null, BigDecimal("65000.00"), BigDecimal.ZERO, 0))
        )
        compose.setContent {
            MaterialTheme {
                FinancialContent(FinancialUiState(serviceMonth = "2026-09", movementsMonth = "2026-10",
                    billingView = true, report = report), {}, {})
            }
        }

        compose.onNodeWithTag("financial_list").performScrollToNode(hasText("Antigüedad de cartera"))
        compose.onNodeWithText("Suscripciones identificadas: 0 · Facturas sin relación: 1").assertExists()
        compose.onNodeWithTag("financial_list").performScrollToNode(hasText("Zonas con mayor deuda"))
        compose.onNodeWithText("Suscripciones identificadas: 0 · Facturas sin relación: 1 · vencido", substring = true).assertExists()
    }

    @Test fun `billing coverage counts use thousands separators`() {
        val report = FinancialReport(
            id = "cut-coverage-counts",
            coverage = FinancialCoverage(28_313, 0, 1_186, 5, emptyList())
        )
        compose.setContent {
            MaterialTheme {
                FinancialContent(FinancialUiState(serviceMonth = "2026-09", movementsMonth = "2026-10",
                    billingView = true, report = report), {}, {})
            }
        }

        compose.onNodeWithTag("financial_list").performScrollToNode(hasText("Cobertura de datos"))
        compose.onNodeWithText("28,313 períodos inferidos · 0 sin período · 1,186 vencimientos sin confirmar · 5 cobros sin fecha").assertExists()
    }

    @Test fun `switching report tabs returns the list to the selected report header`() {
        var state by mutableStateOf(FinancialUiState(
            serviceMonth = "2026-09", movementsMonth = "2026-10",
            report = FinancialReport(id = "cut-tab-scroll", billing = BillingSummary(
                BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal("100.00"), BigDecimal.ZERO,
                BigDecimal("100.00"), BigDecimal.ZERO))
        ))
        compose.setContent {
            MaterialTheme {
                FinancialContent(state, { intent ->
                    if (intent is FinancialIntent.SelectView) state = state.copy(billingView = intent.billing)
                }, {})
            }
        }

        compose.onNodeWithTag("financial_list").performScrollToNode(hasTestTag("financial_export"))
        compose.onNodeWithTag("financial_billing_tab").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Mes de servicio: 2026-09").assertExists()
        compose.onNodeWithTag("financial_gross").assertExists()
    }

    @Test fun `movement and billing tabs keep their own report sections`() {
        val summary = BillingSummary(BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal("100.00"),
            BigDecimal("80.00"), BigDecimal("20.00"), BigDecimal("80.00"))
        var state by mutableStateOf(FinancialUiState(
            serviceMonth = "2026-09", movementsMonth = "2026-10",
            report = FinancialReport(
                id = "cut-tab-content",
                history = listOf(ServiceHistory("2026-09", summary)),
                receiptsByMethod = listOf(FinancialGroup("Yape", BigDecimal("100.00"), 1))
            )
        ))
        compose.setContent {
            MaterialTheme {
                FinancialContent(state, { intent ->
                    if (intent is FinancialIntent.SelectView) state = state.copy(billingView = intent.billing)
                }, {})
            }
        }

        compose.onNodeWithTag("financial_list").performScrollToNode(hasTestTag("financial_method_chart"))
        compose.onNodeWithTag("financial_method_chart").assertExists()
        assertTagNotReachable("financial_history_chart")

        compose.onNodeWithTag("financial_billing_tab").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("financial_list").performScrollToNode(hasTestTag("financial_history_chart"))
        compose.onNodeWithTag("financial_history_chart").assertExists()
        assertTagNotReachable("financial_method_chart")
    }

    @Test fun `operational access cannot open management billing and expense views`() {
        val report = FinancialReport(
            id = "cut-operational-access",
            access = "OPERATIONAL",
            movements = MovementSummary(BigDecimal("100.00"), BigDecimal("50.00"), BigDecimal("50.00"), BigDecimal.ZERO)
        )
        compose.setContent {
            MaterialTheme {
                FinancialContent(FinancialUiState(serviceMonth = "2026-09", movementsMonth = "2026-10", report = report), {}, {})
            }
        }

        compose.onNodeWithTag("financial_billing_tab").assertDoesNotExist()
        compose.onNodeWithTag("financial_expenses").assertDoesNotExist()
        compose.onNodeWithTag("financial_received").assertExists()
    }

    @Test fun `movement dashboard charts receipts by payment method`() {
        compose.setContent {
            MaterialTheme {
                FinancialContent(
                    FinancialUiState(
                        serviceMonth = "2026-09", movementsMonth = "2026-10",
                        report = FinancialReport(
                            id = "cut-methods",
                            receiptsByMethod = listOf(
                                FinancialGroup("Yape", "140.00".toBigDecimal(), 7),
                                FinancialGroup("Efectivo", "60.00".toBigDecimal(), 3)
                            )
                        )
                    ),
                    {}, {}
                )
            }
        }

        compose.onNodeWithTag("financial_list").performScrollToNode(hasTestTag("financial_method_chart"))
        compose.onNodeWithTag("financial_method_chart").assertExists()
        compose.onNodeWithText("Yape").assertExists()
    }

    @Test fun `movement service breakdown identifies the month receipts were received`() {
        val report = FinancialReport(
            id = "cut-service-month-cash",
            receiptsByServiceMonth = listOf(FinancialGroup("2026-09", BigDecimal("50.00"), 3))
        )
        compose.setContent {
            MaterialTheme {
                FinancialContent(FinancialUiState(serviceMonth = "2026-09", movementsMonth = "2026-10", report = report), {}, {})
            }
        }

        compose.onNodeWithTag("financial_list")
            .performScrollToNode(hasText("Cobros recibidos en 2026-10 por mes de servicio"))
        compose.onNodeWithText("Cobros recibidos en 2026-10 por mes de servicio").assertExists()
        compose.onNodeWithText("2026-09").assertExists()
    }

    @Test fun `movement dashboard shows dated subtotal instead of hiding it for undated history`() {
        val receipts = listOf(
            FinancialReceipt(ReceiptInput(1, "60.00".toBigDecimal(), "2026-10-10T12:00:00", "YAPE", null,
                listOf(AllocationInput(1, "60.00".toBigDecimal()))), listOf("2026-09")),
            FinancialReceipt(ReceiptInput(2, "40.00".toBigDecimal(), null, "CASH", null,
                listOf(AllocationInput(1, "40.00".toBigDecimal()))), listOf("2026-09"))
        )
        val report = FinancialReport(
            id = "cut-partial-cash",
            movementsMonth = "2026-10",
            movements = MovementSummary(null, BigDecimal.ZERO, null, null),
            receipts = receipts,
            coverage = FinancialCoverage(0, 0, 0, 1, emptyList())
        )
        compose.setContent {
            MaterialTheme {
                FinancialContent(FinancialUiState(serviceMonth = "2026-09", movementsMonth = "2026-10", report = report), {}, {})
            }
        }

        compose.onNodeWithTag("financial_received_value", useUnmergedTree = true).assertTextEquals("S/\u00a060.00")
        compose.onNodeWithText("Cobros con fecha en el mes").assertExists()
        compose.onNodeWithText("Cobros históricos sin fecha efectiva excluidos del cálculo mensual: 1").assertExists()
    }

    @Test fun `movement difference indicator prioritizes selected service month`() {
        val report = FinancialReport(
            id = "cut-current-difference-count",
            serviceMonth = "2026-08",
            invoices = listOf(
                differenceInvoice(1, "2026-08", "5.00"),
                differenceInvoice(2, "2026-07", "10.00"),
                differenceInvoice(3, null, "15.00")
            ),
            differences = listOf(
                FinancialDifference("SETTLEMENT_MISMATCH", "invoice", 1, BigDecimal("5.00"), "Current", listOf("2026-08")),
                FinancialDifference("SETTLEMENT_MISMATCH", "invoice", 2, BigDecimal("10.00"), "Historical", listOf("2026-07")),
                FinancialDifference("MISSING_SERVICE_PERIOD", "invoice", 3, BigDecimal("15.00"), "Unknown")
            )
        )
        compose.setContent {
            MaterialTheme {
                FinancialContent(FinancialUiState(serviceMonth = "2026-08", movementsMonth = "2026-10", report = report), {}, {})
            }
        }

        compose.onNodeWithTag("financial_differences_value", useUnmergedTree = true).assertTextEquals("1")
        compose.onNodeWithText("Del servicio: 1 · Otros períodos: 1 · Sin período: 1").assertExists()
    }

    @Test fun `billing view separates selected service differences from history and unknown periods`() {
        val report = FinancialReport(
            id = "cut-differences",
            serviceMonth = "2026-08",
            invoices = listOf(
                differenceInvoice(1, "2026-08", "5.00"),
                differenceInvoice(2, "2026-07", "10.00"),
                differenceInvoice(3, null, "15.00")
            ),
            differences = listOf(
                FinancialDifference("SETTLEMENT_MISMATCH", "invoice", 1, BigDecimal("5.00"), "Mismatch"),
                FinancialDifference("SETTLEMENT_MISMATCH", "invoice", 2, BigDecimal("10.00"), "Mismatch"),
                FinancialDifference("MISSING_SERVICE_PERIOD", "invoice", 3, BigDecimal("15.00"), "Missing period")
            )
        )
        compose.setContent {
            MaterialTheme {
                FinancialContent(
                    FinancialUiState(report = report, serviceMonth = "2026-08", movementsMonth = "2026-10", billingView = true),
                    {}, {}
                )
            }
        }

        compose.onNodeWithTag("financial_list").performScrollToNode(hasTestTag("financial_difference_summary"))
        compose.onNodeWithTag("financial_difference_summary").assertExists()
        compose.onNodeWithText("Del servicio: 1 · Otros períodos: 1 · Sin período: 1").assertExists()
    }

    @Test fun `difference details show affected service period and linked receipt ids`() {
        val report = FinancialReport(
            id = "cut-receipt-links",
            serviceMonth = "2026-08",
            invoices = listOf(differenceInvoice(1, "2026-08", "5.00")),
            receipts = listOf(FinancialReceipt(
                ReceiptInput(22, BigDecimal("30.00"), "2026-09-10T12:00:00", "YAPE", null,
                    listOf(AllocationInput(1, BigDecimal("30.00")))),
                listOf("2026-08")
            )),
            differences = listOf(FinancialDifference("SETTLEMENT_MISMATCH", "invoice", 1,
                BigDecimal("5.00"), "Cobro aplicado no concilia con obligación neta"))
        )
        compose.setContent { MaterialTheme { RecordsDialog(report, Detail.DIFFERENCES, {}) } }

        compose.onNodeWithText("Servicio: 2026-08", substring = true).assertExists()
        compose.onNodeWithText("Cobros relacionados: #22", substring = true).assertExists()
    }

    @Test fun `difference details default to selected service period and can include history`() {
        val report = FinancialReport(
            id = "cut-filter-differences",
            serviceMonth = "2026-08",
            invoices = listOf(
                differenceInvoice(1, "2026-08", "5.00"),
                differenceInvoice(2, "2026-07", "10.00"),
                differenceInvoice(3, null, "15.00")
            ),
            differences = listOf(
                FinancialDifference("SETTLEMENT_MISMATCH", "invoice", 1, BigDecimal("5.00"), "Current", listOf("2026-08")),
                FinancialDifference("SETTLEMENT_MISMATCH", "invoice", 2, BigDecimal("10.00"), "Historical", listOf("2026-07")),
                FinancialDifference("MISSING_SERVICE_PERIOD", "invoice", 3, BigDecimal("15.00"), "Unknown")
            )
        )
        compose.setContent { MaterialTheme { RecordsDialog(report, Detail.DIFFERENCES, {}) } }

        compose.onNodeWithTag("financial_record_difference_0").assertExists()
        compose.onNodeWithTag("financial_record_difference_1").assertDoesNotExist()
        compose.onNodeWithText("Todos · 3").assertExists()
        compose.onNodeWithTag("financial_difference_filter_all").performClick()
        compose.onNodeWithTag("financial_record_difference_1").assertExists()
        compose.onNodeWithTag("financial_record_list").performScrollToNode(hasTestTag("financial_record_difference_2"))
        compose.onNodeWithTag("financial_record_difference_2").assertExists()
    }

    @Test fun `receipt difference detail does not describe the receipt as related to itself`() {
        val report = FinancialReport(
            id = "cut-undated-receipt",
            serviceMonth = "2023-05",
            differences = listOf(FinancialDifference("MISSING_RECEIPT_DATE", "receipt", 980,
                BigDecimal("50.00"), "Cobro sin fecha efectiva", listOf("2023-05"), emptyList()))
        )
        compose.setContent { MaterialTheme { RecordsDialog(report, Detail.DIFFERENCES, {}) } }

        compose.onNodeWithText("Cobro #980", substring = true).assertExists()
        compose.onNodeWithText("Cobros relacionados: Sin cobros relacionados", substring = true).assertDoesNotExist()
    }

    private fun differenceInvoice(id: Int, serviceMonth: String?, difference: String) = FinancialInvoice(
        invoice = InvoiceInput(id, id, null, null, serviceMonth, "EXPLICIT", null, null, false,
            BigDecimal("30.00"), BigDecimal("5.00"), null, "PAID", null, "PEN"),
        net = BigDecimal("25.00"), collected = BigDecimal("30.00"), pending = BigDecimal.ZERO,
        difference = BigDecimal(difference), agingBucket = null
    )

    private fun assertTagNotReachable(tag: String) {
        val failure = runCatching {
            compose.onNodeWithTag("financial_list").performScrollToNode(hasTestTag(tag))
        }.exceptionOrNull()
        assertNotNull("Expected $tag to be omitted from the selected report", failure)
    }
}
