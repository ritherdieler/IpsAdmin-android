package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.subscription
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

private val TODAY = LocalDate.of(2026, 10, 14)

private fun jorge() = subscription(status = com.dscorp.ispadmin.domain.model.ServiceStatus.ACTIVE).copy(borneNumber = "7", fiberOnuSn = "HWTC9A3F21B0").apply {
    napBox = NapBoxResponse(code = "LV-15-3", placeName = "La Villa", placeId = 1)
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h1400dp")
class CancelSubscriptionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `the sheet says only what cancelling really does`() {
        val model = CancelSubscription.from(jorge(), TODAY)

        assertEquals("¿Cancelar la suscripción de Jorge Ramos Villanueva?", model.title)
        assertEquals(
            listOf(
                "Deja de generar recibos desde nov 2026.",
                "Se le corta el internet.",
                "Se libera el puerto 7 de la caja LV-15-3.",
                "La ONU HWTC9A3F21B0 queda registrada en la OLT para reutilizarla.",
                "La deuda de S/ 100.00 se mantiene.",
            ),
            model.effects,
        )
    }

    @Test
    fun `wireless without debt has fewer effects`() {
        val radio = subscription(status = com.dscorp.ispadmin.domain.model.ServiceStatus.ACTIVE, pendingInvoices = 0, totalDebt = 0.0).apply { installationType = InstallationType.WIRELESS }

        assertEquals(listOf("Deja de generar recibos desde nov 2026.", "Se le corta el internet."), CancelSubscription.from(radio, TODAY).effects)
        assertEquals("Deja de generar recibos desde ene 2027.", CancelSubscription.from(radio, LocalDate.of(2026, 12, 3)).effects.first())
    }

    @Test
    fun `cancelling needs a reason`() {
        var confirmed: String? = null
        var back = false
        compose.setContent {
            MaterialTheme {
                var reason by remember { mutableStateOf<String?>(null) }
                CancelSubscriptionSheetContent(CancelSubscription.from(jorge(), TODAY), reason = reason, onReason = { reason = it },
                    busy = false, error = null, onConfirm = { confirmed = it }, onBack = { back = true })
            }
        }

        compose.onNodeWithText("Se libera el puerto 7 de la caja LV-15-3.").assertExists()
        compose.onNodeWithText("Motivo *").assertExists()
        compose.onNodeWithTag(CancelSubscriptionTestTags.CONFIRM).assertIsNotEnabled()
        compose.onNodeWithTag(CancelSubscriptionTestTags.REASON).performClick()
        compose.onNodeWithText("Mudanza").performClick()
        compose.onNodeWithTag(CancelSubscriptionTestTags.CONFIRM).performClick()
        compose.onNodeWithText("Volver").performClick()

        assertEquals("Mudanza" to true, confirmed to back)
    }

    @Test
    fun `a cut subscription is already without internet`() {
        val cut = subscription(pendingInvoices = 0, totalDebt = 0.0).apply { installationType = InstallationType.WIRELESS }

        assertEquals(listOf("Deja de generar recibos desde nov 2026."), CancelSubscription.from(cut, TODAY).effects)
    }
}
