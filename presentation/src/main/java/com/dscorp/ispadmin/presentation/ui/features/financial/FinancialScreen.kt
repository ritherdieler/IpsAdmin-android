package com.dscorp.ispadmin.presentation.ui.features.financial

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.domain.model.DashBoardDataResponse
import com.dscorp.ispadmin.domain.model.DownloadDocumentResponse
import com.dscorp.ispadmin.domain.model.financial.*
import com.dscorp.ispadmin.domain.model.dashboard.CollectionSummary
import com.dscorp.ispadmin.presentation.ui.features.dashboard.CollectionsSection
import com.dscorp.ispadmin.presentation.ui.features.dashboard.collectedSoles
import com.dscorp.ispadmin.presentation.ui.features.dashboard.billedMonths
import com.dscorp.ispadmin.presentation.ui.features.dashboard.formatPercent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.dscorp.ispadmin.presentation.designsystem.components.SectionHeader
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.dscorp.ispadmin.presentation.ui.features.home.AdminDailyCard
import com.dscorp.ispadmin.presentation.ui.features.home.AdminHomeHero
import com.dscorp.ispadmin.presentation.ui.features.home.AdminHomeTiles
import com.dscorp.ispadmin.presentation.ui.features.dashboard.formatSoles
import com.dscorp.ispadmin.presentation.ui.features.dashboard.percentChange
import com.dscorp.ispadmin.presentation.ui.features.dashboard.shares
import com.dscorp.ispadmin.presentation.ui.features.dashboard.DashboardTitleBar
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.presentation.ui.features.dashboard.DashboardTabData
import com.dscorp.ispadmin.presentation.designsystem.charts.ChartData
import com.dscorp.ispadmin.presentation.designsystem.charts.ChartSeries
import com.dscorp.ispadmin.presentation.designsystem.charts.IspColumnChart
import com.dscorp.ispadmin.presentation.designsystem.charts.IspLineChart
import com.dscorp.ispadmin.presentation.designsystem.components.*
import com.dscorp.ispadmin.presentation.theme.IspSpacing
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.theme.IspType
import com.dscorp.ispadmin.presentation.theme.MyTheme
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

private const val HISTORY_MONTHS = 12
private const val TREND_MONTHS = 6

/**
 * Financial tab of the dashboard. [header] draws inside the hero band (dashboard tabs);
 * [data] carries what other endpoints add: legacy history charts and the collection follow-up.
 */
@Composable
fun FinancialScreen(
    header: @Composable () -> Unit = {},
    data: DashboardTabData = DashboardTabData(),
    viewModel: FinancialViewModel = koinViewModel(),
    home: com.dscorp.ispadmin.presentation.ui.features.home.AdminHome? = null,
    onHomeAction: (com.dscorp.ispadmin.presentation.ui.features.home.AdminHomeAction) -> Unit = {},
) {
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
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(Modifier.padding(padding)) { FinancialContent(state, viewModel::onIntent, header, data.legacy, data.collections, home, onHomeAction) }
    }
}

internal enum class Detail { RECEIPTS, EXPENSES, DEBT, DIFFERENCES, INVOICES, BUDGET, CORPORATE }

