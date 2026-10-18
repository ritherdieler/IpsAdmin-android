package com.dscorp.ispadmin.presentation.ui.features.payment.cash

import com.dscorp.ispadmin.data.datasource.remote.CashCollectorResponse
import com.dscorp.ispadmin.data.datasource.remote.CashDayResponse
import com.dscorp.ispadmin.data.datasource.remote.CashMethodResponse
import com.dscorp.ispadmin.data.datasource.remote.CashReceiptResponse
import com.dscorp.ispadmin.data.datasource.remote.MoneyCountResponse
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

internal fun receipt(id: Int, subscriptionId: Int, subscriber: String, amount: Double, method: String, paidAt: String,
                     month: String?, collector: String? = "Rosa Ccori", payer: String? = null) =
    CashReceiptResponse(id, subscriptionId, subscriber, amount, method, payer, collector, paidAt, month)

internal fun cashDay(date: String = "2026-10-14") = CashDayResponse(
    date = date,
    total = MoneyCountResponse(1692.0, 31),
    byMethod = listOf(CashMethodResponse("Yape", 1438.0, 26), CashMethodResponse("Efectivo", 203.0, 4), CashMethodResponse("Transferencia", 51.0, 1)),
    byCollector = listOf(CashCollectorResponse("Rosa Ccori", 1200.0, 24), CashCollectorResponse("Sergio Carrillo", 492.0, 7)),
    receipts = listOf(
        receipt(5, 1842, "JORGE RAMOS VILLANUEVA", 50.0, "Yape", "2026-10-14T11:42:10", "2026-09"),
        receipt(4, 1842, "JORGE RAMOS VILLANUEVA", 50.0, "Yape", "2026-10-14T11:42:05", "2026-08"),
        receipt(3, 1901, "HILDA CCORI MAMANI", 50.0, "Yape", "2026-10-14T11:30:00", "2026-10", payer = "Hilda Ccori M."),
        receipt(2, 1777, "ROSA RAMIREZ CCORI", 50.0, "Efectivo", "2026-10-14T11:05:00", "2026-10"),
    ),
)

class CashDayModelTest {
    private val today = LocalDate.of(2026, 10, 14)

    @Test
    fun `the header reads the day and who collected`() {
        val day = CashDay.from(cashDay(), today)

        assertEquals("Cobrado hoy · mié 14 oct", day.title)
        assertEquals("S/ 1,692.00", day.amount)
        assertEquals("31 cobros · Rosa C. 24 · Sergio C. 7", day.subtitle)
        assertEquals("Cobrado el mar 13 oct", CashDay.from(cashDay("2026-10-13"), today).title)
    }

    @Test
    fun `methods keep their share of the day`() {
        val methods = CashDay.from(cashDay(), today).methods

        assertEquals(listOf("Yape", "Efectivo", "Transferencia"), methods.map { it.label })
        assertEquals("S/ 1,438.00", methods[0].amount)
        assertEquals("· 26", methods[0].count)
        assertEquals(1438.0 / 1692.0, methods[0].share.toDouble(), 0.001)
    }

    @Test
    fun `receipts of one charge read as one row with its months`() {
        val rows = CashDay.from(cashDay(), today).rows

        assertEquals(3, rows.size)
        assertEquals(CashRow(1842, "11:42", "Jorge Ramos Villanueva", "Yape · ago y set · Rosa C.", "S/ 100.00"), rows[0])
        assertEquals("Yape · pagó Hilda Ccori M.", rows[1].detail)
        assertEquals("Efectivo · oct · Rosa C.", rows[2].detail)
    }

    @Test
    fun `month lists read naturally`() {
        assertEquals("oct", monthsLabel(listOf("2026-10")))
        assertEquals("ago y set", monthsLabel(listOf("2026-09", "2026-08")))
        assertEquals("jul, ago y set", monthsLabel(listOf("2026-07", "2026-08", "2026-09")))
        assertEquals("Rosa C.", shortName("Rosa Ccori"))
        assertEquals("Ana", shortName("Ana"))
    }

    @Test
    fun `two payments for the same month stay as two rows`() {
        val day = cashDay().copy(
            receipts = listOf(
                receipt(9, 2001, "NOLBERTA IBARRA VEGA", 30.0, "Efectivo", "2026-10-14T11:43:00", "2026-09"),
                receipt(8, 2001, "NOLBERTA IBARRA VEGA", 30.0, "Efectivo", "2026-10-14T11:42:30", "2026-09"),
            ),
        )

        assertEquals(listOf("S/ 30.00", "S/ 30.00"), CashDay.from(day, today).rows.map { it.amount })
    }
}
