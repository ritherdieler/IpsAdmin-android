package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha

import com.dscorp.ispadmin.domain.model.GeoLocation
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.Payment
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.subscription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

private val LIMA = ZoneId.of("America/Lima")
private val TODAY = LocalDate.of(2026, 10, 14)

private fun billing(year: Int, month: Int) =
    LocalDate.of(year, month, 1).atStartOfDay(LIMA).toInstant().toEpochMilli()

private fun receipt(id: Int, month: Int, due: String, paid: Boolean = false, year: Int = 2026) = Payment(
    id = id, billingDate = billing(year, month), amountToPay = 50.0, paid = paid, subscriptionId = 1842, dueDate = due,
)

class FichaModelTest {

    private val receipts = listOf(
        receipt(10, 10, "2026-10-15T00:00:00"),
        receipt(9, 9, "2026-09-15T00:00:00"),
        receipt(8, 8, "2026-08-15T00:00:00"),
        receipt(7, 7, "2026-07-15T00:00:00", paid = true),
    )

    @Test
    fun `cut subscriptions say since when`() {
        val header = FichaHeader.from(
            subscription(pendingInvoices = 2, totalDebt = 100.0).copy(lastCutOffDate = "2026-09-15"),
            receipts, TODAY,
        )

        assertEquals(
            listOf(StatusBadge("Cortado desde 15 set", BadgeTone.WARNING), StatusBadge("Debe 2 meses", BadgeTone.ERROR)),
            header.badges,
        )
        assertEquals("Jorge Ramos Villanueva", header.name)
        assertEquals("Cód. 1842 · Fibra · La Villa", header.subtitle)
    }

    @Test
    fun `debt card names the overdue months and the next due receipt`() {
        val header = FichaHeader.from(subscription(pendingInvoices = 2, totalDebt = 100.0), receipts, TODAY)

        assertEquals(DebtCard("S/ 100.00", "ago y set 2026 · oct vence mañana"), header.debt)
    }

    @Test
    fun `due dates read as today, tomorrow or a date`() {
        assertEquals("oct vence hoy", debtDetail(listOf(receipt(10, 10, "2026-10-14T00:00:00")), TODAY))
        assertEquals("oct vence el 20 oct", debtDetail(listOf(receipt(10, 10, "2026-10-20T00:00:00")), TODAY))
        assertEquals(
            "dic 2025 y ene 2026",
            debtDetail(listOf(receipt(1, 1, "2026-01-15T00:00:00"), receipt(12, 12, "2025-12-15T00:00:00", year = 2025)), TODAY),
        )
        assertEquals("jul, ago y set 2026", debtDetail(listOf(receipt(7, 7, "2026-07-15T00:00:00"), receipt(8, 8, "2026-08-15T00:00:00"), receipt(9, 9, "2026-09-15T00:00:00")), TODAY))
        assertNull(debtDetail(emptyList(), TODAY))
    }

    @Test
    fun `an up to date subscription has no debt card`() {
        val header = FichaHeader.from(subscription(status = ServiceStatus.ACTIVE, pendingInvoices = 0, totalDebt = 0.0), emptyList(), TODAY)

        assertNull(header.debt)
        assertEquals(listOf(StatusBadge("Activo", BadgeTone.SUCCESS)), header.badges)
    }

    @Test
    fun `restore needs fewer than two pending receipts`() {
        assertEquals(
            "Para restablecer debe quedar con menos de 2 recibos pendientes: cobra al menos 1 mes.",
            restoreHint(cut = true, pending = 2),
        )
        assertEquals(
            "Para restablecer debe quedar con menos de 2 recibos pendientes: cobra al menos 2 meses.",
            restoreHint(cut = true, pending = 3),
        )
        assertNull(restoreHint(cut = true, pending = 1))
        assertNull(restoreHint(cut = false, pending = 3))
        assertTrue(canRestore(cut = true, pending = 1))
        assertFalse(canRestore(cut = true, pending = 2))
        assertFalse(canRestore(cut = false, pending = 0))
    }

