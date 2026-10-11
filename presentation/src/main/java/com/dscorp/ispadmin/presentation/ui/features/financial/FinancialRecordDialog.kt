package com.dscorp.ispadmin.presentation.ui.features.financial

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.domain.model.financial.*

@Composable
internal fun RecordsDialog(report: FinancialReport, detail: Detail, close: () -> Unit) {
    var includeOtherPeriods by remember(report.id, detail) { mutableStateOf(false) }
    val allDifferenceRows = remember(report.id) { differenceRows(report) }
    val selectedDifferenceRows = remember(report.id, report.serviceMonth) {
        allDifferenceRows.filter { report.serviceMonth in differenceServiceMonths(report, it.item) }
    }
    val rows = if (detail == Detail.DIFFERENCES) {
        if (includeOtherPeriods) allDifferenceRows else selectedDifferenceRows
    } else {
        remember(report.id, detail) { recordRows(report, detail) }
    }
    Dialog(onDismissRequest = close) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().heightIn(max = 650.dp).padding(16.dp)) {
                Text(stringResource(R.string.financial_records), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.financial_cut_id, report.id), style = MaterialTheme.typography.bodySmall)
                if (detail == Detail.DIFFERENCES) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !includeOtherPeriods, onClick = { includeOtherPeriods = false },
                            label = { Text(stringResource(R.string.financial_difference_filter_service, report.serviceMonth, selectedDifferenceRows.size)) },
                            modifier = Modifier.testTag("financial_difference_filter_service"))
                        FilterChip(selected = includeOtherPeriods, onClick = { includeOtherPeriods = true },
                            label = { Text(stringResource(R.string.financial_difference_filter_all, allDifferenceRows.size)) },
                            modifier = Modifier.testTag("financial_difference_filter_all"))
                    }
                }
                LazyColumn(Modifier.weight(1f, fill = false).testTag("financial_record_list"),
                    verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
                    if (rows.isEmpty()) item { Text(stringResource(R.string.financial_empty)) }
                    items(rows, key = { it.key }) { row -> Text(recordText(row, report), Modifier.fillMaxWidth().testTag("financial_record_${row.key}")); HorizontalDivider() }
                }
                TextButton(onClick = close, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.financial_close_details)) }
            }
        }
    }
}
private sealed interface Record {
    val key: String
    data class Receipt(val item: FinancialReceipt) : Record { override val key = "receipt_${item.receipt.id}" }
    data class Expense(val item: ExpenseInput) : Record { override val key = "expense_${item.id}" }
    data class Invoice(val item: FinancialInvoice) : Record { override val key = "invoice_${item.invoice.id}" }
    data class Difference(val item: FinancialDifference, val index: Int) : Record { override val key = "difference_$index" }
    data class Budget(val item: BudgetInput) : Record { override val key = "budget_${item.id}" }
    data class Corporate(val item: CorporateInput) : Record { override val key = "corporate_${item.id}" }
}
private fun recordRows(report: FinancialReport, detail: Detail): List<Record> = when (detail) {
    Detail.RECEIPTS -> report.receipts.map(Record::Receipt)
    Detail.EXPENSES -> report.expenses.orEmpty().map(Record::Expense)
    Detail.INVOICES, Detail.DEBT -> report.invoices.filter { if (detail == Detail.INVOICES) it.invoice.serviceMonth == report.serviceMonth else it.pending?.signum() == 1 }.map(Record::Invoice)
    Detail.DIFFERENCES -> differenceRows(report)
    Detail.BUDGET -> report.budget.orEmpty().map(Record::Budget)
    Detail.CORPORATE -> report.corporate.orEmpty().map(Record::Corporate)
}

private fun differenceRows(report: FinancialReport): List<Record.Difference> =
    report.differences.mapIndexed { index, item -> Record.Difference(item, index) }

private fun differenceServiceMonths(report: FinancialReport, difference: FinancialDifference): List<String> =
    difference.serviceMonths?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }
        ?: when (difference.recordType.lowercase()) {
            "invoice" -> report.invoices.find { it.invoice.id == difference.recordId }
                ?.invoice?.serviceMonth?.takeIf { it.isNotBlank() }?.let(::listOf).orEmpty()
            "receipt" -> report.receipts.find { it.receipt.id == difference.recordId }?.serviceMonths.orEmpty()
            else -> emptyList()
        }
