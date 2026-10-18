package com.dscorp.ispadmin.presentation.ui.features.payment.cash

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h1400dp")
class PaymentSearchUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `results are grouped by day and open the payment`() {
        val intents = mutableListOf<PaymentSearchIntent>()
        val opened = mutableListOf<Int>()
        val state = PaymentSearchUiState(
            query = "ccori",
            methods = listOf("Efectivo", "Yape"),
            result = PaymentSearchResult.from(ccori(), PaymentPeriod.LAST_30, LocalDate.of(2026, 10, 18)),
        )
        compose.setContent { MaterialTheme { PaymentSearchContent(state, onIntent = { intents += it }, onOpen = { opened += it }, onBack = {}) } }

        compose.onNodeWithText("Hoy").assertExists()
        compose.onNodeWithText("Busca por titular o por el nombre que aparece en Yape o Plin").assertExists()
        compose.onNodeWithText("4 pagos en los últimos 30 días").assertExists()
        compose.onNodeWithText("Yape · pagó: Hilda Ccori M. · 11:30").performClick()
        compose.onNodeWithText("Todos los métodos").performClick()
        compose.onNodeWithText("Yape").performClick()
        compose.onNodeWithTag(PaymentSearchTestTags.QUERY).performTextInput(" m")

        assertEquals(listOf(9), opened)
        assertEquals(listOf(PaymentSearchIntent.MethodSelected("Yape"), PaymentSearchIntent.QueryChanged("ccori m")), intents)
    }

    @Test
    fun `the detail shows the receipt and leads to the subscription`() {
        var shared = false
        var ficha: Int? = null
        compose.setContent {
            MaterialTheme {
                PaymentDetailContent(
                    PaymentReceiptUiState(detail = PaymentDetail.from(PAID_JULY)),
                    onBack = {}, onShare = { shared = true }, onOpenSubscription = { ficha = it }, onRetry = {},
                )
            }
        }

        compose.onNodeWithText("Pago de jul 2026").assertExists()
        compose.onAllNodesWithText("Pagado").assertCountEquals(2) // badge and row
        compose.onNodeWithText("P-46790").assertExists()
        compose.onNodeWithText("Compartir comprobante").performClick()
        compose.onNodeWithText("Ver suscripción").performClick()

        assertEquals(true, shared)
        assertEquals(1842, ficha)
    }
}
