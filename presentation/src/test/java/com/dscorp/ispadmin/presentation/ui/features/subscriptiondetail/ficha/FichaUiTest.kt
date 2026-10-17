package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dscorp.ispadmin.domain.model.CustomerServiceSummary
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h2000dp")
class FichaUiTest {
    @get:Rule val compose = createComposeRule()

    private val header = FichaHeader(
        name = "Jorge Ramos Villanueva",
        subtitle = "Cód. 1842 · Fibra · La Villa",
        badges = listOf(StatusBadge("Cortado desde 15 set", BadgeTone.WARNING), StatusBadge("Debe 2 meses", BadgeTone.ERROR)),
        debt = DebtCard("S/ 100.00", "ago y set 2026 · oct vence mañana"),
    )

    @Test
    fun `header shows state, debt and charges from the card`() {
        var back = false
        var charged = false
        compose.setContent {
            MaterialTheme { FichaHeaderBand(header, compact = false, onBack = { back = true }, onMore = null, onCharge = { charged = true }) }
        }

        compose.onNodeWithText("Jorge Ramos Villanueva").assertExists()
        compose.onNodeWithText("Cortado desde 15 set").assertExists()
        compose.onNodeWithText("Deuda pendiente").assertExists()
        compose.onNodeWithText("ago y set 2026 · oct vence mañana").assertExists()
        compose.onNodeWithText("Cobrar").performClick()
        compose.onNodeWithContentDescription("Atrás").performClick()

        assertTrue(charged)
        assertTrue(back)
    }

    @Test
    fun `compact header keeps only name, code and badges`() {
        compose.setContent { MaterialTheme { FichaHeaderBand(header, compact = true, onBack = {}, onMore = null, onCharge = {}) } }

        compose.onNodeWithText("Debe 2 meses").assertExists()
        compose.onNodeWithText("Deuda pendiente").assertDoesNotExist()
    }

    @Test
    fun `quick actions and tabs report what was tapped`() {
        var action: FichaAction? = null
        var tab: FichaTab? = null
        compose.setContent {
            MaterialTheme {
                androidx.compose.foundation.layout.Column {
                    FichaQuickActions(FichaAction.entries, onAction = { action = it })
                    FichaTabs(FichaTab.RESUMEN, onSelect = { tab = it })
                }
            }
        }

        compose.onNodeWithText("Restablecer").performClick()
        assertEquals(FichaAction.RESTORE, action)
        compose.onNodeWithText("Pagos").performClick()
        assertEquals(FichaTab.PAGOS, tab)
    }

    @Test
    fun `summary tab shows the restore hint and the design cards`() {
        var seeNetwork = false
        var photo = false
        compose.setContent {
            MaterialTheme {
                FichaSummary(
                    restoreHint = "Para restablecer debe quedar con menos de 2 recibos pendientes: cobra al menos 1 mes.",
                    service = listOf(InfoRow("Plan", "Basico · 200/200 Mbps")),
                    holder = listOf(InfoRow("DNI", "70418293")),
                    network = listOf(InfoRow("Caja NAP", "LV-15-3 · puerto 7")),
                    facadePhotoUrl = null,
                    otherServices = listOf(CustomerServiceSummary(id = 7, serviceStatus = "ACTIVE", planName = "cable basico", installationType = "ONLY_TV_FIBER", totalDebt = 0.0)),
                    onSeeNetwork = { seeNetwork = true },
                    onUpdatePhoto = { photo = true },
                    onOpenService = {},
                )
            }
        }

        compose.onNodeWithText("Para restablecer debe quedar con menos de 2 recibos pendientes: cobra al menos 1 mes.").assertExists()
        compose.onNodeWithText("Servicio").assertExists()
        compose.onNodeWithText("Basico · 200/200 Mbps").assertExists()
        compose.onNodeWithText("Titular").assertExists()
        compose.onNodeWithText("LV-15-3 · puerto 7").assertExists()
        compose.onNodeWithText("Otros servicios del titular").assertExists()
        compose.onNodeWithText("Solo TV · Cable basico").assertExists()
        compose.onNodeWithText("Ver red").performClick()
        compose.onNodeWithText("Actualizar foto").performClick()

        assertTrue(seeNetwork)
        assertTrue(photo)
    }
}
