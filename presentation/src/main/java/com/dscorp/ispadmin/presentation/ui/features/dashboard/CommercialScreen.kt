package com.dscorp.ispadmin.presentation.ui.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.domain.model.CancellationResumeDto
import com.dscorp.ispadmin.domain.model.DashBoardDataResponse
import com.dscorp.ispadmin.domain.model.EconomicResume
import com.dscorp.ispadmin.domain.model.MonthlySubscriptionResume
import com.dscorp.ispadmin.domain.model.SubscriptionsResumeStatics
import com.dscorp.ispadmin.presentation.designsystem.charts.ChartData
import com.dscorp.ispadmin.presentation.designsystem.charts.ChartSeries
import com.dscorp.ispadmin.presentation.designsystem.charts.IspColumnChart
import com.dscorp.ispadmin.presentation.designsystem.charts.IspLineChart
import com.dscorp.ispadmin.presentation.designsystem.charts.averageCaption
import com.dscorp.ispadmin.presentation.designsystem.components.BarItem
import com.dscorp.ispadmin.presentation.designsystem.components.ChartCard
import com.dscorp.ispadmin.presentation.designsystem.components.ChartCardHeader
import com.dscorp.ispadmin.presentation.designsystem.components.DashboardSkeleton
import com.dscorp.ispadmin.presentation.designsystem.components.EmptyChart
import com.dscorp.ispadmin.presentation.designsystem.components.HERO_OVERLAP
import com.dscorp.ispadmin.presentation.designsystem.components.HeroBand
import com.dscorp.ispadmin.presentation.designsystem.components.HeroTop
import com.dscorp.ispadmin.presentation.designsystem.components.pullUp
import com.dscorp.ispadmin.presentation.designsystem.components.HeroFigure
import com.dscorp.ispadmin.presentation.designsystem.components.HeroFigureModel
import com.dscorp.ispadmin.presentation.designsystem.components.HorizontalBarList
import com.dscorp.ispadmin.presentation.designsystem.components.KpiModel
import com.dscorp.ispadmin.presentation.designsystem.components.HeroChip
import com.dscorp.ispadmin.presentation.designsystem.components.HeroChips
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import java.time.YearMonth
import com.dscorp.ispadmin.presentation.designsystem.components.CardAction
import com.dscorp.ispadmin.presentation.designsystem.components.KpiDelta
import com.dscorp.ispadmin.presentation.designsystem.components.DeltaTone
import com.dscorp.ispadmin.presentation.designsystem.components.KpiTile
import com.dscorp.ispadmin.presentation.designsystem.components.LegendItem
import com.dscorp.ispadmin.presentation.designsystem.components.ProportionBar
import com.dscorp.ispadmin.presentation.designsystem.components.SectionHeader
import com.dscorp.ispadmin.presentation.designsystem.components.Segment
import com.dscorp.ispadmin.presentation.designsystem.components.Swatch
import com.dscorp.ispadmin.presentation.theme.IspSpacing
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.theme.IspType
import com.dscorp.ispadmin.presentation.theme.MyTheme
import java.util.Date