@Composable
private fun recordText(record: Record, report: FinancialReport): String = when (record) {
    is Record.Receipt -> { val r = record.item.receipt
        stringResource(R.string.financial_receipt_detail, r.id, r.paidAt ?: stringResource(R.string.financial_unavailable),
            r.method ?: stringResource(R.string.financial_unavailable), r.collector ?: stringResource(R.string.financial_unavailable), money(r.amount)) +
            "\n" + stringResource(R.string.financial_service_month) + ": " + record.item.serviceMonths.joinToString(", ") +
            "\n" + r.allocations.joinToString("\n") { a -> "#${a.invoiceId} · ${a.amount.toPlainString()} PEN" }
    }
    is Record.Expense -> { val e = record.item
        stringResource(R.string.financial_expense_detail, e.id, e.date, e.description, money(e.amount)) + "\n${e.category.orEmpty()} · ${e.costCenter.orEmpty()}"
    }
    is Record.Invoice -> { val row = record.item; val i = row.invoice
        stringResource(R.string.financial_invoice_detail, i.id, i.subscriptionId?.toString() ?: stringResource(R.string.financial_unavailable),
            i.customer ?: stringResource(R.string.financial_unavailable), i.zone ?: stringResource(R.string.financial_unavailable), i.serviceMonth ?: stringResource(R.string.financial_unavailable),
            if (i.dueDateConfirmed) stringResource(R.string.financial_invoice_due, i.dueDate ?: stringResource(R.string.financial_unavailable)) else stringResource(R.string.financial_aging_unconfirmed),
            money(row.net), money(row.collected), money(row.pending)) + "\n" +
            stringResource(R.string.financial_invoice_amounts, money(i.gross), money(i.adjustment), i.adjustmentReason.orEmpty())
    }
    is Record.Difference -> {
        val d = record.item
        val invoice = report.invoices.find { it.invoice.id == d.recordId }
        val periods = differenceServiceMonths(report, d)
        val receiptIdsFromMovements = if (d.recordType.equals("invoice", ignoreCase = true)) {
            report.receipts.filter { receipt -> receipt.receipt.allocations.any { it.invoiceId == d.recordId } }.map { it.receipt.id }
        } else emptyList()
        val receiptIds = listOfNotNull(d.relatedReceiptIds, invoice?.relatedReceiptIds)
            .firstOrNull { it.isNotEmpty() } ?: receiptIdsFromMovements.distinct().sorted()
        val receiptLinksKnown = d.relatedReceiptIds != null || invoice?.relatedReceiptIds != null || receiptIdsFromMovements.isNotEmpty()
        val serviceValue = periods.takeIf { it.isNotEmpty() }?.joinToString(", ")
            ?: stringResource(R.string.financial_unavailable)
        val receiptValue = when {
            receiptIds.isNotEmpty() -> receiptIds.joinToString(", ") { "#$it" }
            receiptLinksKnown -> stringResource(R.string.financial_no_related_receipts)
            else -> stringResource(R.string.financial_unavailable)
        }
        val receiptLinkText = if (d.recordType.equals("receipt", ignoreCase = true)) "" else
            "\n" + stringResource(R.string.financial_difference_related_receipts, receiptValue)
        "${recordTypeLabel(d.recordType)} #${d.recordId ?: "—"}\n${d.description}\n${money(d.amount)}\n" +
            stringResource(R.string.financial_difference_service, serviceValue) + receiptLinkText
    }
    is Record.Budget -> { val b = record.item; "#${b.id} · ${b.description}\n${money(b.amount)}" }
    is Record.Corporate -> { val c = record.item; "#${c.id} · ${c.name}\n${c.currency} · ${money(c.amount, c.currency)}" }
}

@Composable
private fun recordTypeLabel(type: String) = stringResource(when (type) {
    "invoice" -> R.string.financial_record_invoice
    "receipt" -> R.string.financial_record_receipt
    "expense" -> R.string.financial_record_expense
    "budget" -> R.string.financial_budget
    "corporate" -> R.string.financial_corporate
    else -> R.string.financial_records
})