@Composable
fun FinancialContent(
    uiState: FinancialUiState,
    onIntent: (FinancialIntent) -> Unit,
    header: @Composable () -> Unit,
    legacy: DashBoardDataResponse? = null,
    collections: CollectionSummary? = null,
    home: com.dscorp.ispadmin.presentation.ui.features.home.AdminHome? = null,
    onHomeAction: (com.dscorp.ispadmin.presentation.ui.features.home.AdminHomeAction) -> Unit = {},
) {
    var detail by remember(uiState.report?.id) { mutableStateOf<Detail?>(null) }
    val listState = rememberLazyListState()
    val report = uiState.report
    val managementAccess = report?.access == "FULL"
    val billingView = uiState.billingView && managementAccess
    val openDetail: (Detail) -> Unit = { detail = it }
    // One effect: two concurrent scrollToItem calls before the first layout crash the lazy list.
    // The summary usually arrives after the report; without this the list stays anchored to the report band below it.
    // It arrives within a second of opening, so going back to the top then does not fight the user.
    LaunchedEffect(billingView, home != null) { listState.scrollToItem(0) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 1000.dp).fillMaxSize()) {
            FinancialHeroTop(uiState, onIntent, header, managementAccess, billingView, home?.subtitle)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f).testTag("financial_list"),
                contentPadding = PaddingValues(bottom = IspSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(IspSpacing.md),
            ) {
                val heroContext = HeroContext(report, managementAccess, billingView, uiState.previousReport, uiState.movementsMonth)
                if (home == null) {
                    item("hero") { FinancialHeroBody(uiState, onIntent, heroContext, openDetail) }
                } else {
                    // Home summary first (a-ini-01); the full report follows in its own band.
                    item("home_hero") { AdminHomeHero(home) }
                    item("home_tiles") { AdminHomeTiles(home, onHomeAction) }
                    item("home_daily") { AdminDailyCard(home, onHomeAction) }
                    item("report_title") {
                        SectionHeader(stringResource(R.string.financial_report_section), Modifier.padding(horizontal = IspSpacing.lg))
                    }
                    item("hero") {
                        FinancialHeroBody(
                            uiState, onIntent, heroContext, openDetail,
                            modifier = Modifier.padding(horizontal = IspSpacing.lg).clip(RoundedCornerShape(24.dp)),
                        ) {
                            report?.let { Text(stringResource(R.string.financial_cut, cutoffLabel(it.cutoff), it.revision), Modifier.testTag("financial_cut"),
                                style = MaterialTheme.typography.bodySmall, color = IspTheme.colors.onHeroMuted) }
                            HeroChips(viewChips(managementAccess, billingView, onIntent))
                        }
                    }
                }
                if (report != null) item("tiles") { if (billingView) BillingTiles(report, uiState, openDetail) else MovementTiles(report, openDetail) }
                item("status") { StatusArea(uiState, report) }
                if (report != null) {
                    if (billingView) billingItems(report, openDetail) else movementItems(report, uiState, openDetail, MovementExtras(legacy, collections))
                    closingItems(report, uiState, onIntent, openDetail)
                }
            }
        }
    }
    val openRecords = detail
    if (openRecords != null && report != null) RecordsDialog(report, openRecords) { detail = null }
}

private data class HeroContext(
    val report: FinancialReport?,
    val managementAccess: Boolean,
    val billingView: Boolean,
    val previousReport: FinancialReport? = null,
    val movementsMonth: String = "",
)

/** Fixed part: dashboard tabs, report view selector and refresh stay reachable while scrolling. */
@Composable
private fun FinancialHeroTop(
    uiState: FinancialUiState,
    onIntent: (FinancialIntent) -> Unit,
    header: @Composable () -> Unit,
    managementAccess: Boolean,
    billingView: Boolean,
    homeStatus: String? = null,
) {
    val report = uiState.report
    val status = homeStatus ?: report?.let { stringResource(R.string.financial_cut, cutoffLabel(it.cutoff), it.revision) }
    HeroTop {
        DashboardTitleBar(status, statusTag = if (homeStatus == null) "financial_cut" else "home_status") {
            if (report != null) {
                IconButton(onClick = { onIntent(FinancialIntent.Export) }, enabled = !uiState.exporting && !uiState.isLoading,
                    modifier = Modifier.size(44.dp).testTag("financial_export_header")) {
                    Icon(Icons.Default.FileDownload, stringResource(R.string.financial_export_short), tint = IspTheme.colors.onHero)
                }
            }
            IconButton(onClick = { onIntent(FinancialIntent.Load) }, enabled = !uiState.isLoading,
                modifier = Modifier.size(44.dp).testTag("financial_refresh")) {
                Icon(Icons.Default.Refresh, stringResource(R.string.dashboard_refresh), tint = IspTheme.colors.onHero)
            }
        }
        header()
        // With the home summary on top, the report view selector moves down to the report band.
        if (homeStatus == null) HeroChips(viewChips(managementAccess, billingView, onIntent))
    }
}

@Composable
private fun FinancialHeroBody(
    uiState: FinancialUiState,
    onIntent: (FinancialIntent) -> Unit,
    context: HeroContext,
    openDetail: (Detail) -> Unit,
    modifier: Modifier = Modifier,
    top: @Composable ColumnScope.() -> Unit = {},
) {
    HeroBand(modifier) {
        top()
        HeroError(uiState, context.report, onIntent)
        if (context.billingView) {
            PeriodPicker(R.string.financial_service_month, uiState.serviceMonth, "service") { onIntent(FinancialIntent.SelectServiceMonth(it)) }
        } else {
            PeriodPicker(R.string.financial_movements_month, uiState.movementsMonth, "movements") { onIntent(FinancialIntent.SelectMovementsMonth(it)) }
        }
        context.report?.let { report -> HeroMetric(report, context.billingView, openDetail, context.previousReport?.let { it to context.movementsMonth }) }
    }
}