/** Commercial tab: subscriber flow, churn and ARPU from the legacy dashboard endpoint. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommercialContent(state: DashboardState, onRefresh: () -> Unit, header: @Composable () -> Unit) {
    val dashboard = remember(state.dashboardData) { state.dashboardData?.toCommercialDashboard() }
    var period by rememberSaveable { mutableStateOf(CommercialPeriod.MONTH) }
    val charts = remember(dashboard, period) { dashboard?.forCharts(period) }
    Column(Modifier.fillMaxSize()) {
        CommercialHeroTop(state, onRefresh, header, period) { period = it }
        PullToRefreshBox(isRefreshing = state.isLoading && dashboard != null, onRefresh = onRefresh, modifier = Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                Modifier.fillMaxSize().testTag("commercial_list"),
                contentPadding = PaddingValues(bottom = IspSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(IspSpacing.md),
            ) {
                item("hero") { CommercialHeroBody(dashboard) }
                if (dashboard != null) item("kpis") { CommercialKpis(dashboard, period) }
                item("status") { CommercialStatus(state, dashboard, onRefresh) }
                if (charts != null) commercialItems(charts, period)
            }
        }
    }
}

@Composable
private fun CommercialHeroTop(
    state: DashboardState,
    onRefresh: () -> Unit,
    header: @Composable () -> Unit,
    period: CommercialPeriod,
    onPeriod: (CommercialPeriod) -> Unit,
) {
    HeroTop {
        DashboardTitleBar(updatedLabel(state.lastUpdatedAtMillis)) {
            IconButton(onClick = onRefresh, enabled = !state.isLoading, modifier = Modifier.size(44.dp).testTag("commercial_refresh")) {
                Icon(Icons.Default.Refresh, stringResource(R.string.dashboard_refresh), tint = IspTheme.colors.onHero)
            }
        }
        header()
        HeroChips(CommercialPeriod.entries.map { option ->
            HeroChip(stringResource(option.label), option == period, "commercial_period_${option.name}") { onPeriod(option) }
        })
    }
}

private val CommercialPeriod.label: Int
    get() = when (this) {
        CommercialPeriod.MONTH -> R.string.commercial_period_month
        CommercialPeriod.SIX_MONTHS -> R.string.commercial_period_six
        CommercialPeriod.TWELVE_MONTHS -> R.string.commercial_period_twelve
    }

/** Trends cut to the months the charts of [period] show. */
private fun CommercialDashboard.forCharts(period: CommercialPeriod): CommercialDashboard {
    val months = period.chartMonths
    return copy(
        nonCancelled = nonCancelled.takeLastMonths(months), active = active.takeLastMonths(months),
        newSubscriptions = newSubscriptions.takeLastMonths(months), cancellations = cancellations.takeLastMonths(months),
        reconnections = reconnections.takeLastMonths(months), migrations = migrations.takeLastMonths(months),
        churnPercent = churnPercent.takeLastMonths(months), arpu = arpu.takeLastMonths(months),
        digitalPaymentsPercent = digitalPaymentsPercent.takeLastMonths(months),
    )
}

@Composable
private fun CommercialHeroBody(dashboard: CommercialDashboard?) {
    HeroBand {
        dashboard?.let {
            HeroFigure(HeroFigureModel(stringResource(R.string.commercial_active_subscriptions), formatCount(it.activeSubscriptions),
                "commercial_active", delta = netAddsDelta(it)))
        }
    }
}

/** "▲ 13 netos en oct": new + reconnected − cancelled this month. */
@Composable
private fun netAddsDelta(dashboard: CommercialDashboard): KpiDelta? {
    val net = netAdds(dashboard.newSubscriptions, dashboard.reconnections, dashboard.cancellations) ?: return null
    val month = dashboard.newSubscriptions.months.lastOrNull() ?: return null
    val arrow = when {
        net > 0 -> "▲"
        net < 0 -> "▼"
        else -> "="
    }
    val tone = when {
        net > 0 -> DeltaTone.Positive
        net < 0 -> DeltaTone.Negative
        else -> DeltaTone.Neutral
    }
    return KpiDelta(stringResource(R.string.commercial_net_adds, "$arrow ${formatCount(kotlin.math.abs(net))}"), tone,
        stringResource(R.string.commercial_in_month, month.shortLabel()))
}

@Composable
private fun CommercialStatus(state: DashboardState, dashboard: CommercialDashboard?, onRefresh: () -> Unit) {
    Column(Modifier.padding(horizontal = IspSpacing.lg)) {
        if (state.isLoading && dashboard == null) DashboardSkeleton()
        if (state.event is DashboardEvent.ShowError) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (dashboard == null) R.string.commercial_initial_failed else R.string.dashboard_refresh_failed),
                    Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRefresh, modifier = Modifier.testTag("commercial_retry")) { Text(stringResource(R.string.retry)) }
            }
        }
    }
}

private fun LazyListScope.commercialItems(dashboard: CommercialDashboard, period: CommercialPeriod) {
    item("subscriptions_header") {
        val openSubscriptions = LocalDashboardActions.current.openSubscriptions
        SectionHeader(stringResource(R.string.commercial_subscriptions_section), Modifier.padding(horizontal = IspSpacing.lg),
            stringResource(R.string.commercial_subscriptions_caption, period.chartMonths),
            openSubscriptions?.let { CardAction(stringResource(R.string.dashboard_see_all), it) })
    }
    item("new_vs_cancelled") { Padded { NewVsCancelledCard(dashboard) } }
    item("active_history") { Padded { ActiveHistoryCard(dashboard) } }
    item("cancellation_reason") { Padded { CancellationReasonCard(dashboard) } }
    item("installations") { Padded { InstallationsCard(dashboard.installations) } }
    item("evolution_header") {
        SectionHeader(stringResource(R.string.commercial_monthly_evolution), Modifier.padding(horizontal = IspSpacing.lg),
            stringResource(R.string.dashboard_last_n_months, period.chartMonths))
    }
    item("churn_chart") { Padded { ChurnCard(dashboard.churnPercent) } }
    item("digital_payments") { Padded { DigitalPaymentsCard(dashboard.digitalPaymentsPercent) } }
    item("cancellations_chart") { Padded { CountTrendCard(R.string.commercial_cancellations, dashboard.cancellations, IspTheme.colors.chartCategorical[1], Polarity.LowerIsBetter) } }
    item("reconnections_chart") { Padded { CountTrendCard(R.string.commercial_reconnections, dashboard.reconnections, IspTheme.colors.chartCategorical[2], Polarity.HigherIsBetter) } }
    item("migrations_chart") { Padded { CountTrendCard(R.string.commercial_migrations, dashboard.migrations, IspTheme.colors.chartCategorical[0], Polarity.Neutral) } }
}

