package com.dscorp.ispadmin.presentation.ui.features.home

import com.dscorp.ispadmin.data.datasource.remote.DailyCollectionResponse
import com.dscorp.ispadmin.data.datasource.remote.HomeCollectionsResponse
import com.dscorp.ispadmin.data.datasource.remote.HomeCutResponse
import com.dscorp.ispadmin.data.datasource.remote.HomeSubscriptionsResponse
import com.dscorp.ispadmin.data.datasource.remote.HomeSummaryResponse
import com.dscorp.ispadmin.data.datasource.remote.HomeTicketsResponse
import com.dscorp.ispadmin.data.datasource.remote.MoneyCountResponse
import com.dscorp.ispadmin.presentation.designsystem.components.DeltaTone
import com.dscorp.ispadmin.presentation.designsystem.components.KpiDelta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

internal fun adminSummary(
    monthToDate: Double = 23688.0,
    previous: Double = 22548.0,
) = HomeSummaryResponse(
    asOf = "2026-10-14T10:42:00",
    collections = HomeCollectionsResponse(
        today = MoneyCountResponse(3900.0, 31),
        sameWeekdayLastWeek = MoneyCountResponse(3000.0, 25),
        lastWeekDate = "2026-10-07",
        monthToDate = MoneyCountResponse(monthToDate, 410),
        previousMonthToDate = MoneyCountResponse(previous, 395),
        daily = listOf(
            DailyCollectionResponse("2026-10-01", 1200.0, 10),
            DailyCollectionResponse("2026-10-02", 0.0, 0),
            DailyCollectionResponse("2026-10-03", 3900.0, 31),
        ),
    ),
    subscriptions = HomeSubscriptionsResponse(active = 426, cutOff = 490),
    overdue = MoneyCountResponse(18700.0, 353),
    cut = HomeCutResponse("2026-10-15", 353, automatic = true),
    tickets = HomeTicketsResponse(4, 1),
)

class AdminHomeModelTest {

    @Test
    fun `the hero compares the month so far with the previous month at the same day`() {
        val home = AdminHome.from(adminSummary())

        assertEquals("Corte 15 oct · actualizado 10:42", home.subtitle)
        assertEquals("Cobrado en oct (al día 14)", home.monthTitle)
        assertEquals("S/ 23,688.00", home.monthAmount)
        assertEquals(KpiDelta("▲ S/ 1,140.00", DeltaTone.Positive, "vs set al día 14"), home.monthDelta)

        assertEquals(KpiDelta("▼ S/ 500.00", DeltaTone.Negative, "vs set al día 14"), AdminHome.from(adminSummary(monthToDate = 100.0, previous = 600.0)).monthDelta)
        assertNull(AdminHome.from(adminSummary(monthToDate = 0.0, previous = 0.0)).monthDelta)
    }

    @Test
    fun `four tiles summarise service, cuts, debt and tickets`() {
        val tiles = AdminHome.from(adminSummary()).tiles

        assertEquals(
            listOf(
                AdminTile(AdminTileKind.WITH_SERVICE, "Con servicio", "426", "de 916 activos"),
                AdminTile(AdminTileKind.CUT_OFF, "Cortados", "490", "con deuda vencida"),
                AdminTile(AdminTileKind.RECEIVABLE, "Por cobrar", "S/ 18,700.00", "353 abonados con deuda"),
                AdminTile(AdminTileKind.TICKETS, "Tickets pendientes", "4", "1 de prioridad alta"),
            ),
            tiles,
        )
    }

    @Test
    fun `the daily chart covers the month with its average and today's count`() {
        val home = AdminHome.from(adminSummary())

        assertEquals("Cobros por día · oct", home.dailyTitle)
        assertEquals(listOf("1", "2", "3"), home.dailyLabels)
        assertEquals(listOf(1200.0, 0.0, 3900.0), home.dailyValues)
        assertEquals("Hoy · 31 cobros", home.todayCaption)
    }
}
