package com.dscorp.ispadmin.presentation.ui.features.home

import com.dscorp.ispadmin.data.datasource.remote.HomeCollectionsResponse
import com.dscorp.ispadmin.data.datasource.remote.HomeCutResponse
import com.dscorp.ispadmin.data.datasource.remote.HomeOwingResponse
import com.dscorp.ispadmin.data.datasource.remote.HomeSummaryResponse
import com.dscorp.ispadmin.data.datasource.remote.HomeTicketsResponse
import com.dscorp.ispadmin.data.datasource.remote.MoneyCountResponse
import com.dscorp.ispadmin.data.datasource.remote.OwingSubscriptionResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal fun homeSummary(
    today: MoneyCountResponse = MoneyCountResponse(1692.0, 31),
    lastWeek: MoneyCountResponse = MoneyCountResponse(1552.0, 28),
    cut: HomeCutResponse = HomeCutResponse("2026-10-15", 353, automatic = true),
    owing: HomeOwingResponse = HomeOwingResponse(
        21,
        listOf(
            OwingSubscriptionResponse(1842, "JORGE RAMOS VILLANUEVA", "Basico", "la villa", 100.0),
            OwingSubscriptionResponse(1901, "HILDA CCORI MAMANI", "Basico wireless 50", null, 100.0),
        ),
    ),
    tickets: HomeTicketsResponse = HomeTicketsResponse(4, 1),
) = HomeSummaryResponse(
    asOf = "2026-10-14T10:42:00",
    collections = HomeCollectionsResponse(
        today = today,
        sameWeekdayLastWeek = lastWeek,
        lastWeekDate = "2026-10-07",
        monthToDate = MoneyCountResponse(23688.0, 410),
        previousMonthToDate = MoneyCountResponse(22548.0, 395),
        daily = emptyList(),
    ),
    cut = cut,
    owingTwoMonths = owing,
    tickets = tickets,
)

class HomeModelTest {

    @Test
    fun `the header shows today's collections against the same weekday last week`() {
        val home = SecretaryHome.from(homeSummary())

        assertEquals("Cobrado hoy · mié 14 oct", home.todayTitle)
        assertEquals("S/ 1,692.00", home.todayAmount)
        assertEquals("31 cobros", home.todayCount)
        assertEquals(Delta("▲ S/ 140.00", positive = true), home.delta)
        assertEquals("vs mié 7 oct", home.deltaVs)
    }

    @Test
    fun `a slower day reads as a drop, one receipt in singular and no movement has no delta`() {
        val slower = SecretaryHome.from(homeSummary(today = MoneyCountResponse(50.0, 1), lastWeek = MoneyCountResponse(90.0, 2)))
        assertEquals("1 cobro", slower.todayCount)
        assertEquals(Delta("▼ S/ 40.00", positive = false), slower.delta)

        assertNull(SecretaryHome.from(homeSummary(today = MoneyCountResponse(0.0, 0), lastWeek = MoneyCountResponse(0.0, 0))).delta)
    }

    @Test
    fun `attention lists the next cut, who owes two months and pending tickets`() {
        val rows = SecretaryHome.from(homeSummary()).attention

        assertEquals(
            listOf(
                AttentionRow(AttentionKind.CUT, "Corte automático esta medianoche", "353 suscripciones con recibo pendiente"),
                AttentionRow(AttentionKind.OWING, "21 deben 2 meses", "Cobra antes del corte del 15 oct"),
                AttentionRow(AttentionKind.TICKETS, "4 tickets pendientes", "1 de prioridad alta"),
            ),
            rows,
        )
    }

    @Test
    fun `a later or manual cut names its day and empty items are left out`() {
        val later = SecretaryHome.from(
            homeSummary(
                cut = HomeCutResponse("2026-11-16", 1, automatic = false),
                owing = HomeOwingResponse(0, emptyList()),
                tickets = HomeTicketsResponse(1, 0),
            ),
        ).attention

        assertEquals(
            listOf(
                AttentionRow(AttentionKind.CUT, "Corte del lun 16 nov", "1 suscripción con recibo pendiente"),
                AttentionRow(AttentionKind.TICKETS, "1 ticket pendiente", "Ninguno de prioridad alta"),
            ),
            later,
        )
        assertTrue(
            SecretaryHome.from(
                homeSummary(cut = HomeCutResponse("2026-10-15", 0, true), owing = HomeOwingResponse(0, emptyList()), tickets = HomeTicketsResponse(0, 0)),
            ).attention.isEmpty(),
        )
    }

    @Test
    fun `subscribers owing two months read in title case with plan, place and debt`() {
        val home = SecretaryHome.from(homeSummary())

        assertEquals("Deben 2 meses · 21", home.owingTitle)
        assertEquals(
            listOf(
                OwingRow(1842, "JR", "Jorge Ramos Villanueva", "Basico · La Villa", "S/ 100.00"),
                OwingRow(1901, "HC", "Hilda Ccori Mamani", "Basico wireless 50", "S/ 100.00"),
            ),
            home.owing,
        )
    }
}