@Composable
private fun comparison(trend: MonthlyTrend): String? =
    trend.months.getOrNull(trend.months.size - 2)?.let { stringResource(R.string.dashboard_vs_previous, it.shortLabel()) }

/** Counts add up the period; churn and ARPU average it. The change compares with the same number of months before. */
@Composable
private fun CommercialKpis(dashboard: CommercialDashboard, period: CommercialPeriod) {
    val unavailable = stringResource(R.string.dashboard_unavailable)
    @Composable
    fun counted(trend: MonthlyTrend, polarity: Polarity): Pair<String, KpiDelta?> {
        val current = periodTotal(trend, period)
        val previous = previousPeriodTotal(trend, period)
        return (current?.let(::formatCount) ?: unavailable) to
            if (current != null && previous != null) countDelta(pair(previous, current), polarity, periodComparison(trend, period)) else null
    }
    val (newValue, newDelta) = counted(dashboard.newSubscriptions, Polarity.HigherIsBetter)
    val (cancelValue, cancelDelta) = counted(dashboard.cancellations, Polarity.LowerIsBetter)
    val (reconnectValue, reconnectDelta) = counted(dashboard.reconnections, Polarity.HigherIsBetter)
    val (migrateValue, migrateDelta) = counted(dashboard.migrations, Polarity.Neutral)
    val churn = periodAverage(dashboard.churnPercent, period)
    val churnBefore = previousPeriodAverage(dashboard.churnPercent, period)
    val arpu = periodAverage(dashboard.arpu, period)
    val arpuBefore = previousPeriodAverage(dashboard.arpu, period)
    val tiles = listOf(
        KpiModel(stringResource(R.string.commercial_new), newValue, "commercial_new", newDelta),
        KpiModel(stringResource(R.string.commercial_cancellations), cancelValue, "commercial_cancellations", cancelDelta),
        KpiModel(stringResource(R.string.commercial_reconnections), reconnectValue, "commercial_reconnections", reconnectDelta),
        KpiModel(stringResource(R.string.commercial_migrations), migrateValue, "commercial_migrations", migrateDelta,
            badge = dashboard.planUpgrades?.takeIf { it > 0 }?.let { stringResource(R.string.commercial_plan_upgrades, formatCount(it)) }),
        KpiModel(stringResource(R.string.commercial_churn), churn?.let(::formatPercent) ?: unavailable, "commercial_churn",
            if (churn != null && churnBefore != null) pointsDelta(pair(churnBefore, churn), Polarity.LowerIsBetter, periodComparison(dashboard.churnPercent, period)) else null,
            info = stringResource(R.string.commercial_churn_info)),
        KpiModel(stringResource(R.string.commercial_arpu), arpu?.let(::formatSoles) ?: unavailable, "commercial_arpu",
            if (arpu != null && arpuBefore != null) solesDelta(pair(arpuBefore, arpu), Polarity.HigherIsBetter, periodComparison(dashboard.arpu, period)) else null,
            info = stringResource(R.string.commercial_arpu_info),
            supporting = staleMonth(dashboard.arpu, currentMonth())?.takeIf { period == CommercialPeriod.MONTH }
                ?.let { stringResource(R.string.commercial_month_of, it.shortLabel()) }),
    )
    KpiGrid(tiles, overlapHero = true)
}

/** Two-point trend so the period figures reuse the month delta formatting. */
private fun pair(previous: Double, current: Double): MonthlyTrend {
    val now = YearMonth.now()
    return MonthlyTrend(listOf(now.minusMonths(1), now), listOf(previous, current))
}

/** "vs sep" for a month, "vs 6 meses anteriores" for longer periods. */
@Composable
private fun periodComparison(trend: MonthlyTrend, period: CommercialPeriod): String? =
    if (period == CommercialPeriod.MONTH) comparison(trend) else stringResource(R.string.commercial_vs_previous_months, period.months)

