package com.dscorp.ispadmin.presentation.ui.features.outlay

import com.dscorp.ispadmin.data.model.OutlayDTO
import com.dscorp.ispadmin.domain.model.OutlayType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

internal fun outlay(
    id: Int, amount: Double, description: String, category: String?, center: String?, who: String?, day: Int, type: String? = null,
    document: String? = null,
) = OutlayDTO(
    id = id, amount = amount, description = description, category = category, cost_center = center, responsibleName = who, type = type,
    document_code = document,
    date = Date.from(LocalDate.of(2026, 10, day).atTime(10, 0).atZone(ZoneId.of("America/Lima")).toInstant()),
)

internal val OCTOBER = listOf(
    outlay(1, 85.0, "Combustible de la moto", "Transporte", "Técnico", "Diego Huerta", 14),
    outlay(2, 120.0, "Conectores SC/APC x50", "Suministros", "Técnico", "Marco Salinas", 12),
    outlay(3, 200.0, "Flete de bobina de fibra", "Transporte", "Técnico", "Sergio Carrillo", 2),
    outlay(4, 64.0, "Agua y limpieza", "Otros", "Administración", "Rosa Ccori", 6),
)

class OutlayMonthTest {
    private val today = LocalDate.of(2026, 10, 14)
    private val mixed = OCTOBER + outlay(5, 1800.0, "Alquiler del local", "Alquiler", null, "Rosa Ccori", 5, type = "FIXED", document = "F001-0087")

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
    fun `rows read newest first, name their type and category, and leave the cost centre out`() {
        val month = OutlayMonth.from(OCTOBER, "Transporte", today)

        assertEquals(listOf(1, 3), month.rows.map { it.id })
        assertEquals(
            OutlayRow(1, "Combustible de la moto", "S/ 85.00", "Variable · Transporte · Diego H. · 14 oct", isFixed = false, category = "Transporte"),
            month.rows[0],
        )
    }

    @Test
    fun `a fixed outlay is marked and shows its type in the detail`() {
        val row = OutlayMonth.from(mixed, null, today).rows.first { it.id == 5 }

        assertEquals(true, row.isFixed)
        assertEquals("Fijo · Alquiler · Rosa C. · 5 oct", row.detail)
    }

    @Test
    fun `the type filter keeps only that type and a missing type counts as variable`() {
        assertEquals(listOf(5), OutlayMonth.from(mixed, null, today, OutlayType.FIXED).rows.map { it.id })
        assertEquals(listOf(1, 2, 4, 3), OutlayMonth.from(mixed, null, today, OutlayType.VARIABLE).rows.map { it.id })
    }

    @Test
    fun `the month counts its variable and fixed outlays whatever the filter shows`() {
        val month = OutlayMonth.from(mixed, null, today, OutlayType.FIXED)

        assertEquals(4, month.variableCount)
        assertEquals(1, month.fixedCount)
    }

    @Test
    fun `the previous month is named for the fixed comparison`() {
        assertEquals("set 2026", OutlayMonth.from(OCTOBER, null, today).previousTitle)
        assertEquals("dic 2025", OutlayMonth.from(OCTOBER, null, LocalDate.of(2026, 1, 3)).previousTitle)
    }

    @Test
    fun `the detail screen reads the outlay with its date, registration time and document`() {
        val detail = OutlayDetail.from(mixed.first { it.id == 5 })

        assertEquals("Alquiler del local", detail.description)
        assertEquals("S/ 1,800.00", detail.amount)
        assertEquals(true, detail.isFixed)
        assertEquals("Alquiler", detail.category)
        assertEquals("5 oct 2026", detail.date)
        assertEquals("Registrado el 5 oct 2026, 10:00", detail.registeredAt)
        assertEquals("F001-0087", detail.documentCode)
        assertEquals("Rosa Ccori", detail.responsible)
    }
}
