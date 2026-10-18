package com.dscorp.ispadmin.presentation.ui.features.outlay

import com.dscorp.ispadmin.data.model.OutlayDTO
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

internal fun outlay(id: Int, amount: Double, description: String, category: String?, center: String?, who: String?, day: Int) =
    OutlayDTO(id = id, amount = amount, description = description, category = category, cost_center = center, responsibleName = who,
        date = Date.from(LocalDate.of(2026, 10, day).atTime(10, 0).atZone(ZoneId.of("America/Lima")).toInstant()))

internal val OCTOBER = listOf(
    outlay(1, 85.0, "Combustible de la moto", "Transporte", "Técnico", "Diego Huerta", 14),
    outlay(2, 120.0, "Conectores SC/APC x50", "Suministros", "Técnico", "Marco Salinas", 12),
    outlay(3, 200.0, "Flete de bobina de fibra", "Transporte", "Técnico", "Sergio Carrillo", 2),
    outlay(4, 64.0, "Agua y limpieza", "Otros", "Administración", "Rosa Ccori", 6),
)

class OutlayMonthTest {
    private val today = LocalDate.of(2026, 10, 14)

    @Test
    fun `the month sums its outlays and splits the biggest categories`() {
        val month = OutlayMonth.from(OCTOBER, null, today)

        assertEquals("oct 2026", month.title)
        assertEquals("S/ 469.00", month.total)
        assertEquals("· 4 egresos", month.count)
        assertEquals("Transporte S/ 285.00 · Suministros S/ 120.00 · Otros S/ 64.00", month.breakdown)
        assertEquals(listOf("Transporte", "Suministros", "Otros"), month.categories)
    }

    @Test
    fun `rows read newest first and a category filters them`() {
        val month = OutlayMonth.from(OCTOBER, "Transporte", today)

        assertEquals(listOf(1, 3), month.rows.map { it.id })
        assertEquals(OutlayRow(1, "Combustible de la moto", "S/ 85.00", "Transporte · Técnico · Diego H. · 14 oct"), month.rows[0])
    }
}
