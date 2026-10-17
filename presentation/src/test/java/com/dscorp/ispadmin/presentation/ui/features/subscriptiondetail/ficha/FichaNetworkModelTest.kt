package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha

import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.NetworkDevice
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.subscription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class FichaNetworkModelTest {

    private val now = Instant.parse("2026-10-14T15:00:00Z")
    private val fiber = subscription().copy(
        lastCutOffDate = "2026-09-15",
        fiberOnuSn = "HWTC9A3F21B0",
        accessMode = "PPPOE_DYNAMIC",
        pppoeUsername = "jramos.lv",
        ip = "192.168.221.35",
        hostDevice = NetworkDevice(name = "core-1"),
        napBox = NapBoxResponse(code = "LV-15-3", address = "poste de interseccion de calles", mufaId = 4, placeName = "La Villa", placeId = 1, oltBoard = 1, oltPort = 3),
        borneNumber = "7",
        wifiSsid24 = "Ramos_Fibra",
        wifiSsid5 = "Ramos_Fibra_5G",
    )

    @Test
    fun `fiber network reads like the design`() {
        val network = FichaNetwork.from(
            fiber,
            cpe = CpeReading(online = true, rxDbm = -21.4, lastInform = now.minusSeconds(240), model = "Huawei HG8145V5"),
            cpeFailed = false,
            mufaName = "Antena Claro (96 hilos)",
            now = now,
        )

        assertEquals(
            "Acceso cortado por deuda desde 15 set (lista de deudores del MikroTik). Se restablece al cobrar.",
            network.cutNotice,
        )
        assertEquals(
            listOf(
                InfoRow("Serie", "HWTC9A3F21B0"),
                InfoRow("Modelo", "Huawei HG8145V5"),
                InfoRow("Estado", "", badge = StatusBadge("En línea", BadgeTone.SUCCESS)),
                InfoRow("Potencia", "−21.4 dBm · buena"),
                InfoRow("Último reporte", "hace 4 min"),
            ),
            network.equipment,
        )
        assertEquals(
            listOf(InfoRow("Modo", "PPPoE"), InfoRow("Usuario", "jramos.lv"), InfoRow("IP", "192.168.221.35"), InfoRow("Router núcleo", "core-1")),
            network.access,
        )
        assertEquals(
            listOf(
                InfoRow("Caja NAP", "LV-15-3 · puerto 7", note = "poste de interseccion de calles"),
                InfoRow("Mufa", "Antena Claro (96 hilos)"),
                InfoRow("OLT", "tarjeta 1 · puerto 3"),
            ),
            network.fiber,
        )
        assertEquals(listOf(InfoRow("2.4 GHz", "Ramos_Fibra"), InfoRow("5 GHz", "Ramos_Fibra_5G")), network.wifi)
    }

    @Test
    fun `optical power and report age read in plain words`() {
        assertEquals("−26.1 dBm · aceptable", powerLabel(-26.1))
        assertEquals("−28.0 dBm · baja", powerLabel(-28.0))
        assertEquals("hace 2 h", ageLabel(now.minusSeconds(7_300), now))
        assertEquals("hace 3 d", ageLabel(now.minusSeconds(3 * 86_400 + 60), now))
        assertEquals("hace un momento", ageLabel(now.minusSeconds(20), now))
    }

    @Test
    fun `without a reading the equipment card says so and keeps the serial`() {
        val network = FichaNetwork.from(fiber, cpe = null, cpeFailed = true, mufaName = null, now = now)

        assertTrue(network.equipmentUnavailable)
        assertEquals(listOf(InfoRow("Serie", "HWTC9A3F21B0")), network.equipment)
        assertEquals(listOf(InfoRow("Caja NAP", "LV-15-3 · puerto 7", note = "poste de interseccion de calles"), InfoRow("OLT", "tarjeta 1 · puerto 3")), network.fiber)
    }

    @Test
    fun `an offline ONU and an active wireless service`() {
        val offline = FichaNetwork.from(fiber, CpeReading(online = false, rxDbm = null, lastInform = null, model = null), false, null, now)
        assertEquals(InfoRow("Estado", "", badge = StatusBadge("Sin conexión", BadgeTone.ERROR)), offline.equipment[1])

        val wireless = FichaNetwork.from(
            subscription(status = ServiceStatus.ACTIVE).copy(installationType = InstallationType.WIRELESS, accessMode = "STATIC_IP", ip = "10.0.0.9"),
            cpe = null, cpeFailed = false, mufaName = null, now = now,
        )
        assertNull(wireless.cutNotice)
        assertEquals(emptyList<InfoRow>(), wireless.equipment)
        assertEquals(emptyList<InfoRow>(), wireless.fiber)
        assertEquals(listOf(InfoRow("Modo", "IP fija"), InfoRow("IP", "10.0.0.9")), wireless.access)
    }
}
