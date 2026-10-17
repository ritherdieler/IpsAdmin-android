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
}
