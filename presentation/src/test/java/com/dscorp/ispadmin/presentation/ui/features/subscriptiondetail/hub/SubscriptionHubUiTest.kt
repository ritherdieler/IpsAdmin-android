package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.domain.model.ServiceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h2000dp")
class SubscriptionHubUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `header shows status, debt and the charge action`() {
        var charged = false
        compose.setContent {
            MaterialTheme { SubscriptionHubHeader(HubHeader.from(subscription()), onCharge = { charged = true }) }
        }

        compose.onNodeWithText("Jorge Ramos Villanueva").assertExists()
        compose.onNodeWithText("Cortado").assertExists()
        compose.onNodeWithText("Debe 2 meses").assertExists()
        compose.onNodeWithText("S/ 100.00").assertExists()
        compose.onNodeWithTag(SubscriptionHubTestTags.CHARGE).performClick()
        assertTrue(charged)
    }

    @Test
    fun `up to date subscription has no charge action`() {
        compose.setContent {
            MaterialTheme {
                SubscriptionHubHeader(HubHeader.from(subscription(ServiceStatus.ACTIVE, 0, 0.0)), onCharge = {})
            }
        }

        compose.onNodeWithTag(SubscriptionHubTestTags.CHARGE).assertDoesNotExist()
        compose.onNodeWithText("Activo").assertExists()
    }

    @Test
    fun `charge sheet explains why it cannot be submitted yet`() {
        val draft = ChargeDraft.start(listOf(receipt(1, 100), receipt(2, 200)), METHODS, ServiceStatus.CUT_OFF)
        var intent: SubscriptionHubIntent? = null
        compose.setContent { MaterialTheme { ChargeSheetContent(draft, holderName = "Jorge Ramos", onIntent = { intent = it }) } }

        compose.onNodeWithText("Escribe el nombre que aparece en Yape").assertExists()
        compose.onNodeWithTag(SubscriptionHubTestTags.SUBMIT_CHARGE).assertIsNotEnabled()
        compose.onNodeWithText("Restablecer el servicio").assertExists()

        compose.onNodeWithText("Efectivo").performClick()
        assertEquals(SubscriptionHubIntent.MethodSelected("Efectivo"), intent)
    }

    @Test
    fun `ready charge sheet shows the total on the action`() {
        val draft = ChargeDraft.start(listOf(receipt(1, 100), receipt(2, 200)), METHODS, ServiceStatus.CUT_OFF)
            .copy(method = "Efectivo")
        compose.setContent { MaterialTheme { ChargeSheetContent(draft, holderName = "Jorge Ramos", onIntent = {}) } }

        compose.onNodeWithTag(SubscriptionHubTestTags.SUBMIT_CHARGE).assertIsEnabled()
        compose.onNodeWithText("Registrar cobro · S/ 100.00").assertExists()
    }

    @Test
    fun `cash shows the change and a discount lowers the action`() {
        val draft = ChargeDraft.start(listOf(receipt(1, 100), receipt(2, 200)), METHODS, ServiceStatus.ACTIVE)
            .copy(method = "Efectivo", received = "110", discountOpen = true, discount = "10", discountReason = "Promoción")
        compose.setContent { MaterialTheme { ChargeSheetContent(draft, holderName = "Jorge Ramos", onIntent = {}) } }

        compose.onNodeWithText("Vuelto S/ 20.00").assertExists()
        compose.onNodeWithText("Registrar cobro · S/ 90.00").assertExists()
        compose.onNodeWithText("Quitar").assertExists()
    }

    @Test
    fun `the receipt confirms the charge`() {
        var done = false
        compose.setContent {
            MaterialTheme {
                ChargeReceiptContent(
                    ChargeReceipt("P-1", "Jorge Ramos Villanueva", "ago y set 2026", "Yape · Jorge Ramos V.", "S/ 100.00", "Rosa C.", "14 oct 2026 · 11:42", restored = true),
                    onDone = { done = true }, onShare = {},
                )
            }
        }

        compose.onNodeWithText("Cobro registrado").assertExists()
        compose.onNodeWithText("Servicio restablecido.").assertExists()
        compose.onNodeWithText("Listo").performClick()
        assertTrue(done)
    }
}
