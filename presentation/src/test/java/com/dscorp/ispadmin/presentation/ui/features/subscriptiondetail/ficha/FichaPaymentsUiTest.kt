package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h2000dp")
class FichaPaymentsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `payments tab charges from the pending card and opens older payments`() {
        var charged = false
        var history = false
        compose.setContent {
            MaterialTheme {
                FichaPaymentsTab(
                    FichaPayments(
                        pending = PendingCard("S/ 100.00", "2 recibos vencidos · ago y set 2026", "Cobrar S/ 100.00"),
                        lastPayment = "S/ 50.00 · 12 jul · Yape",
                        dueRule = "Vencen el 15 de cada mes",
                        receipts = listOf(
                            ReceiptRow(9, "Set 2026", "Venció 15 set", StatusBadge("Vencido", BadgeTone.ERROR), "S/ 50.00", overdue = true),
                            ReceiptRow(7, "Jul 2026", "Pagado 12 jul · Yape", StatusBadge("Pagado", BadgeTone.SUCCESS), "S/ 50.00", overdue = false),
                        ),
                    ),
                    onCharge = { charged = true },
                    onSeeHistory = { history = true },
                )
            }
        }

        compose.onNodeWithText("Pendiente").assertExists()
        compose.onNodeWithText("2 recibos vencidos · ago y set 2026").assertExists()
        compose.onNodeWithText("S/ 50.00 · 12 jul · Yape").assertExists()
        compose.onNodeWithText("Vencen el 15 de cada mes").assertExists()
        compose.onNodeWithText("Venció 15 set").assertExists()
        compose.onNodeWithText("Pagado 12 jul · Yape").assertExists()
        compose.onNodeWithText("Cobrar S/ 100.00").performClick()
        compose.onNodeWithText("Ver pagos anteriores").performClick()

        assertTrue(charged)
        assertTrue(history)
    }
}
