package com.dscorp.ispadmin.data.datasource.remote.financial

import java.math.BigDecimal
import com.dscorp.ispadmin.domain.model.financial.*

data class FinancialReportDto(
    val id: String = "", val revision: Int = 1, val previousCutId: String? = null, val revisionReason: String? = null,
    val kind: String = "OBSERVATION", val serviceMonth: String = "", val movementsMonth: String = "",
    val cutoff: String = "", val observedAt: String = "", val zoneId: String = "America/Lima", val currency: String = "PEN",
    val access: String = "FULL", val status: String = "OBSERVED", val billing: BillingSummary? = null,
    val movements: MovementSummary? = null, val overdueTotal: BigDecimal? = null, val pendingTotal: BigDecimal? = null,
    val budgetTotal: BigDecimal? = null, val budget: List<BudgetInput>? = null, val invoices: List<FinancialInvoice> = emptyList(),
    val receipts: List<FinancialReceipt> = emptyList(), val expenses: List<ExpenseInput>? = null, val corporate: List<CorporateInput>? = null,
    val aging: List<FinancialGroup> = emptyList(), val zones: List<ZoneDebt> = emptyList(), val receiptsByMethod: List<FinancialGroup> = emptyList(),
    val receiptsByCollector: List<FinancialGroup> = emptyList(), val receiptsByServiceMonth: List<FinancialGroup> = emptyList(),
    val expensesByCategory: List<FinancialGroup>? = null, val expensesByCostCenter: List<FinancialGroup>? = null,
    val adjustmentsByReason: List<FinancialGroup> = emptyList(), val history: List<ServiceHistory> = emptyList(),
    val differences: List<FinancialDifference> = emptyList(), val coverage: FinancialCoverage? = null
) {
    fun toDomain() = FinancialReport(
        id = id, revision = revision, previousCutId = previousCutId, revisionReason = revisionReason, kind = kind, serviceMonth = serviceMonth, movementsMonth = movementsMonth, cutoff = cutoff, observedAt = observedAt, zoneId = zoneId, currency = currency, access = access, status = status, billing = billing, movements = movements, overdueTotal = overdueTotal, pendingTotal = pendingTotal, budgetTotal = budgetTotal, budget = budget, invoices = invoices, receipts = receipts, expenses = expenses, corporate = corporate, aging = aging, zones = zones, receiptsByMethod = receiptsByMethod, receiptsByCollector = receiptsByCollector, receiptsByServiceMonth = receiptsByServiceMonth, expensesByCategory = expensesByCategory, expensesByCostCenter = expensesByCostCenter, adjustmentsByReason = adjustmentsByReason, history = history, differences = differences, coverage = coverage
    )
}
