package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub

import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.Payment
import com.dscorp.ispadmin.domain.model.PaymentMethodOption
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal fun subscription(
    status: ServiceStatus = ServiceStatus.CUT_OFF,
    pendingInvoices: Int = 2,
    totalDebt: Double = 100.0,
    firstName: String = "JORGE",
    lastName: String = "RAMOS  villanueva",
) = SubscriptionResponse(
    id = 1842,
    firstName = firstName,
    lastName = lastName,
    place = Place(name = "la villa"),
    serviceStatus = status,
    isMigration = false,
    installationType = InstallationType.FIBER,
    email = null,
    pendingInvoiceQuantity = pendingInvoices,
    antiquityInMonths = 19,
    qualification = 0,
    ics = 0,
    totalDebt = totalDebt,
    lastPaymentDate = null,
)

internal fun receipt(id: Int, billingDate: Long, amount: Double = 50.0, paid: Boolean = false) =
    Payment(id = id, billingDate = billingDate, amountToPay = amount, paid = paid, subscriptionId = 1842)

internal val METHODS = listOf(
    PaymentMethodOption("Efectivo"),
    PaymentMethodOption("Yape", requiresPayerName = true),
    PaymentMethodOption("Plin", requiresPayerName = true),
    PaymentMethodOption("Transferencia"),
)

class SubscriptionHubModelTest {

    @Test
    fun `names are shown in title case whatever the stored casing`() {
        assertEquals("Jorge Ramos Villanueva", displayName("JORGE", "RAMOS  villanueva"))
        assertEquals("Ana", displayName("ana", null))
    }

    @Test
    fun `money uses soles with two decimals and es-PE grouping`() {
        assertEquals("S/ 1,692.00", formatSoles(1692.0))
        assertEquals("S/ 50.00", formatSoles(50.0))
    }

    @Test
    fun `header of a cut subscription with debt`() {
        val header = HubHeader.from(subscription())

        assertEquals("Jorge Ramos Villanueva", header.name)
        assertEquals("Cód. 1842 · Fibra · La Villa", header.subtitle)
        assertEquals(StatusBadge("Cortado", BadgeTone.WARNING), header.status)
        assertEquals(StatusBadge("Debe 2 meses", BadgeTone.ERROR), header.debtBadge)
        assertEquals("S/ 100.00", header.debtAmount)
        assertTrue(header.canCharge)
    }

    @Test
    fun `one pending month reads in singular and an up to date subscription has no debt`() {
        assertEquals("Debe 1 mes", HubHeader.from(subscription(pendingInvoices = 1, totalDebt = 50.0)).debtBadge?.text)
        val upToDate = HubHeader.from(subscription(status = ServiceStatus.ACTIVE, pendingInvoices = 0, totalDebt = 0.0))
        assertEquals(StatusBadge("Activo", BadgeTone.SUCCESS), upToDate.status)
        assertNull(upToDate.debtBadge)
        assertFalse(upToDate.canCharge)
    }

    @Test
    fun `the current Core sends no debt totals, so the unpaid receipts give the header debt`() {
        val header = HubHeader.from(
            subscription(status = ServiceStatus.ACTIVE, pendingInvoices = 0, totalDebt = 0.0),
            receipts = listOf(receipt(1, 100), receipt(2, 200, amount = 80.0), receipt(3, 300, paid = true)),
        )

        assertEquals(StatusBadge("Activo", BadgeTone.SUCCESS), header.status)
        assertEquals(StatusBadge("Debe 2 meses", BadgeTone.ERROR), header.debtBadge)
        assertEquals("S/ 130.00", header.debtAmount)
        assertTrue(header.canCharge)
        assertEquals("S/ 100.00", HubHeader.from(subscription(), receipts = listOf(receipt(1, 100))).debtAmount)
    }

    @Test
    fun `charge preselects every unpaid receipt oldest first`() {
        val draft = ChargeDraft.start(
            receipts = listOf(receipt(3, 300), receipt(1, 100), receipt(2, 200), receipt(9, 50, paid = true)),
            methods = METHODS,
            status = ServiceStatus.CUT_OFF,
        )

        assertEquals(listOf(1, 2, 3), draft.receipts.map { it.id })
        assertEquals(setOf(1, 2, 3), draft.selectedIds)
        assertEquals(150.0, draft.total, 0.001)
        assertEquals(0, draft.pendingAfter)
    }

    @Test
    fun `yape is the default method because it is most of the payments`() {
        assertEquals("Yape", ChargeDraft.start(listOf(receipt(1, 100)), METHODS, ServiceStatus.ACTIVE).method)
        assertEquals("Efectivo", ChargeDraft.start(listOf(receipt(1, 100)), METHODS.filter { it.name != "Yape" }, ServiceStatus.ACTIVE).method)
    }