/** Two-column KPI grid used by the dashboard tabs; the first grid of a tab rides over the hero band. */
@Composable
internal fun KpiGrid(tiles: List<KpiModel>, overlapHero: Boolean = false) {
    val overlap = if (overlapHero) Modifier.pullUp(HERO_OVERLAP + IspSpacing.md) else Modifier
    Column(overlap.padding(horizontal = IspSpacing.lg), verticalArrangement = Arrangement.spacedBy(IspSpacing.md)) {
        tiles.chunked(2).forEach { row ->
            // Tiles of a row share its height, as in the design grid; an open explanation grows both.
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(IspSpacing.md)) {
                row.forEach { KpiTile(it, Modifier.weight(1f).fillMaxHeight()) }
            }
        }
    }
}

@Composable
internal fun Padded(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = IspSpacing.lg), verticalArrangement = Arrangement.spacedBy(IspSpacing.sm), content = content)
}

/** Altas above zero and bajas below, one column per month. */
@Composable
private fun NewVsCancelledCard(dashboard: CommercialDashboard) {
    val colors = IspTheme.colors
    val series = listOf(
        ChartSeries(stringResource(R.string.commercial_new), colors.chartCategorical[0], dashboard.newSubscriptions.values),
        ChartSeries(stringResource(R.string.commercial_bajas), colors.chartCategorical[1], dashboard.cancellations.values.map { -it }),
    )
    val data = ChartData(dashboard.newSubscriptions.months.map { it.shortLabel() }, series)
    ChartCard(ChartCardHeader(stringResource(R.string.commercial_new_vs_cancelled), legend = series.map { LegendItem(it.name, it.color) }),
        Modifier.testTag("commercial_new_vs_cancelled")) {
        if (data.isEmpty) EmptyChart(stringResource(R.string.dashboard_no_data)) else IspColumnChart(data, integers = true, diverging = true)
    }
}

/** ACTIVE subscribers at month end once the Core has recorded two or more months; until then, non-cancelled ones. */
@Composable
private fun ActiveHistoryCard(dashboard: CommercialDashboard) {
    val activeOnly = dashboard.active.months.size >= MIN_LINE_POINTS
    val active = if (activeOnly) dashboard.active else dashboard.nonCancelled
    val title = stringResource(if (activeOnly) R.string.commercial_active_end_of_month else R.string.commercial_active_history)
    val subtitle = stringResource(if (activeOnly) R.string.commercial_active_end_of_month_subtitle else R.string.commercial_active_history_subtitle)
    val data = ChartData(active.months.map { it.shortLabel() }, listOf(ChartSeries(title, IspTheme.colors.chartPrimary, active.values)))
    ChartCard(ChartCardHeader(title, subtitle), Modifier.testTag("commercial_active_history")) {
        if (data.isEmpty) EmptyChart(stringResource(R.string.dashboard_no_data)) else IspLineChart(data)
    }
}

@Composable
private fun CancellationReasonCard(dashboard: CommercialDashboard) {
    val colors = IspTheme.colors
    val segments = listOf(
        Segment(stringResource(R.string.commercial_cancelled_by_user), dashboard.cancelledByUser.toDouble(), colors.chartCategorical[0]),
        Segment(stringResource(R.string.commercial_cancelled_by_system), dashboard.cancelledBySystem.toDouble(), colors.chartCategorical[1]),
    )
    ProportionCard(stringResource(R.string.commercial_cancellation_reason), segments, "commercial_cancellation_reason")
}

@Composable
private fun InstallationsCard(installations: SubscriptionsResumeStatics) {
    val palette = IspTheme.colors.chartCategorical
    val segments = listOf(
        Segment(stringResource(R.string.commercial_fiber), installations.fiberInternetInstallations.toDouble(), palette[0]),
        Segment(stringResource(R.string.commercial_wireless), installations.wirelessInternetInstallations.toDouble(), palette[1]),
        Segment(stringResource(R.string.commercial_cable_tv), installations.cableTvInstallations.toDouble(), palette[2]),
    )
    ProportionCard(stringResource(R.string.commercial_installations_by_type), segments, "commercial_installations")
}

@Composable
internal fun ProportionCard(title: String, segments: List<Segment>, tag: String) {
    val total = segments.sumOf { it.value }
    ChartCard(ChartCardHeader(title, formatCount(total).takeIf { total > 0 }), Modifier.testTag(tag)) {
        if (total <= 0) {
            EmptyChart(stringResource(R.string.dashboard_no_data))
        } else {
            ProportionBar(segments)
            segments.forEach { segment -> SegmentRow(segment.label, formatCount(segment.value), segment.color) }
        }
    }
}