@Composable
private fun viewChips(managementAccess: Boolean, billingView: Boolean, onIntent: (FinancialIntent) -> Unit): List<HeroChip> {
    val movements = HeroChip(stringResource(R.string.financial_movements), !billingView, "financial_movements_tab") {
        onIntent(FinancialIntent.SelectView(false))
    }
    if (!managementAccess) return listOf(movements)
    return listOf(movements, HeroChip(stringResource(R.string.financial_billing), billingView, "financial_billing_tab") {
        onIntent(FinancialIntent.SelectView(true))
    })
}

@Composable
private fun HeroMetric(
    report: FinancialReport,
    billingView: Boolean,
    openDetail: (Detail) -> Unit,
    comparison: Pair<FinancialReport, String>? = null,
) {
    val badge = HeroBadge(
        stringResource(if (report.status == "COMPLETE") R.string.financial_status_complete else R.string.financial_status_observed),
        if (report.status == "COMPLETE") DeltaTone.Positive else DeltaTone.Neutral,
    )
    if (billingView) {
        HeroFigure(HeroFigureModel(stringResource(R.string.financial_net), money(report.billing?.net), "financial_net", badge = badge)) {
            openDetail(Detail.INVOICES)
        }
    } else {
        val receiptsWithoutDate = report.coverage?.receiptsWithoutDate ?: 0
        val warning = if (receiptsWithoutDate > 0) stringResource(R.string.financial_receipts_missing_date_warning, receiptsWithoutDate) else null
        HeroFigure(
            HeroFigureModel(stringResource(R.string.financial_received_dated), money(receivedWithDate(report), report.currency),
                "financial_received", delta = comparison?.let { (previous, month) -> receivedDelta(report, previous, month) }, badge = badge, supporting = warning),
        ) { openDetail(Detail.RECEIPTS) }
    }
}

/** Loading feedback below the hero; refresh errors and the cut identity live inside the hero. */
@Composable
private fun StatusArea(uiState: FinancialUiState, report: FinancialReport?) {
    if (!uiState.isLoading) return
    Column(Modifier.padding(horizontal = IspSpacing.lg)) {
        if (report == null) DashboardSkeleton(Modifier.padding(top = IspSpacing.sm)) else LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@Composable
private fun HeroError(uiState: FinancialUiState, report: FinancialReport?, onIntent: (FinancialIntent) -> Unit) {
    if (!uiState.failed) return
    val colors = IspTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(if (report == null) R.string.financial_initial_failed else R.string.financial_refresh_failed),
            Modifier.weight(1f), color = colors.heroNegative, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { onIntent(FinancialIntent.Load) }, Modifier.testTag("financial_retry")) {
            Text(stringResource(R.string.retry), color = colors.onHero)
        }
    }
}


@Composable
private fun MovementTiles(report: FinancialReport, openDetail: (Detail) -> Unit) {
    val managementAccess = report.access == "FULL"
    val scope = differenceScope(report, report.serviceMonth)
    val summary = stringResource(R.string.financial_difference_summary, scope.selected, scope.otherPeriods, scope.unknownPeriods)
    TileGrid(buildList {
        if (managementAccess) add(TileSpec(KpiModel(stringResource(R.string.financial_expenses), money(report.movements?.expenses), "financial_expenses"), Detail.EXPENSES))
        add(TileSpec(KpiModel(stringResource(R.string.financial_partial_balance), money(report.movements?.partialBalance), "financial_balance"), Detail.RECEIPTS))
        add(TileSpec(KpiModel(stringResource(R.string.financial_overdue), money(report.overdueTotal), "financial_overdue"), Detail.DEBT))
        add(TileSpec(KpiModel(stringResource(R.string.financial_differences_service), scope.selected.toString(), "financial_differences", supporting = summary), Detail.DIFFERENCES))
    }, openDetail)
}

/** Data from other endpoints shown in the movements view. */
private data class MovementExtras(val legacy: DashBoardDataResponse?, val collections: CollectionSummary?)

