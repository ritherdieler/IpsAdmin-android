package com.dscorp.ispadmin.domain.model.financial

import java.math.BigDecimal

data class AllocationInput(val invoiceId: Int, val amount: BigDecimal)
data class InvoiceInput(val id: Int, val subscriptionId: Int?, val zone: String?, val customer: String?,
    val serviceMonth: String?, val periodSource: String, val issuedAt: String?, val dueDate: String?,
    val dueDateConfirmed: Boolean, val gross: BigDecimal, val adjustment: BigDecimal, val adjustmentReason: String?,
    val status: String, val settledAt: String?, val currency: String)
data class ReceiptInput(val id: Int, val amount: BigDecimal, val paidAt: String?, val method: String?,
    val collector: String?, val allocations: List<AllocationInput>)
data class ExpenseInput(val id: Int, val amount: BigDecimal, val date: String, val description: String,
    val category: String?, val costCenter: String?)
data class BudgetInput(val id: Int, val amount: BigDecimal, val description: String, val type: String)
data class CorporateInput(val id: Int, val name: String, val currency: String, val amount: BigDecimal)
data class FinancialDifference(
    val code: String,
    val recordType: String,
    val recordId: Int?,
    val amount: BigDecimal?,
    val description: String,
    val serviceMonths: List<String>? = null,
    val relatedReceiptIds: List<Int>? = null
)
data class BillingSummary(val gross: BigDecimal, val adjustments: BigDecimal, val net: BigDecimal?,
    val collected: BigDecimal?, val pending: BigDecimal?, val effectivenessPercent: BigDecimal?)
data class MovementSummary(
    val received: BigDecimal?,
    val expenses: BigDecimal?,
    val partialBalance: BigDecimal?,
    val previousServiceRecovery: BigDecimal?,
    val receivedWithDate: BigDecimal? = null
)
data class FinancialInvoice(val invoice: InvoiceInput, val net: BigDecimal?, val collected: BigDecimal?,
    val pending: BigDecimal?, val difference: BigDecimal?, val agingBucket: String?, val relatedReceiptIds: List<Int>? = null)
data class FinancialReceipt(val receipt: ReceiptInput, val serviceMonths: List<String>)
data class FinancialGroup(val key: String, val amount: BigDecimal, val count: Int)
data class ZoneDebt(val zone: String?, val pending: BigDecimal, val overdue: BigDecimal, val subscriptions: Int)
data class ServiceHistory(val serviceMonth: String, val billing: BillingSummary?)
data class FinancialCoverage(val inferredPeriods: Int, val missingPeriods: Int, val unconfirmedDueDates: Int,
    val receiptsWithoutDate: Int, val notes: List<String>)
data class FinancialReport(
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
)