    @Test
    fun `the current Core sends no debt totals, so the unpaid receipts give the debt card`() {
        val header = FichaHeader.from(subscription(status = ServiceStatus.ACTIVE, pendingInvoices = 0, totalDebt = 0.0), receipts, TODAY)

        assertEquals(DebtCard("S/ 150.00", "ago y set 2026 · oct vence mañana"), header.debt)
        assertEquals(listOf(StatusBadge("Activo", BadgeTone.SUCCESS), StatusBadge("Debe 3 meses", BadgeTone.ERROR)), header.badges)
        assertEquals(3, header.pending)
    }

    @Test
    fun `the Core debt totals win over the loaded receipts`() {
        val header = FichaHeader.from(subscription(pendingInvoices = 5, totalDebt = 250.0), receipts, TODAY)

        assertEquals("S/ 250.00", header.debt?.amount)
        assertEquals(5, header.pending)
    }

    @Test
    fun `the current Core cut flag offers restore but is no cut badge`() {
        val flagged = subscription(status = ServiceStatus.ACTIVE, pendingInvoices = 0, totalDebt = 0.0)
            .copy(isServiceCutOff = true, lastCutOffDate = "2026-09-15", phone = null, location = GeoLocation())

        assertTrue(flagged.offersRestore())
        assertFalse(subscription(status = ServiceStatus.ACTIVE).offersRestore())
        assertTrue(subscription(status = ServiceStatus.CUT_OFF).offersRestore())
        assertEquals(listOf(FichaAction.RESTORE, FichaAction.TICKET), quickActions(flagged))
        assertEquals(listOf(StatusBadge("Activo", BadgeTone.SUCCESS)), FichaHeader.from(flagged, emptyList(), TODAY).badges)
    }

    @Test
    fun `service card reads like the design`() {
        val sub = subscription().copy(
            plan = PlanResponse(name = "basico", downloadSpeed = "200", uploadSpeed = "200", type = InstallationType.FIBER),
            price = 50.0,
            accessMode = "PPPOE_DYNAMIC",
            subscriptionDate = LocalDate.of(2025, 3, 12).atTime(10, 0).atZone(LIMA).toInstant().toEpochMilli(),
            equipmentCondition = "LOAN",
            technician = User(id = 3, name = "marco", lastName = "SALINAS"),
        )

        assertEquals(
            listOf(
                InfoRow("Plan", "Basico · 200/200 Mbps"),
                InfoRow("Mensualidad", "S/ 50.00"),
                InfoRow("Tipo", "Fibra · PPPoE"),
                InfoRow("Instalada", "12 mar 2025"),
                InfoRow("Equipo", "En préstamo"),
                InfoRow("Técnico", "Marco Salinas"),
            ),
            serviceRows(sub),
        )
    }

    @Test
    fun `holder and network cards skip what is missing`() {
        val sub = subscription().copy(
            dni = "70418293", phone = "987654321", address = "Jr. Los Pinos 214",
            napBox = NapBoxResponse(code = "LV-15-3", placeName = "La Villa", placeId = 1), borneNumber = "7", ip = "192.168.221.35",
        )

        assertEquals(
            listOf(InfoRow("DNI", "70418293"), InfoRow("Teléfono", "987 654 321"), InfoRow("Dirección", "Jr. Los Pinos 214"), InfoRow("Lugar", "La Villa")),
            holderRows(sub),
        )
        assertEquals(listOf(InfoRow("Caja NAP", "LV-15-3 · puerto 7"), InfoRow("IP", "192.168.221.35")), networkRows(sub))
    }

    @Test
    fun `quick actions follow the design and the data`() {
        val cut = subscription().copy(phone = "987654321", location = GeoLocation(-11.2, -77.3))
        assertEquals(
            listOf(FichaAction.CALL, FichaAction.WHATSAPP, FichaAction.RESTORE, FichaAction.TICKET, FichaAction.MAP),
            quickActions(cut),
        )
        val active = subscription(status = ServiceStatus.ACTIVE).copy(phone = null, location = GeoLocation())
        assertEquals(listOf(FichaAction.TICKET), quickActions(active))
    }
}
