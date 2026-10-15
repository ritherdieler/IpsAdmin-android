package com.dscorp.ispadmin.presentation.ui.features.financial

import com.dscorp.ispadmin.domain.model.financial.FinancialReceipt
import com.dscorp.ispadmin.domain.model.financial.FinancialReport
import com.dscorp.ispadmin.domain.model.financial.MovementSummary
import com.dscorp.ispadmin.domain.model.financial.ReceiptInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class FinancialComparisonTest {
    private fun movements(received: String) = MovementSummary(BigDecimal(received), BigDecimal.ZERO, BigDecimal(received), BigDecimal.ZERO, BigDecimal(received))
    private fun receipt(id: Int, amount: String, paidAt: String) =
        FinancialReceipt(ReceiptInput(id, BigDecimal(amount), paidAt, "Yape", "Ana", emptyList()), emptyList())

    private val current = FinancialReport(id = "oct", movementsMonth = "2026-10", movements = movements("18708"))
    private val previous = FinancialReport(id = "sep", movementsMonth = "2026-09", movements = movements("45000"), receipts = listOf(
        receipt(1, "10000", "2026-09-05T10:00:00"),
        receipt(2, "5000", "2026-09-11T23:00:00"),
        receipt(3, "30000", "2026-09-20T09:00:00"),
    ))

    @Test fun `a month in progress is compared with the previous month up to the same day`() {
        // 18,708 against the 15,000 received from 1 to 11 September.
        assertEquals(24.72, receivedChange(current, previous, sameDay = 11) ?: Double.NaN, 0.01)
    }

    @Test fun `a finished month is compared with the whole previous month`() {
        assertEquals(-58.43, receivedChange(current, previous, sameDay = null) ?: Double.NaN, 0.01)
    }

    @Test fun `there is no comparison without money received in the previous period`() {
        assertNull(receivedChange(current, previous.copy(receipts = emptyList()), sameDay = 11))
    }
}