    @Test
    fun `payer name is required only for methods that ask for it`() {
        val yape = ChargeDraft.start(listOf(receipt(1, 100)), METHODS, ServiceStatus.ACTIVE)
        assertEquals("Escribe el nombre que aparece en Yape", yape.blockingReason)

        assertNull(yape.copy(payerName = "Jorge Ramos V.").blockingReason)
        assertNull(yape.copy(method = "Efectivo").blockingReason)
    }

    @Test
    fun `at least one receipt must be selected`() {
        val draft = ChargeDraft.start(listOf(receipt(1, 100)), METHODS, ServiceStatus.ACTIVE)
            .copy(method = "Efectivo")
            .toggle(1)

        assertEquals(0.0, draft.total, 0.001)
        assertEquals("Elige al menos un recibo", draft.blockingReason)
    }

    @Test
    fun `restore is offered when the cut subscription ends with fewer than two pending receipts`() {
        val receipts = listOf(receipt(1, 100), receipt(2, 200), receipt(3, 300))
        val all = ChargeDraft.start(receipts, METHODS, ServiceStatus.CUT_OFF)
        assertTrue(all.canRestore)
        assertTrue(all.restore)

        val onlyOne = all.toggle(2).toggle(3)
        assertEquals(2, onlyOne.pendingAfter)
        assertFalse(onlyOne.canRestore)

        assertFalse(ChargeDraft.start(receipts, METHODS, ServiceStatus.ACTIVE).canRestore)
    }

    @Test
    fun `the current Core cut flag offers the restore toggle without turning it on`() {
        val flagged = ChargeDraft.start(listOf(receipt(1, 100)), METHODS, ServiceStatus.ACTIVE, cutFlagged = true)

        assertTrue(flagged.canRestore)
        assertFalse(flagged.restore)
        val three = listOf(receipt(1, 100), receipt(2, 200), receipt(3, 300))
        assertFalse(ChargeDraft.start(three, METHODS, ServiceStatus.ACTIVE, cutFlagged = true).toggle(2).toggle(3).canRestore)
    }

    private val fifty = listOf(receipt(1, 100, amount = 50.0), receipt(2, 200, amount = 50.0))

    @Test
    fun `cash shows the change and blocks when it does not cover the total`() {
        val cash = ChargeDraft.start(fifty, METHODS, ServiceStatus.ACTIVE).copy(method = "Efectivo")

        assertEquals(10.0, cash.copy(received = "110").change!!, 0.001)
        assertNull(cash.copy(received = "110").blockingReason)
        assertEquals("Lo recibido no alcanza para el total", cash.copy(received = "80").blockingReason)
        assertNull(cash.copy(method = "Yape", payerName = "Ana", received = "80").blockingReason)
    }

    @Test
    fun `a discount lowers the total and needs a reason`() {
        val discounted = ChargeDraft.start(fifty, METHODS, ServiceStatus.ACTIVE).copy(method = "Efectivo", discountOpen = true, discount = "10")

        assertEquals(90.0, discounted.toPay, 0.001)
        assertEquals("Elige el motivo del descuento", discounted.blockingReason)
        assertNull(discounted.copy(discountReason = "Promoción").blockingReason)
        assertEquals("El descuento no puede ser mayor al total", discounted.copy(discount = "200", discountReason = "Otro").blockingReason)
        assertEquals(100.0, discounted.copy(discountOpen = false).toPay, 0.001)
    }

    @Test
    fun `the last method used comes first`() {
        assertEquals("Efectivo", ChargeDraft.start(fifty, METHODS, ServiceStatus.ACTIVE, lastUsed = "Efectivo").method)
        assertEquals("Yape", ChargeDraft.start(fifty, METHODS, ServiceStatus.ACTIVE, lastUsed = "Bitcoin").method)
    }

    @Test
    fun `receipts say whether they are overdue`() {
        val today = java.time.LocalDate.of(2026, 10, 14)
        fun due(date: String?) = receiptStatus(Payment(id = 1, dueDate = date), today)

        assertEquals(StatusBadge("Vencido", BadgeTone.ERROR), due("2026-10-13T00:00:00"))
        assertEquals(StatusBadge("Vence hoy", BadgeTone.WARNING), due("2026-10-14T00:00:00"))
        assertEquals(StatusBadge("Vence mañana", BadgeTone.WARNING), due("2026-10-15T00:00:00"))
        assertEquals(StatusBadge("Pendiente", BadgeTone.NEUTRAL), due("2026-10-30T00:00:00"))
        assertNull(due(null))
    }

    @Test
    fun `the charge subtitle names the plan as the ficha does`() {
        val withPlan = subscription().apply {
            plan = com.dscorp.ispadmin.domain.model.PlanResponse("9", "basico_wireless 50", 50.0, "20", "20", InstallationType.WIRELESS)
        }
        assertEquals("Cód. 1842 · Basico wireless 50 · S/ 50.00 al mes", chargeSubtitle(withPlan))
    }
}
