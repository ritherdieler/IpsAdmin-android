package com.dscorp.ispadmin.presentation.ui.features.dashboard

import com.dscorp.ispadmin.domain.model.MonthlyGrossRevenueResume
import com.dscorp.ispadmin.presentation.designsystem.charts.NiceScale
import com.dscorp.ispadmin.presentation.designsystem.charts.divergingScale
import com.dscorp.ispadmin.presentation.designsystem.charts.niceScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

class DashboardLogicTest {
    private val oct = YearMonth.of(2026, 10)
    private fun trend(vararg values: Double, last: YearMonth = oct) =
        MonthlyTrend(values.indices.map { last.minusMonths((values.size - 1 - it).toLong()) }, values.toList())

    @Test fun `long rankings keep the largest entries and fold the rest into one row`() {
        val counts = listOf("a" to 10, "b" to 8, "c" to 5, "d" to 3, "e" to 2, "f" to 1, "g" to 1)

        assertEquals(listOf("a" to 10, "b" to 8, "c" to 5, "d" to 3, "Otros" to 4), topWithOthers(counts, keep = 4, othersLabel = "Otros"))
    }

    @Test fun `the folded rest joins an existing entry with the same label`() {
        val counts = listOf("Sin internet" to 248, "Otros" to 247, "Lento" to 139, "Instalación" to 102, "TV" to 28, "Clave" to 19)

        // "Otros" always closes the ranking, even when the folded rest makes it the largest row.
        assertEquals(listOf("Sin internet" to 248, "Lento" to 139, "Instalación" to 102, "TV" to 28, "Otros" to 266),
            topWithOthers(counts, keep = 4, othersLabel = "Otros"))
    }

    @Test fun `a ranking with at most one extra entry is shown whole`() {
        val counts = listOf("a" to 10, "b" to 8, "c" to 5, "d" to 3, "e" to 2)

        assertEquals(counts, topWithOthers(counts, keep = 4, othersLabel = "Otros"))
    }

    @Test fun `zone revenue in the tens of thousands is shortened to fit its column`() {
        assertEquals("S/ 13.6k", formatSolesCompact(13_600.0))
        assertEquals("S/ 986.7M", formatSolesCompact(986_690_708.0))
        assertEquals("S/ 863.00", formatSolesCompact(863.0))
    }

    @Test fun `axes use round steps and never split a count`() {
        assertEquals(NiceScale(max = 60_000.0, step = 20_000.0), niceScale(57_900.0))
        assertEquals(NiceScale(max = 40.0, step = 10.0), niceScale(35.0, integers = true))
        assertEquals(NiceScale(max = 4.0, step = 1.0), niceScale(4.0, integers = true))
        assertEquals(NiceScale(max = 1.0, step = 1.0), niceScale(0.0, integers = true))
    }

    @Test fun `a diverging chart gets the same round scale above and below zero`() {
        assertEquals(-40.0..40.0, divergingScale(up = 33.0, down = 35.0).let { it.min..it.max })
    }

    @Test fun `net adds are new plus reconnected minus cancelled in the current month`() {
        assertEquals(13, netAdds(new = trend(25.0, 7.0), reconnections = trend(6.0, 8.0), cancellations = trend(19.0, 2.0)))
        assertNull(netAdds(new = trend(), reconnections = trend(8.0), cancellations = trend(2.0)))
    }

    @Test fun `digital payments keep only the months of the window`() {
        val resume = mapOf("Feb." to 80.0, "Mar." to 81.0, "Abr." to 82.0, "May." to 83.0, "Jun." to 84.0,
            "Jul." to 85.0, "Ago." to 86.0, "Set." to 87.0, "Oct." to 29.0)

        val digital = digitalPayments(resume, oct)

        assertEquals((5 downTo 0).map { oct.minusMonths(it.toLong()) }, digital.months)
        assertEquals(29.0, digital.current)
    }

    @Test fun `trend average ignores nothing and is null without data`() {
        assertEquals(3.0, trend(2.0, 4.0).average)
        assertNull(trend().average)
    }

    @Test fun `a trend whose last month is not the current one reports that month`() {
        assertEquals(YearMonth.of(2026, 9), staleMonth(trend(48.0, 49.77, last = YearMonth.of(2026, 9)), oct))
        assertNull(staleMonth(trend(48.0, 49.77), oct))
    }

    @Test fun `months not billed yet at the end of the history are left out`() {
        val history = listOf(
            MonthlyGrossRevenueResume(48_000.0, "2026-08-01"),
            MonthlyGrossRevenueResume(49_000.0, "2026-09-01"),
            MonthlyGrossRevenueResume(0.0, "2026-10-01"),
        )

        assertEquals(listOf("2026-08-01", "2026-09-01"), billedMonths(history).map { it.billingMonth })
    }

    @Test fun `the month in progress is left out even when a few invoices are already billed`() {
        val history = listOf(
            MonthlyGrossRevenueResume(48_000.0, "2026-08-01"),
            MonthlyGrossRevenueResume(49_000.0, "2026-09-01"),
            MonthlyGrossRevenueResume(200.0, "2026-10-01"),
        )

        assertEquals(listOf("2026-08-01", "2026-09-01"), billedMonths(history, currentMonth = oct).map { it.billingMonth })
    }

    @Test fun `percent change is relative to the earlier value and unknown from zero`() {
        assertEquals(14.5, percentChange(from = 44.0, to = 50.4) ?: Double.NaN, 0.05)
        assertNull(percentChange(from = 0.0, to = 10.0))
    }

    @Test fun `shares split a whole into percentages`() {
        assertEquals(listOf(75.0, 25.0), shares(listOf(3.0, 1.0)))
        assertEquals(listOf(0.0, 0.0), shares(listOf(0.0, 0.0)))
    }
}