private fun LazyListScope.movementItems(
    report: FinancialReport,
    uiState: FinancialUiState,
    openDetail: (Detail) -> Unit,
    extras: MovementExtras,
) {
    val legacy = extras.legacy
    val managementAccess = report.access == "FULL"
    extras.collections?.let { summary -> item("collections") { CollectionsSection(summary, onSeeDebtors = { openDetail(Detail.DEBT) }) } }
    val trend = report.history.sortedBy { it.serviceMonth }.takeLast(TREND_MONTHS).filter { it.billing?.net != null || it.billing?.collected != null }
    if (trend.isNotEmpty()) {
        item("trend_header") {
            SectionHeader(stringResource(R.string.financial_trend), Modifier.padding(horizontal = IspSpacing.lg), stringResource(R.string.financial_trend_caption, trend.size))
        }
        item("trend") { Padded { NetVsCollectedCard(trend, report.currency) } }
    }
    if (legacy != null) {
        item("history_header") { SectionHeader(stringResource(R.string.dashboard_history), Modifier.padding(horizontal = IspSpacing.lg), stringResource(R.string.dashboard_history_caption)) }
        item("gross_revenue") { Padded { GrossRevenueCard(legacy) } }
        item("monthly_collects") { Padded { MonthlyCollectsCard(legacy) } }
    }
    item("breakdown_header") { SectionHeader(stringResource(R.string.dashboard_breakdown), Modifier.padding(horizontal = IspSpacing.lg), uiState.movementsMonth) }
    item("by_method") { Padded { MethodsCard(report.receiptsByMethod) } }
    item("by_service") {
        Padded {
            GroupsCard(stringResource(R.string.financial_by_service, uiState.movementsMonth), report.receiptsByServiceMonth,
                subtitle = stringResource(R.string.financial_recovery_caption, money(report.movements?.previousServiceRecovery)))
        }
    }
    item("by_collector") { Padded { GroupsCard(stringResource(R.string.financial_by_collector), report.receiptsByCollector) } }
    if (managementAccess) {
        item("by_category") {
            Padded { GroupsCard(stringResource(R.string.financial_by_category), report.expensesByCategory.orEmpty(), Modifier.testTag("financial_expense_category_chart")) }
        }
        item("by_center") { Padded { GroupsCard(stringResource(R.string.financial_by_cost_center), report.expensesByCostCenter.orEmpty()) } }
        item("corporate") { Padded { CorporateCard(report) { openDetail(Detail.CORPORATE) } } }
    }
    item("scope") {
        Text(stringResource(if (managementAccess) R.string.financial_cash_scope else R.string.financial_operational_scope),
            Modifier.padding(horizontal = IspSpacing.lg), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BillingTiles(report: FinancialReport, uiState: FinancialUiState, openDetail: (Detail) -> Unit) {
        val effectiveness = report.billing?.effectivenessPercent
        TileGrid(listOf(
            TileSpec(KpiModel(stringResource(R.string.financial_gross), money(report.billing?.gross), "financial_gross"), Detail.INVOICES),
            TileSpec(KpiModel(stringResource(R.string.financial_adjustments), money(report.billing?.adjustments), "financial_adjustments"), Detail.INVOICES),
            TileSpec(KpiModel(stringResource(R.string.financial_collected), money(report.billing?.collected), "financial_collected"), Detail.INVOICES),
            TileSpec(KpiModel(stringResource(R.string.financial_pending), money(report.billing?.pending), "financial_pending"), Detail.INVOICES),
            TileSpec(KpiModel(stringResource(R.string.financial_effectiveness),
                effectiveness?.let { stringResource(R.string.financial_percent, it.toPlainString()) } ?: stringResource(R.string.financial_unavailable),
                "financial_effectiveness", progress = effectiveness?.toFloat()?.div(100f)), Detail.INVOICES),
            TileSpec(KpiModel(stringResource(R.string.financial_budget), money(report.budgetTotal), "financial_budget",
                supporting = stringResource(R.string.financial_budget_scope, uiState.movementsMonth)), Detail.BUDGET),
        ), openDetail)
}

private fun LazyListScope.billingItems(report: FinancialReport, openDetail: (Detail) -> Unit) {
    item("billing_differences") {
        val scope = differenceScope(report, report.serviceMonth)
        Padded {
            ChartCard(ChartCardHeader(stringResource(R.string.financial_difference_scope),
                action = CardAction(stringResource(R.string.financial_differences)) { openDetail(Detail.DIFFERENCES) })) {
                Text(stringResource(R.string.financial_difference_summary, scope.selected, scope.otherPeriods, scope.unknownPeriods),
                    Modifier.testTag("financial_difference_summary"), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    item("history_chart") { Padded { FinancialHistoryChart(report.history, report.currency) } }
    item("history") { Padded { HistoryListCard(report.history) } }
    item("adjustments") { Padded { GroupsCard(stringResource(R.string.financial_by_reason), report.adjustmentsByReason) } }
    item("aging") { Padded { AgingCard(report) } }
    item("zones") { Padded { ZonesCard(report) } }
}

private fun LazyListScope.closingItems(report: FinancialReport, uiState: FinancialUiState, onIntent: (FinancialIntent) -> Unit, openDetail: (Detail) -> Unit) {
    item("coverage") {
        Padded {
            ChartCard(ChartCardHeader(stringResource(R.string.financial_coverage),
                action = CardAction(stringResource(R.string.financial_differences)) { openDetail(Detail.DIFFERENCES) })) {
                report.coverage?.let { coverage ->
                    Text(stringResource(R.string.financial_coverage_counts, formatCount(coverage.inferredPeriods), formatCount(coverage.missingPeriods),
                        formatCount(coverage.unconfirmedDueDates), formatCount(coverage.receiptsWithoutDate)), style = MaterialTheme.typography.bodyMedium)
                    coverage.notes.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                cutKindNote(report.kind)?.let {
                    Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    item("export") {
        Padded {
            OutlinedButton(onClick = { onIntent(FinancialIntent.Export) }, enabled = !uiState.exporting && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().testTag("financial_export")) { Text(stringResource(R.string.financial_export)) }
            if (uiState.exportFailed) Text(stringResource(R.string.financial_export_failed), color = MaterialTheme.colorScheme.error)
        }
    }
}

private data class TileSpec(val model: KpiModel, val detail: Detail)

/** Two-column KPI grid riding over the hero band; an odd last tile spans the full width. */
@Composable
private fun TileGrid(tiles: List<TileSpec>, openDetail: (Detail) -> Unit) {
    Column(Modifier.pullUp(HERO_OVERLAP + IspSpacing.md).padding(horizontal = IspSpacing.lg), verticalArrangement = Arrangement.spacedBy(IspSpacing.md)) {
        tiles.chunked(2).forEach { row ->
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(IspSpacing.md)) {
                row.forEach { tile -> KpiTile(tile.model, Modifier.weight(1f).fillMaxHeight()) { openDetail(tile.detail) } }
            }
        }
    }
}

@Composable
private fun Padded(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = IspSpacing.lg), verticalArrangement = Arrangement.spacedBy(IspSpacing.sm), content = content)
}

@Composable
private fun FinancialHistoryChart(history: List<ServiceHistory>, currency: String) {
    val months = history.sortedBy { it.serviceMonth }.takeLast(HISTORY_MONTHS)
    val colors = IspTheme.colors
    val net = stringResource(R.string.financial_chart_net)
    val collected = stringResource(R.string.financial_chart_collected)
    val pending = stringResource(R.string.financial_chart_pending)
    val hasData = months.any { it.billing?.net != null || it.billing?.collected != null || it.billing?.pending != null }
    val chartPoints = months.map { month ->
        stringResource(R.string.financial_chart_point, month.serviceMonth,
            money(month.billing?.net, currency), money(month.billing?.collected, currency), money(month.billing?.pending, currency))
    }
    val chartDescription = buildString {
        append(stringResource(R.string.financial_chart_accessibility))
        if (chartPoints.isNotEmpty()) append(". ").append(chartPoints.joinToString(". "))
    }
    val series = listOf(
        ChartSeries(net, colors.chartPrimary, months.map { it.billing?.net.orZero() }),
        ChartSeries(collected, colors.chartCollected, months.map { it.billing?.collected.orZero() }),
        ChartSeries(pending, colors.chartCategorical[1], months.map { it.billing?.pending.orZero() }),
    )
    ChartCard(ChartCardHeader(stringResource(R.string.financial_chart_title), currencySymbol(currency),
        series.map { LegendItem(it.name, it.color) })) {
        if (months.isEmpty() || !hasData) {
            EmptyChart(stringResource(R.string.financial_empty))
        } else {
            IspLineChart(ChartData(months.map { serviceMonthLabel(it.serviceMonth) }, series),
                Modifier.testTag("financial_history_chart").semantics { contentDescription = chartDescription }, markerFormat = ::formatSoles)
        }
    }
}

/** Net billed vs collected for the last service months, side by side. */
@Composable
private fun NetVsCollectedCard(months: List<ServiceHistory>, currency: String) {
    val colors = IspTheme.colors
    val series = listOf(
        ChartSeries(stringResource(R.string.financial_chart_net), colors.chartPrimary, months.map { it.billing?.net.orZero() }),
        ChartSeries(stringResource(R.string.financial_chart_collected), colors.chartCollected, months.map { it.billing?.collected.orZero() }),
    )
    ChartCard(ChartCardHeader(stringResource(R.string.financial_trend_chart), currencySymbol(currency), series.map { LegendItem(it.name, it.color) }),
        Modifier.testTag("financial_trend_chart")) {
        IspColumnChart(ChartData(months.map { serviceMonthLabel(it.serviceMonth) }, series), markerFormat = ::formatSoles)
    }
}

/** Unbilled trailing months (billing runs mid-month) are left out instead of plotted as a drop to zero. */
@Composable
private fun GrossRevenueCard(legacy: DashBoardDataResponse) {
    val months = billedMonths(legacy.grossRevenueHistoryStatics, YearMonth.now(ZoneId.of("America/Lima"))).takeLast(HISTORY_MONTHS)
    val labels = months.map { serviceMonthLabel(it.billingMonth.take(7)) }
    val values = months.map { it.totalCharged }
    val data = ChartData(labels, listOf(ChartSeries(stringResource(R.string.gross_revenue_per_month), IspTheme.colors.chartCategorical[0], values)))
    ChartCard(ChartCardHeader(stringResource(R.string.gross_revenue_per_month), withAverage(lastMonthSummary(labels, values), values)),
        Modifier.testTag("financial_gross_revenue_chart")) {
        if (data.isEmpty) EmptyChart(stringResource(R.string.financial_empty)) else IspLineChart(data, startAtZero = true, markerFormat = ::formatSoles, showAverage = true)
    }
}

/** "sep: S/ 49,000.00 · ▲ 2.1% vs ago"; the currency alone when there is no data. */
@Composable
private fun lastMonthSummary(labels: List<String>, values: List<Double>): String {
    val last = values.lastOrNull() ?: return currencySymbol("PEN")
    val summary = stringResource(R.string.dashboard_month_value, labels.last(), formatSoles(last))
    val previous = values.getOrNull(values.size - 2) ?: return summary
    val change = percentChange(previous, last) ?: return summary
    val arrow = if (change > 0) "▲" else if (change < 0) "▼" else "="
    return "$summary · $arrow ${formatPercent(kotlin.math.abs(change))} " + stringResource(R.string.dashboard_vs_previous, labels[labels.size - 2])
}

@Composable
private fun MonthlyCollectsCard(legacy: DashBoardDataResponse) {
    val months = legacy.monthlyCollects.sortedBy { it.date }.takeLast(HISTORY_MONTHS)
    val zone = ZoneId.of("America/Lima")
    val labels = months.map { serviceMonthLabel(YearMonth.from(it.date.toInstant().atZone(zone)).toString()) }
    val values = months.map { collectedSoles(it.grossIncome, it.totalRaised) }
    val data = ChartData(labels, listOf(ChartSeries(stringResource(R.string.monthly_collects_history), IspTheme.colors.chartCollected, values)))
    ChartCard(ChartCardHeader(stringResource(R.string.monthly_collects_history), withAverage(lastMonthSummary(labels, values), values)),
        Modifier.testTag("financial_collects_chart")) {
        if (data.isEmpty) EmptyChart(stringResource(R.string.financial_empty)) else IspColumnChart(data, markerFormat = ::formatSoles, showAverage = true)
    }
}

@Composable
private fun MethodsCard(groups: List<FinancialGroup>) {
    val palette = IspTheme.colors.chartCategorical
    val other = IspTheme.colors.chartOther
    val sorted = groups.sortedByDescending { it.amount }
    val colored = sorted.mapIndexed { index, group -> group to (palette.getOrNull(index) ?: other) }
    ChartCard(ChartCardHeader(stringResource(R.string.financial_by_method),
        stringResource(R.string.financial_group_count, formatCount(groups.sumOf { it.count }))), Modifier.testTag("financial_method_chart")) {
        if (groups.isEmpty()) {
            EmptyChart(stringResource(R.string.financial_empty))
        } else {
            val percents = shares(colored.map { (group, _) -> group.amount.toDouble() })
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(IspSpacing.lg)) {
                DonutChart(colored.map { (group, color) -> Segment(groupLabel(group.key), group.amount.toDouble(), color) },
                    stringResource(R.string.financial_group_count, formatCount(groups.sumOf { it.count })),
                    money(groups.fold(BigDecimal.ZERO) { total, group -> total + group.amount }))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(IspSpacing.sm)) {
                    colored.forEachIndexed { index, (group, color) -> LegendRow(groupLabel(group.key), formatPercent(percents[index]), color) }
                }
            }
        }
    }
}

@Composable
private fun LegendRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(IspSpacing.sm)) {
        Swatch(color)
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = IspType.number)
    }
}

@Composable
private fun GroupsCard(title: String, groups: List<FinancialGroup>, modifier: Modifier = Modifier, subtitle: String? = null) {
    ChartCard(ChartCardHeader(title, subtitle), modifier) {
        if (groups.isEmpty()) {
            EmptyChart(stringResource(R.string.financial_empty))
        } else {
            HorizontalBarList(groups.map { BarItem(groupLabel(it.key), money(it.amount), it.amount.toDouble()) })
        }
    }
}

@Composable
private fun AgingCard(report: FinancialReport) {
    ChartCard(ChartCardHeader(stringResource(R.string.financial_aging), money(report.overdueTotal))) {
        if (report.aging.isEmpty()) EmptyChart(stringResource(R.string.financial_empty))
        val maximum = report.aging.maxOfOrNull { it.amount }?.toDouble()?.takeIf { it > 0 } ?: 1.0
        report.aging.forEach { group ->
            val severe = group.key == "OVER_90"
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(IspSpacing.sm)) {
                Text(agingLabel(group.key), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                    color = if (severe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                Text(money(group.amount), style = IspType.number)
            }
            ProgressTrack((group.amount.toDouble() / maximum).toFloat(),
                if (severe) MaterialTheme.colorScheme.error else IspTheme.colors.chartPrimary, height = 8.dp)
            val unlinkedInvoices = report.invoices.count { it.agingBucket == group.key && it.invoice.subscriptionId == null }
            Text(stringResource(R.string.financial_aging_count, formatCount(group.count), formatCount(unlinkedInvoices)),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ZonesCard(report: FinancialReport) {
    ChartCard(ChartCardHeader(stringResource(R.string.financial_zones))) {
        if (report.zones.isEmpty()) EmptyChart(stringResource(R.string.financial_empty))
        report.zones.forEach { zone ->
            val unlinkedInvoices = report.invoices.count {
                it.pending?.signum() == 1 && it.invoice.zone == zone.zone && it.invoice.subscriptionId == null
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(zone.zone ?: stringResource(R.string.financial_unavailable), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    Text(money(zone.pending), style = IspType.number)
                }
                Text(stringResource(R.string.financial_zone_detail, formatCount(zone.subscriptions), formatCount(unlinkedInvoices), money(zone.overdue)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun HistoryListCard(history: List<ServiceHistory>) {
    ChartCard(ChartCardHeader(stringResource(R.string.financial_history))) {
        history.forEach { period ->
            Text(period.serviceMonth, style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.financial_history_detail, money(period.billing?.net), money(period.billing?.collected), money(period.billing?.pending)),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun CorporateCard(report: FinancialReport, openRecords: () -> Unit) {
    ChartCard(ChartCardHeader(stringResource(R.string.financial_corporate), stringResource(R.string.financial_corporate_scope),
        action = CardAction(stringResource(R.string.financial_records), openRecords))) {
        report.corporate.orEmpty().groupBy { it.currency }.forEach { (currency, entries) ->
            Text("$currency · ${money(entries.fold(BigDecimal.ZERO) { sum, item -> sum + item.amount }, currency)}", style = IspType.number)
        }
    }
}

/** "▲ 6.1% vs sep"; while the month is in progress it compares up to the same day ("vs sep al día 11"). */
@Composable
private fun receivedDelta(report: FinancialReport, previous: FinancialReport, movementsMonth: String): KpiDelta? {
    val selected = runCatching { YearMonth.parse(movementsMonth) }.getOrNull() ?: return null
    val today = java.time.LocalDate.now(ZoneId.of("America/Lima"))
    val inProgress = selected == YearMonth.from(today)
    val change = receivedChange(report, previous, if (inProgress) today.dayOfMonth else null) ?: return null
    val month = serviceMonthLabel(selected.minusMonths(1).toString())
    val comparison = if (inProgress) stringResource(R.string.financial_vs_same_day, month, today.dayOfMonth)
        else stringResource(R.string.dashboard_vs_previous, month)
    val arrow = if (change > 0) "▲" else if (change < 0) "▼" else "="
    val tone = if (change > 0) DeltaTone.Positive else if (change < 0) DeltaTone.Negative else DeltaTone.Neutral
    return KpiDelta("$arrow ${formatPercent(kotlin.math.abs(change))}", tone, comparison)
}

@Composable
private fun groupLabel(key: String) = key.takeUnless { it == "SIN_DATO" } ?: stringResource(R.string.financial_unavailable)

private fun BigDecimal?.orZero(): Double = this?.toDouble() ?: 0.0

/** "sep: S/ 49,000.00 · ▲ 2.1% vs ago · prom. S/ 49.2k": the dashed line of the chart is the average. */
private fun withAverage(summary: String, values: List<Double>): String =
    listOfNotNull(summary, com.dscorp.ispadmin.presentation.designsystem.charts.averageCaption(values) {
        "S/ " + com.dscorp.ispadmin.presentation.designsystem.charts.compactNumber(it)
    }).joinToString(" · ")

private fun currencySymbol(currency: String) = when (currency) { "PEN" -> "S/"; "USD" -> "US$"; else -> currency }


internal fun serviceMonthLabel(month: String): String = runCatching {
    YearMonth.parse(month).month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.forLanguageTag("es-PE")).trimEnd('.')
}.getOrDefault(month.takeLast(2))

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

/** "MES DE MOVIMIENTOS        ‹ oct 2026 ›"; screen readers hear "Mes de movimientos: 2026-10". */
@Composable
private fun PeriodPicker(label: Int, month: String, tag: String, select: (String) -> Unit) {
    val parsed = runCatching { YearMonth.parse(month) }.getOrNull() ?: return
    val colors = IspTheme.colors
    val canGoForward = parsed < YearMonth.now(ZoneId.of("America/Lima"))
    val name = stringResource(label)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Overline(name, colors.onHeroMuted, Modifier.weight(1f), maxLines = 1, spokenText = "$name: $month")
        IconButton(onClick = { select(parsed.minusMonths(1).toString()) }, modifier = Modifier.size(36.dp).testTag("financial_previous_$tag")) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.financial_previous_month), Modifier.size(20.dp), tint = colors.onHero)
        }
        Text("${serviceMonthLabel(month)} ${parsed.year}", Modifier.clearAndSetSemantics {}, style = IspType.label.copy(fontSize = 14.sp),
            color = colors.onHero)
        IconButton(onClick = { select(parsed.plusMonths(1).toString()) }, enabled = canGoForward,
            modifier = Modifier.size(36.dp).testTag("financial_next_$tag")) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.financial_next_month), Modifier.size(20.dp),
                tint = if (canGoForward) colors.onHero else colors.onHeroMuted)
        }
    }
}

private fun cutKindNote(kind: String): Int? = when (kind) {
    "RECONSTRUCTED" -> R.string.financial_reconstructed
    "MONTHLY", "REVISION" -> R.string.financial_monthly_cut
    else -> null
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

@Preview(heightDp = 900)
@Composable
private fun FinancialContentPreview() {
    MyTheme(darkTheme = false) {
        FinancialContent(FinancialUiState(serviceMonth = "2026-09", movementsMonth = "2026-10",
            report = FinancialReport(id = "preview", status = "COMPLETE",
                movements = MovementSummary("41870.40".toBigDecimal(), "18240.00".toBigDecimal(), "23630.40".toBigDecimal(), "0".toBigDecimal()),
                receiptsByMethod = listOf(FinancialGroup("Yape", "15900".toBigDecimal(), 160), FinancialGroup("Efectivo", "11300".toBigDecimal(), 110)))),
            {}, {})
    }
}

@Preview(heightDp = 900)
@Composable
private fun FinancialContentDarkPreview() {
    MyTheme(darkTheme = true) { FinancialContent(FinancialUiState(serviceMonth = "2026-09", movementsMonth = "2026-10", isLoading = true), {}, {}) }
}
