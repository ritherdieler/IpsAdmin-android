package com.dscorp.ispadmin.presentation.ui.features.financial

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.domain.model.DownloadDocumentResponse
import com.dscorp.ispadmin.domain.model.financial.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

@Composable
fun FinancialScreen(onOperational: () -> Unit, viewModel: FinancialViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var document by remember { mutableStateOf<DownloadDocumentResponse?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        val file = document
        document = null
        if (uri != null && file != null) scope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(android.util.Base64.decode(file.base64, android.util.Base64.DEFAULT)) }
                        ?: error("Cannot open document")
                }.isSuccess
            }
            snackbar.showSnackbar(context.getString(if (saved) R.string.financial_export_saved else R.string.financial_export_failed))
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.onIntent(FinancialIntent.Load)
        viewModel.events.collect { event ->
            when (event) {
                is FinancialEvent.Document -> { document = event.document; launcher.launch(event.document.getNameWithExtension()) }
            }
        }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Box(Modifier.padding(padding)) { FinancialContent(state, viewModel::onIntent, onOperational) }
    }
}

internal enum class Detail { RECEIPTS, EXPENSES, DEBT, DIFFERENCES, INVOICES, BUDGET, CORPORATE }

@Composable
fun FinancialContent(uiState: FinancialUiState, onIntent: (FinancialIntent) -> Unit, onOperational: () -> Unit) {
    var detail by remember(uiState.report?.id) { mutableStateOf<Detail?>(null) }
    val listState = rememberLazyListState()
    val report = uiState.report
    val managementAccess = report?.access == "FULL"
    val billingView = uiState.billingView && managementAccess
    LaunchedEffect(billingView) { listState.scrollToItem(0) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 1000.dp).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.financial_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { onIntent(FinancialIntent.Load) }, enabled = !uiState.isLoading,
                    modifier = Modifier.size(48.dp).testTag("financial_refresh")) {
                    Icon(Icons.Default.Refresh, stringResource(R.string.dashboard_refresh))
                }
            }
            if (uiState.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (uiState.failed) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (report == null) R.string.financial_initial_failed else R.string.financial_refresh_failed),
                    Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { onIntent(FinancialIntent.Load) }, Modifier.testTag("financial_retry")) { Text(stringResource(R.string.retry)) }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                FilterChip(selected = !billingView, onClick = { onIntent(FinancialIntent.SelectView(false)) },
                    label = { Text(stringResource(R.string.financial_movements)) }, modifier = Modifier.weight(1f).testTag("financial_movements_tab"))
                if (managementAccess) {
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = billingView, onClick = { onIntent(FinancialIntent.SelectView(true)) },
                        label = { Text(stringResource(R.string.financial_billing)) }, modifier = Modifier.weight(1f).testTag("financial_billing_tab"))
                }
            }
            LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().weight(1f).testTag("financial_list"), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item("periods") {
                    if (billingView) {
                        PeriodPicker(R.string.financial_service_month, uiState.serviceMonth, "service") { onIntent(FinancialIntent.SelectServiceMonth(it)) }
                    } else {
                        PeriodPicker(R.string.financial_movements_month, uiState.movementsMonth, "movements") { onIntent(FinancialIntent.SelectMovementsMonth(it)) }
                        Text(stringResource(R.string.financial_service_month) + ": " + uiState.serviceMonth, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (report != null) {
                    val differenceScope = differenceScope(report, report.serviceMonth)
                    item("cut") {
                        Column(Modifier.testTag("financial_cut")) {
                            Text(stringResource(R.string.financial_cut, cutoffLabel(report.cutoff), report.revision), style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.financial_cut_id, report.id), style = MaterialTheme.typography.bodySmall)
                            Text(stringResource(if (report.status == "COMPLETE") R.string.financial_status_complete else R.string.financial_status_observed),
                                color = if (report.status == "COMPLETE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            if (report.kind == "RECONSTRUCTED") Text(stringResource(R.string.financial_reconstructed), style = MaterialTheme.typography.bodySmall)
                            if (report.kind == "MONTHLY" || report.kind == "REVISION") Text(stringResource(R.string.financial_monthly_cut))
                        }
                    }
                    if (!billingView) {
                        val datedReceived = receivedWithDate(report)
                        item("cash") {
                            val receiptsWithoutDate = report.coverage?.receiptsWithoutDate ?: 0
                            val warning = if (receiptsWithoutDate > 0) stringResource(R.string.financial_receipts_missing_date_warning, receiptsWithoutDate) else null
                            if (managementAccess) {
                                MetricPair(
                                    { Metric(R.string.financial_received_dated, money(datedReceived, report.currency), "financial_received",
                                        supportingText = warning) { detail = Detail.RECEIPTS } },
                                    { Metric(R.string.financial_expenses, money(report.movements?.expenses), "financial_expenses") { detail = Detail.EXPENSES } })
                            } else {
                                Metric(R.string.financial_received_dated, money(datedReceived, report.currency), "financial_received",
                                    supportingText = warning) { detail = Detail.RECEIPTS }
                            }
                        }
                        item("control") {
                            val summary = stringResource(R.string.financial_difference_summary,
                                differenceScope.selected, differenceScope.otherPeriods, differenceScope.unknownPeriods)
                            MetricPair(
                                { Metric(R.string.financial_overdue, money(report.overdueTotal), "financial_overdue") { detail = Detail.DEBT } },
                                { Metric(R.string.financial_differences_service, differenceScope.selected.toString(), "financial_differences",
                                    supportingText = summary) { detail = Detail.DIFFERENCES } })
                        }
                        item("balance") {
                            Metric(R.string.financial_partial_balance, money(report.movements?.partialBalance), "financial_balance") { detail = Detail.RECEIPTS }
                            Text(stringResource(if (report.access == "FULL") R.string.financial_cash_scope else R.string.financial_operational_scope), style = MaterialTheme.typography.bodySmall)
                        }
                        item("recovery") { Metric(R.string.financial_recovery, money(report.movements?.previousServiceRecovery), "financial_recovery") { detail = Detail.RECEIPTS } }
                    } else {
                        item("billing_main") { MetricPair(
                            { Metric(R.string.financial_gross, money(report.billing?.gross), "financial_gross") { detail = Detail.INVOICES } },
                            { Metric(R.string.financial_net, money(report.billing?.net), "financial_net") { detail = Detail.INVOICES } }) }
                        item("billing_adjustment") { MetricPair(
                            { Metric(R.string.financial_adjustments, money(report.billing?.adjustments), "financial_adjustments") { detail = Detail.INVOICES } },
                            { Metric(R.string.financial_collected, money(report.billing?.collected), "financial_collected") { detail = Detail.INVOICES } }) }
                        item("billing_pending") { MetricPair(
                            { Metric(R.string.financial_pending, money(report.billing?.pending), "financial_pending") { detail = Detail.INVOICES } },
                            { Metric(R.string.financial_effectiveness, report.billing?.effectivenessPercent?.let { stringResource(R.string.financial_percent, it.toPlainString()) }
                                ?: stringResource(R.string.financial_unavailable), "financial_effectiveness") { detail = Detail.INVOICES } }) }
                        item("billing_differences") {
                            Section(R.string.financial_difference_scope) {
                                Text(stringResource(R.string.financial_difference_summary,
                                    differenceScope.selected, differenceScope.otherPeriods, differenceScope.unknownPeriods),
                                    Modifier.testTag("financial_difference_summary"), style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { detail = Detail.DIFFERENCES }) { Text(stringResource(R.string.financial_differences)) }
                            }
                        }
                        item("history_chart") { FinancialHistoryChart(report.history, report.currency) }
                        item("budget") {
                            Metric(R.string.financial_budget, money(report.budgetTotal), "financial_budget") { detail = Detail.BUDGET }
                            Text(stringResource(R.string.financial_budget_scope, uiState.movementsMonth), style = MaterialTheme.typography.bodySmall)
                        }
                        item("history") {
                            Section(R.string.financial_history) {
                                report.history.forEach { period ->
                                    Text(period.serviceMonth, style = MaterialTheme.typography.titleSmall)
                                    Text(stringResource(R.string.financial_history_detail, money(period.billing?.net), money(period.billing?.collected), money(period.billing?.pending)), style = MaterialTheme.typography.bodySmall)
                                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                                }
                            }
                        }
                        item("adjustments") { Groups(R.string.financial_by_reason, report.adjustmentsByReason) }
                        item("aging") { Groups(R.string.financial_aging, report.aging, report, aging = true) }
                        item("zones") {
                            Section(R.string.financial_zones) {
                                if (report.zones.isEmpty()) Text(stringResource(R.string.financial_empty))
                                report.zones.forEach { zone ->
                                    val unlinkedInvoices = report.invoices.count {
                                        it.pending?.signum() == 1 && it.invoice.zone == zone.zone && it.invoice.subscriptionId == null
                                    }
                                    Text("${zone.zone ?: stringResource(R.string.financial_unavailable)} · ${money(zone.pending)}", style = MaterialTheme.typography.titleSmall)
                                    Text(stringResource(R.string.financial_zone_detail, formatCount(zone.subscriptions), formatCount(unlinkedInvoices), money(zone.overdue)),
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    if (!billingView) {
                        item("by_service") {
                            Groups(R.string.financial_by_service, report.receiptsByServiceMonth, titleArgument = uiState.movementsMonth)
                        }
                        item("by_method") { FinancialGroupsChart(R.string.financial_by_method, report.receiptsByMethod, "financial_method_chart") }
                        item("by_collector") { Groups(R.string.financial_by_collector, report.receiptsByCollector) }
                        if (report.access == "FULL") {
                            item("by_category") { FinancialGroupsChart(R.string.financial_by_category, report.expensesByCategory.orEmpty(), "financial_expense_category_chart") }
                            item("by_center") { Groups(R.string.financial_by_cost_center, report.expensesByCostCenter.orEmpty()) }
                            item("corporate") {
                                Section(R.string.financial_corporate) {
                                    Text(stringResource(R.string.financial_corporate_scope), style = MaterialTheme.typography.bodySmall)
                                    report.corporate.orEmpty().groupBy { it.currency }.forEach { (currency, entries) ->
                                        Text("$currency · ${money(entries.fold(BigDecimal.ZERO) { sum, item -> sum + item.amount }, currency)}")
                                    }
                                    TextButton(onClick = { detail = Detail.CORPORATE }) { Text(stringResource(R.string.financial_records)) }
                                }
                            }
                        }
                    }
                    item("coverage") {
                        Section(R.string.financial_coverage) {
                            report.coverage?.let { coverage ->
                                Text(stringResource(R.string.financial_coverage_counts, formatCount(coverage.inferredPeriods), formatCount(coverage.missingPeriods),
                                    formatCount(coverage.unconfirmedDueDates), formatCount(coverage.receiptsWithoutDate)))
                                coverage.notes.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                            }
                            TextButton(onClick = { detail = Detail.DIFFERENCES }) { Text(stringResource(R.string.financial_differences)) }
                        }
                    }
                    item("export") {
                        Button(onClick = { onIntent(FinancialIntent.Export) }, enabled = !uiState.exporting && !uiState.isLoading,
                            modifier = Modifier.fillMaxWidth().testTag("financial_export")) { Text(stringResource(R.string.financial_export)) }
                        if (uiState.exportFailed) Text(stringResource(R.string.financial_export_failed), color = MaterialTheme.colorScheme.error)
                    }
                }
                item("commercial") { TextButton(onClick = onOperational, modifier = Modifier.testTag("financial_commercial")) { Text(stringResource(R.string.financial_commercial)) } }
            }
        }
    }
    if (detail != null && report != null) RecordsDialog(report, detail!!) { detail = null }
}

@Composable
private fun FinancialHistoryChart(history: List<ServiceHistory>, currency: String) {
    val months = history.sortedBy { it.serviceMonth }.takeLast(12)
    val series = listOf(
        HistorySeries(stringResource(R.string.financial_chart_net), MaterialTheme.colorScheme.primary) { it?.net },
        HistorySeries(stringResource(R.string.financial_chart_collected), MaterialTheme.colorScheme.tertiary) { it?.collected },
        HistorySeries(stringResource(R.string.financial_chart_pending), MaterialTheme.colorScheme.error) { it?.pending }
    )
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val values = series.map { item -> months.map { month -> item.value(month.billing)?.toFloat() } }
    val maximum = values.flatten().filterNotNull().maxOrNull() ?: 0f
    val hasData = values.flatten().any { it != null }
    val chartPoints = months.map { month ->
        stringResource(R.string.financial_chart_point, month.serviceMonth,
            money(month.billing?.net, currency), money(month.billing?.collected, currency), money(month.billing?.pending, currency))
    }
    val chartDescription = buildString {
        append(stringResource(R.string.financial_chart_accessibility))
        if (chartPoints.isNotEmpty()) append(". ").append(chartPoints.joinToString(". "))
    }
    Section(R.string.financial_chart_title) {
        if (months.isEmpty() || !hasData) {
            Text(stringResource(R.string.financial_empty))
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(64.dp).height(150.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Text(axisMoney(maximum, currency), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    Text(axisMoney(maximum / 2, currency), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    Text(axisMoney(0f, currency), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
                Canvas(Modifier.weight(1f).height(150.dp).testTag("financial_history_chart")
                    .semantics { contentDescription = chartDescription }) {
                    val topInset = 8.dp.toPx()
                    val chartHeight = size.height - topInset * 2
                    val chartWidth = size.width
                    val scaleMaximum = maximum.coerceAtLeast(1f)
                    for (fraction in listOf(0f, 0.5f, 1f)) {
                        val y = topInset + chartHeight * fraction
                        drawLine(gridColor, Offset(0f, y), Offset(chartWidth, y), strokeWidth = 1.dp.toPx())
                    }
                    series.zip(values).forEach { (line, points) ->
                        val path = Path()
                        var segmentStarted = false
                        points.forEachIndexed { index, value ->
                            if (value == null) {
                                segmentStarted = false
                            } else {
                                val x = if (months.size == 1) chartWidth / 2 else chartWidth * index / (months.size - 1)
                                val y = topInset + chartHeight * (1f - value / scaleMaximum)
                                if (!segmentStarted) {
                                    path.moveTo(x, y)
                                    segmentStarted = true
                                } else path.lineTo(x, y)
                                drawCircle(line.color, radius = 3.dp.toPx(), center = Offset(x, y))
                            }
                        }
                        drawPath(path, line.color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 64.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                months.forEach { month ->
                    Text(serviceMonthLabel(month.serviceMonth), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                series.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Canvas(Modifier.size(8.dp)) { drawCircle(item.color) }
                        Text(item.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

private fun axisMoney(value: Float, currency: String): String {
    val prefix = when (currency) { "PEN" -> "S/"; "USD" -> "US$"; else -> currency }
    val thousands = value >= 1000
    val scaled = if (thousands) value / 1000 else value
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-PE")).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
    }
    return "$prefix ${formatter.format(scaled)}${if (thousands) "k" else ""}"
}

private fun receivedWithDate(report: FinancialReport): BigDecimal? {
    val movements = report.movements ?: return null
    movements.receivedWithDate?.let { return it }
    movements.received?.let { return it }
    val month = runCatching { YearMonth.parse(report.movementsMonth) }.getOrNull() ?: return null
    return report.receipts.fold(BigDecimal.ZERO) { total, item ->
        val paidAt = item.receipt.paidAt?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
        if (paidAt != null && YearMonth.from(paidAt) == month) total + item.receipt.amount else total
    }
}

private fun serviceMonthLabel(month: String): String = runCatching {
    YearMonth.parse(month).month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.forLanguageTag("es-PE")).trimEnd('.')
}.getOrDefault(month.takeLast(2))

private data class HistorySeries(
    val label: String,
    val color: androidx.compose.ui.graphics.Color,
    val value: (BillingSummary?) -> BigDecimal?
)

private data class DifferenceScope(val selected: Int, val otherPeriods: Int, val unknownPeriods: Int)

private fun differenceScope(report: FinancialReport, selectedMonth: String): DifferenceScope {
    var selected = 0
    var otherPeriods = 0
    var unknownPeriods = 0
    report.differences.forEach { difference ->
        val periods = difference.serviceMonths?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }
            ?: when (difference.recordType.lowercase()) {
                "invoice" -> report.invoices.find { it.invoice.id == difference.recordId }
                    ?.invoice?.serviceMonth?.takeIf { it.isNotBlank() }?.let(::listOf).orEmpty()
                "receipt" -> report.receipts.find { it.receipt.id == difference.recordId }?.serviceMonths.orEmpty()
                else -> emptyList()
            }
        when {
            selectedMonth in periods -> selected++
            periods.isNotEmpty() -> otherPeriods++
            else -> unknownPeriods++
        }
    }
    return DifferenceScope(selected, otherPeriods, unknownPeriods)
}

@Composable
private fun PeriodPicker(label: Int, month: String, tag: String, select: (String) -> Unit) {
    val parsed = runCatching { YearMonth.parse(month) }.getOrNull() ?: return
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { select(parsed.minusMonths(1).toString()) }, modifier = Modifier.size(48.dp).testTag("financial_previous_$tag")) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.financial_previous_month))
        }
        Text(stringResource(label) + ": " + month, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        IconButton(onClick = { select(parsed.plusMonths(1).toString()) }, enabled = parsed < YearMonth.now(ZoneId.of("America/Lima")),
            modifier = Modifier.size(48.dp).testTag("financial_next_$tag")) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.financial_next_month))
        }
    }
}
@Composable
private fun MetricPair(first: @Composable () -> Unit, second: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f)) { first() }
        Box(Modifier.weight(1f)) { second() }
    }
}
@Composable
private fun Metric(label: Int, value: String, tag: String, supportingText: String? = null, open: () -> Unit) {
    Card(onClick = open, modifier = Modifier.fillMaxWidth().heightIn(min = 108.dp).testTag(tag)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
            Text(value, Modifier.testTag("${tag}_value"), style = MaterialTheme.typography.headlineSmall)
            supportingText?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
@Composable
private fun Section(title: Int, titleArgument: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (titleArgument == null) stringResource(title) else stringResource(title, titleArgument),
                style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
@Composable
private fun Groups(title: Int, groups: List<FinancialGroup>, report: FinancialReport? = null, aging: Boolean = false,
    titleArgument: String? = null) {
    Section(title, titleArgument) {
        if (groups.isEmpty()) Text(stringResource(R.string.financial_empty))
        groups.forEach { group ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (aging) agingLabel(group.key) else group.key.takeUnless { it == "SIN_DATO" } ?: stringResource(R.string.financial_unavailable), Modifier.weight(1f))
                Text(money(group.amount))
            }
            if (aging) {
                val unlinkedInvoices = report?.invoices?.count {
                    it.agingBucket == group.key && it.invoice.subscriptionId == null
                } ?: 0
                Text(stringResource(R.string.financial_aging_count, formatCount(group.count), formatCount(unlinkedInvoices)),
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun FinancialGroupsChart(title: Int, groups: List<FinancialGroup>, tag: String) {
    val accent = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val maximum = groups.maxOfOrNull { it.amount }?.toFloat()?.coerceAtLeast(0f) ?: 0f
    Card(Modifier.fillMaxWidth().testTag(tag)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            if (groups.isEmpty()) Text(stringResource(R.string.financial_empty))
            groups.forEach { group ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(group.key.takeUnless { it == "SIN_DATO" } ?: stringResource(R.string.financial_unavailable),
                            Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(money(group.amount), style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(track)) {
                            val fraction = if (maximum == 0f) 0f else (group.amount.toFloat() / maximum).coerceIn(0f, 1f)
                            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(accent))
                        }
                        Text(stringResource(R.string.financial_group_count, formatCount(group.count)), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun agingLabel(key: String) = stringResource(when (key) {
    "CURRENT" -> R.string.financial_aging_current
    "1_30" -> R.string.financial_aging_1_30
    "31_60" -> R.string.financial_aging_31_60
    "61_90" -> R.string.financial_aging_61_90
    "OVER_90" -> R.string.financial_aging_over_90
    else -> R.string.financial_aging_unconfirmed
})
private fun formatCount(value: Int): String = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-PE")).format(value)
@Composable
internal fun money(value: BigDecimal?, currency: String = "PEN"): String = value?.let {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-PE"))
    formatter.currency = runCatching { Currency.getInstance(currency) }.getOrDefault(Currency.getInstance("PEN"))
    formatter.format(it)
} ?: stringResource(R.string.financial_unavailable)
private fun cutoffLabel(value: String): String = runCatching {
    OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.of("America/Lima")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
}.getOrDefault(value)

@Preview
@Composable
private fun FinancialContentPreview() {
    MaterialTheme { FinancialContent(FinancialUiState(serviceMonth="2026-09", movementsMonth="2026-10", report=FinancialReport(id="preview")), {}, {}) }
}
