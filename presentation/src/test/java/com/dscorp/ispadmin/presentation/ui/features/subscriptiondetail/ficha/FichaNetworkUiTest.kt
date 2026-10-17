package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h2400dp")
class FichaNetworkUiTest {
    @get:Rule val compose = createComposeRule()

    private var reboots = 0
    private var resends = 0
    private var registration = false

    private fun show(network: FichaNetwork, isFiber: Boolean = true) = compose.setContent {
        MaterialTheme {
            FichaNetworkTab(
                network, isFiber = isFiber,
                onReboot = { reboots++ }, onResend = { resends++ }, onSeeRegistration = { registration = true },
            )
        }
    }

    private val network = FichaNetwork(
        cutNotice = "Acceso cortado por deuda desde 15 set (lista de deudores del MikroTik). Se restablece al cobrar.",
        equipment = listOf(InfoRow("Serie", "HWTC9A3F21B0"), InfoRow("Estado", "", badge = StatusBadge("En línea", BadgeTone.SUCCESS))),
        equipmentUnavailable = false,
        access = listOf(InfoRow("Modo", "PPPoE")),
        fiber = listOf(InfoRow("Caja NAP", "LV-15-3 · puerto 7", note = "poste de interseccion de calles")),
        wifi = listOf(InfoRow("2.4 GHz", "Ramos_Fibra")),
    )

    @Test
    fun `network tab shows the design cards`() {
        show(network)

        compose.onNodeWithText(network.cutNotice!!).assertExists()
        compose.onNodeWithText("Equipo").assertExists()
        compose.onNodeWithText("En línea").assertExists()
        compose.onNodeWithText("Acceso a internet").assertExists()
        compose.onNodeWithText("Fibra").assertExists()
        compose.onNodeWithText("poste de interseccion de calles").assertExists()
        compose.onNodeWithText("Wi-Fi").assertExists()
        compose.onNodeWithText("Pide confirmación; el abonado pierde la conexión 1–2 minutos.").assertExists()
        compose.onNodeWithText("Ver registro de la ONU").performClick()
        assertTrue(registration)
    }

    @Test
    fun `equipment actions ask for confirmation first`() {
        show(network)

        compose.onNodeWithText("Reiniciar ONU").performClick()
        assertEquals(0, reboots)
        compose.onNodeWithText("¿Reiniciar la ONU?").assertExists()
        compose.onAllNodesWithText("Reiniciar")[0].performClick()
        assertEquals(1, reboots)

        compose.onNodeWithText("Reenviar configuración").performClick()
        compose.onAllNodesWithText("Reenviar")[0].performClick()
        assertEquals(1, resends)
    }

    @Test
    fun `without a reading the equipment card says so`() {
        show(network.copy(equipmentUnavailable = true, equipment = listOf(InfoRow("Serie", "HWTC9A3F21B0"))))

        compose.onNodeWithText("Sin lectura del equipo por ahora.").assertExists()
    }

    @Test
    fun `wireless services have no equipment actions`() {
        show(network.copy(equipment = emptyList(), fiber = emptyList(), cutNotice = null), isFiber = false)

        compose.onNodeWithText("Reiniciar ONU").assertDoesNotExist()
        compose.onNodeWithText("Acceso a internet").assertExists()
    }
}