@Composable
internal fun SegmentRow(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(IspSpacing.sm)) {
        Swatch(color)
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = IspType.number)
    }
}

@Composable
private fun ChurnCard(churn: MonthlyTrend) {
    val data = ChartData(churn.months.map { it.shortLabel() },
        listOf(ChartSeries(stringResource(R.string.commercial_churn_chart), IspTheme.colors.chartCategorical[1], churn.values)))
    val subtitle = churn.average?.let { stringResource(R.string.commercial_churn_average, formatPercent(it)) }
        ?: stringResource(R.string.commercial_churn_chart_subtitle)
    ChartCard(ChartCardHeader(stringResource(R.string.commercial_churn_chart), subtitle),
        Modifier.testTag("commercial_churn_chart")) {
        if (data.isEmpty) EmptyChart(stringResource(R.string.dashboard_no_data)) else IspLineChart(data, valueFormat = ::formatDecimal, startAtZero = true, markerFormat = ::formatPercent, showAverage = true)
    }
}

@Composable
private fun DigitalPaymentsCard(trend: MonthlyTrend) {
    val name = stringResource(R.string.commercial_digital_payments)
    val data = ChartData(trend.months.map { it.shortLabel() }, listOf(ChartSeries(name, IspTheme.colors.chartPrimary, trend.values)))
    val subtitle = trend.current?.let { current ->
        stringResource(R.string.commercial_digital_payments_current, trend.months.last().shortLabel(), formatPercent(current))
    }?.let { listOfNotNull(it, averageCaption(trend.values, ::formatPercent)).joinToString(" · ") }
        ?: stringResource(R.string.commercial_digital_payments_subtitle)
    ChartCard(ChartCardHeader(name, subtitle), Modifier.testTag("commercial_digital_payments")) {
        if (data.isEmpty) EmptyChart(stringResource(R.string.dashboard_no_data)) else IspColumnChart(data, valueFormat = { "${it.toInt()}%" }, markerFormat = ::formatPercent, showAverage = true)
    }
}

@Composable
private fun CountTrendCard(title: Int, trend: MonthlyTrend, color: Color, polarity: Polarity) {
    val name = stringResource(title)
    val data = ChartData(trend.months.map { it.shortLabel() }, listOf(ChartSeries(name, color, trend.values)))
    val subtitle = listOfNotNull(stringResource(R.string.commercial_total_months, trend.months.size, formatCount(trend.total)),
        averageCaption(trend.values, ::formatDecimal)).joinToString(" · ")
    ChartCard(ChartCardHeader(name, subtitle),
        Modifier.testTag("commercial_trend_$title")) {
        trend.current?.let { current ->
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(IspSpacing.sm)) {
                Text(formatCount(current), Modifier.testTag("commercial_trend_${title}_value"), style = IspType.kpi)
                countDelta(trend, polarity, comparison(trend))?.let {
                    com.dscorp.ispadmin.presentation.designsystem.components.DeltaText(it, Modifier.padding(bottom = 4.dp))
                }
            }
        }
        if (data.isEmpty) EmptyChart(stringResource(R.string.dashboard_no_data)) else IspColumnChart(data, integers = true, showAverage = true)
    }
}

private const val MIN_LINE_POINTS = 2

private fun currentMonth(): java.time.YearMonth = java.time.YearMonth.now(java.time.ZoneId.of("America/Lima"))


@Preview(heightDp = 1400)
@Composable
private fun CommercialContentPreview() {
    val now = System.currentTimeMillis()
    val month = 30L * 24 * 60 * 60 * 1000
    val history = (5 downTo 0).map { MonthlySubscriptionResume(1284 - it * 20, 35 + it, 15 + it % 3, Date(now - it * month)) }
    val data = DashBoardDataResponse(
        economicResume = EconomicResume(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        activeSubscriptions = 1284,
        subscriptionsResume = SubscriptionsResumeStatics(68, 1062, 154),
        cancellationsResume = CancellationResumeDto(11, 6),
        paymentResume = mapOf("Yape" to 38.0, "Efectivo" to 27.0),
        subscriptionsHistoryStatics = history,
        monthlyCollects = emptyList(),
        grossRevenueHistoryStatics = emptyList(),
        reconnections = 9,
    )
    MyTheme(darkTheme = false) { CommercialContent(DashboardState(dashboardData = data, lastUpdatedAtMillis = now), {}, {}) }
}
