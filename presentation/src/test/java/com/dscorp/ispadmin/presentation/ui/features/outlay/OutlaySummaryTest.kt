package com.dscorp.ispadmin.presentation.ui.features.outlay

import org.junit.Assert.assertEquals
import org.junit.Test

class OutlaySummaryTest {
    private fun summary(spent: Double) = OutlaySummary(
        spent = spent, limit = 3000.0, available = (3000.0 - spent).coerceAtLeast(0.0), threshold = 2500.0,
        fixedTotal = 4150.0, previousFixedTotal = 4100.0,
    )

    @Test
    fun `the level follows the threshold and the limit`() {
        assertEquals(LimitLevel.InRange, summary(2499.99).level)
        assertEquals(LimitLevel.NearLimit, summary(2500.0).level)
        assertEquals(LimitLevel.NearLimit, summary(2999.99).level)
        assertEquals(LimitLevel.Reached, summary(3000.0).level)
    }

    @Test
    fun `used fraction is capped at one`() {
        assertEquals(0.88, summary(2640.0).usedFraction, 0.001)
        assertEquals(1.0, summary(3500.0).usedFraction, 0.0)
    }

    @Test
    fun `fixed variation compares with the previous month`() {
        assertEquals(50.0, summary(0.0).fixedChange, 0.001)
    }
}
