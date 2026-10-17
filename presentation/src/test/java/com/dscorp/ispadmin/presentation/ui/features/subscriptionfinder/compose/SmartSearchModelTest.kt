package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import com.dscorp.ispadmin.domain.model.CustomerData
import com.dscorp.ispadmin.domain.model.GeoLocation
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionResume
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal fun row(
    id: Int = 1842,
    name: String = "JORGE RAMOS  villanueva",
    plan: String = "Basico",
    place: String = "la villa",
    status: ServiceStatus = ServiceStatus.CUT_OFF,
    type: InstallationType = InstallationType.FIBER,
    debt: Double = 100.0,
) = SubscriptionResume(
    id = id, planName = plan, customerName = name, antiquity = "19", qualification = "0",
    placeName = place, ics = "0", lastPaymentDate = null, pendingInvoicesQuantity = if (debt > 0) 2 else 0,
    totalDebt = debt, ipAddress = "192.168.26.24",
    customer = CustomerData(id, "JORGE", "RAMOS", "12345678", place, "Calle 1", "999999999", ""),
    serviceStatus = status, installationType = type, napBox = null, placeId = "1", location = GeoLocation(),
)

class SmartSearchModelTest {

    @Test
    fun `one box recognizes ip, document, code and name`() {
        assertEquals(SubscriptionFilter.BY_IP("192.168.26.24"), SmartQuery.toFilter(" 192.168.26.24 "))
        assertEquals(SubscriptionFilter.BY_DOCUMENT("45781236"), SmartQuery.toFilter("45781236"))
        assertEquals(SubscriptionFilter.BY_DOCUMENT("20601234567"), SmartQuery.toFilter("20601234567"))
        assertEquals(SubscriptionFilter.BY_CODE("1842"), SmartQuery.toFilter("1842"))
        assertEquals(SubscriptionFilter.BY_NAME("JORGE RAMOS", ""), SmartQuery.toFilter("jorge ramos"))
        // Partial documents fall back to the free-text search, which also indexes the DNI.
        assertEquals(SubscriptionFilter.BY_NAME("4578123", ""), SmartQuery.toFilter("4578123"))
        assertNull(SmartQuery.toFilter("   "))
    }

    @Test
    fun `the box tells what it understood`() {
        assertEquals("IP", SmartQuery.kindLabel("192.168.26.24"))
        assertEquals("DNI o RUC", SmartQuery.kindLabel("45781236"))
        assertEquals("Código", SmartQuery.kindLabel("1842"))
        assertEquals("Nombre", SmartQuery.kindLabel("ramos"))
        assertNull(SmartQuery.kindLabel(""))
    }

    @Test
    fun `a row shows name in title case, plan and place, state and debt`() {
        val model = SubscriptionRowModel.from(row())

        assertEquals("Jorge Ramos Villanueva", model.name)
        assertEquals("JR", model.initials)
        assertEquals("Basico · La Villa", model.subtitle)
        assertEquals(StatusBadge("Cortado", BadgeTone.WARNING), model.status)
        assertEquals("S/ 100.00", model.debt)
        assertTrue(model.owes)
    }

    @Test
    fun `an up to date subscription reads al dia`() {
        val model = SubscriptionRowModel.from(row(status = ServiceStatus.ACTIVE, debt = 0.0))

        assertEquals("Al día", model.debt)
        assertFalse(model.owes)
        assertEquals(StatusBadge("Activo", BadgeTone.SUCCESS), model.status)
    }

    @Test
    fun `the typed text is highlighted in the name ignoring case and accents`() {
        assertEquals(listOf(6..8), highlightRanges("Jorge Ramos Villanueva", "RAM"))
        assertEquals(listOf(0..4, 6..10), highlightRanges("Jorge Ramos Villanueva", "jorge ramos"))
        assertEquals(listOf(0..4), highlightRanges("Ramón Quispe", "ramon"))
        assertEquals(emptyList<IntRange>(), highlightRanges("Ana Paredes", "1842"))
    }

    @Test
    fun `chips filter the loaded results`() {
        val list = listOf(
            row(id = 1, status = ServiceStatus.CUT_OFF, type = InstallationType.FIBER, debt = 100.0),
            row(id = 2, status = ServiceStatus.ACTIVE, type = InstallationType.WIRELESS, debt = 0.0),
            row(id = 3, status = ServiceStatus.ACTIVE, type = InstallationType.ONLY_TV_FIBER, debt = 50.0),
        )

        assertEquals(listOf(1, 2, 3), list.filter(ResultFilter.ALL::matches).map { it.id })
        assertEquals(listOf(1, 3), list.filter(ResultFilter.WITH_DEBT::matches).map { it.id })
        assertEquals(listOf(1), list.filter(ResultFilter.CUT_OFF::matches).map { it.id })
        assertEquals(listOf(1), list.filter(ResultFilter.FIBER::matches).map { it.id })
        assertEquals(listOf(2), list.filter(ResultFilter.WIRELESS::matches).map { it.id })
        assertEquals(listOf(3), list.filter(ResultFilter.ONLY_TV::matches).map { it.id })
        assertEquals(
            listOf("Todos", "Con deuda", "Cortados", "Fibra", "Inalámbrico", "Solo TV"),
            ResultFilter.entries.map { it.label },
        )
    }

    @Test
    fun `result count says when more pages exist`() {
        assertEquals("24 resultados", resultCountLabel(24, canLoadMore = false))
        assertEquals("1 resultado", resultCountLabel(1, canLoadMore = false))
        assertEquals("20+ resultados", resultCountLabel(20, canLoadMore = true))
    }

    @Test
    fun `plan names read like the design`() {
        assertEquals("Basico wireless 50 · La Villa", SubscriptionRowModel.from(row(plan = "basico_wireless 50")).subtitle)
        assertEquals("Basico · La Villa", SubscriptionRowModel.from(row(plan = "basico")).subtitle)
        assertEquals("Internet100 · La Villa", SubscriptionRowModel.from(row(plan = "Internet100")).subtitle)
    }

    @Test
    fun `cancelled subscriptions go after the rest, keeping the search order`() {
        val state = SubscriptionFinderUiState(
            subscriptions = mapOf(
                ServiceStatus.CANCELLED to listOf(row(id = 1, status = ServiceStatus.CANCELLED), row(id = 3, status = ServiceStatus.CANCELLED)),
                ServiceStatus.ACTIVE to listOf(row(id = 2), row(id = 4, status = ServiceStatus.ACTIVE)),
            )
        )

        assertEquals(listOf(2, 4, 1, 3), state.results.map { it.id })
    }
}
