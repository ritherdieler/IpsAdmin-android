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
import java.text.DateFormat
import java.util.Date

/** Commercial tab: subscriber flow, churn and ARPU from the legacy dashboard endpoint. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommercialContent(state: DashboardState, onRefresh: () -> Unit, header: @Composable () -> Unit) {
    val dashboard = remember(state.dashboardData) { state.dashboardData?.toCommercialDashboard() }
    Column(Modifier.fillMaxSize()) {
        CommercialHeroTop(state, onRefresh, header)
        PullToRefreshBox(isRefreshing = state.isLoading && dashboard != null, onRefresh = onRefresh, modifier = Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                Modifier.fillMaxSize().testTag("commercial_list"),
                contentPadding = PaddingValues(bottom = IspSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(IspSpacing.md),
            ) {
                item("hero") { CommercialHeroBody(dashboard) }
                if (dashboard != null) item("kpis") { CommercialKpis(dashboard) }
                item("status") { CommercialStatus(state, dashboard, onRefresh) }
                if (dashboard != null) commercialItems(dashboard)
            }
        }
    }
}

@Composable
private fun CommercialHeroTop(state: DashboardState, onRefresh: () -> Unit, header: @Composable () -> Unit) {
    HeroTop {
        header()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(state.lastUpdatedAtMillis?.let { stringResource(R.string.dashboard_last_updated, timeLabel(it)) }.orEmpty(),
                Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = IspTheme.colors.onHeroMuted)
            IconButton(onClick = onRefresh, enabled = !state.isLoading, modifier = Modifier.size(48.dp).testTag("commercial_refresh")) {
                Icon(Icons.Default.Refresh, stringResource(R.string.dashboard_refresh), tint = IspTheme.colors.onHero)
            }
        }
    }
}

@Composable
private fun CommercialHeroBody(dashboard: CommercialDashboard?) {
    HeroBand {
        dashboard?.let {
            val previous = it.active.months.getOrNull(it.active.months.size - 2)
            val delta = countDelta(it.active, Polarity.HigherIsBetter, previous?.let { month -> stringResource(R.string.dashboard_vs_previous, month.shortLabel()) })
            HeroFigure(HeroFigureModel(stringResource(R.string.commercial_active_subscriptions), formatCount(it.activeSubscriptions),
                "commercial_active", delta = delta))
        }
    }
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

private fun LazyListScope.commercialItems(dashboard: CommercialDashboard) {
    item("subscriptions_header") { SectionHeader(stringResource(R.string.commercial_subscriptions_section), Modifier.padding(horizontal = IspSpacing.lg), stringResource(R.string.dashboard_last_6_months)) }
    item("new_vs_cancelled") { Padded { NewVsCancelledCard(dashboard) } }
    item("active_history") { Padded { ActiveHistoryCard(dashboard.active) } }
    item("cancellation_reason") { Padded { CancellationReasonCard(dashboard) } }
    item("installations") { Padded { InstallationsCard(dashboard.installations) } }
    item("evolution_header") { SectionHeader(stringResource(R.string.commercial_monthly_evolution), Modifier.padding(horizontal = IspSpacing.lg), stringResource(R.string.dashboard_last_6_months)) }
    item("churn_chart") { Padded { ChurnCard(dashboard.churnPercent) } }
    item("payment_methods") { Padded { PaymentMethodsCard(dashboard.paymentMethods) } }
    item("cancellations_chart") { Padded { CountTrendCard(R.string.commercial_cancellations, dashboard.loggedCancellations, IspTheme.colors.chartCategorical[1], Polarity.LowerIsBetter) } }
    item("reconnections_chart") { Padded { CountTrendCard(R.string.commercial_reconnections, dashboard.reconnections, IspTheme.colors.chartCategorical[2], Polarity.HigherIsBetter) } }
    item("migrations_chart") { Padded { CountTrendCard(R.string.commercial_migrations, dashboard.migrations, IspTheme.colors.chartCategorical[0], Polarity.Neutral) } }
}

@Composable
private fun comparison(trend: MonthlyTrend): String? =
    trend.months.getOrNull(trend.months.size - 2)?.let { stringResource(R.string.dashboard_vs_previous, it.shortLabel()) }

@Composable
private fun CommercialKpis(dashboard: CommercialDashboard) {
    val unavailable = stringResource(R.string.dashboard_unavailable)
    val tiles = listOf(
        KpiModel(stringResource(R.string.commercial_new), dashboard.newSubscriptions.current?.let(::formatCount) ?: unavailable, "commercial_new",
            countDelta(dashboard.newSubscriptions, Polarity.HigherIsBetter, comparison(dashboard.newSubscriptions))),
        KpiModel(stringResource(R.string.commercial_cancellations), dashboard.cancellations.current?.let(::formatCount) ?: unavailable, "commercial_cancellations",
            countDelta(dashboard.cancellations, Polarity.LowerIsBetter, comparison(dashboard.cancellations))),
        KpiModel(stringResource(R.string.commercial_reconnections), dashboard.reconnections.current?.let(::formatCount) ?: unavailable, "commercial_reconnections",
            countDelta(dashboard.reconnections, Polarity.HigherIsBetter, comparison(dashboard.reconnections))),
        KpiModel(stringResource(R.string.commercial_migrations), dashboard.migrations.current?.let(::formatCount) ?: unavailable, "commercial_migrations",
            countDelta(dashboard.migrations, Polarity.Neutral, comparison(dashboard.migrations))),
        KpiModel(stringResource(R.string.commercial_churn), dashboard.churnPercent.current?.let(::formatPercent) ?: unavailable, "commercial_churn",
            pointsDelta(dashboard.churnPercent, Polarity.LowerIsBetter, comparison(dashboard.churnPercent)), info = stringResource(R.string.commercial_churn_info)),
        KpiModel(stringResource(R.string.commercial_arpu), dashboard.arpu.current?.let(::formatSoles) ?: unavailable, "commercial_arpu",
            solesDelta(dashboard.arpu, Polarity.HigherIsBetter, comparison(dashboard.arpu)), info = stringResource(R.string.commercial_arpu_info)),
    )
    KpiGrid(tiles, overlapHero = true)
}

/** Two-column KPI grid used by the dashboard tabs; the first grid of a tab rides over the hero band. */
@Composable
internal fun KpiGrid(tiles: List<KpiModel>, overlapHero: Boolean = false) {
    val overlap = if (overlapHero) Modifier.pullUp(HERO_OVERLAP + IspSpacing.md) else Modifier
    Column(overlap.padding(horizontal = IspSpacing.lg), verticalArrangement = Arrangement.spacedBy(IspSpacing.md)) {
        tiles.chunked(2).forEach { row ->
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

@Composable
private fun NewVsCancelledCard(dashboard: CommercialDashboard) {
    val colors = IspTheme.colors
    val series = listOf(
        ChartSeries(stringResource(R.string.commercial_new), colors.chartCategorical[0], dashboard.newSubscriptions.values),
        ChartSeries(stringResource(R.string.commercial_cancellations), colors.chartCategorical[1], dashboard.cancellations.values),
    )
    val data = ChartData(dashboard.newSubscriptions.months.map { it.shortLabel() }, series)
    ChartCard(ChartCardHeader(stringResource(R.string.commercial_new_vs_cancelled), legend = series.map { LegendItem(it.name, it.color) }),
        Modifier.testTag("commercial_new_vs_cancelled")) {
        if (data.isEmpty) EmptyChart(stringResource(R.string.dashboard_no_data)) else IspColumnChart(data)
    }
}

@Composable
private fun ActiveHistoryCard(active: MonthlyTrend) {
    val data = ChartData(active.months.map { it.shortLabel() },
        listOf(ChartSeries(stringResource(R.string.commercial_active_history), IspTheme.colors.chartPrimary, active.values)))
    ChartCard(ChartCardHeader(stringResource(R.string.commercial_active_history), stringResource(R.string.commercial_end_of_month)),
        Modifier.testTag("commercial_active_history")) {
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
    ChartCard(ChartCardHeader(title, formatCount(total)), Modifier.testTag(tag)) {
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
    ChartCard(ChartCardHeader(stringResource(R.string.commercial_churn_chart), stringResource(R.string.commercial_churn_chart_subtitle)),
        Modifier.testTag("commercial_churn_chart")) {
        if (data.isEmpty) EmptyChart(stringResource(R.string.dashboard_no_data)) else IspLineChart(data, valueFormat = ::formatDecimal)
    }
}

@Composable
private fun PaymentMethodsCard(methods: Map<String, Double>) {
    val items = methods.entries.sortedByDescending { it.value }.map { BarItem(it.key, formatPercent(it.value), it.value) }
    ChartCard(ChartCardHeader(stringResource(R.string.commercial_payment_methods), stringResource(R.string.commercial_payment_methods_subtitle)),
        Modifier.testTag("commercial_payment_methods")) {
        if (items.isEmpty()) EmptyChart(stringResource(R.string.dashboard_no_data)) else HorizontalBarList(items)
    }
}

@Composable
private fun CountTrendCard(title: Int, trend: MonthlyTrend, color: Color, polarity: Polarity) {
    val name = stringResource(title)
    val data = ChartData(trend.months.map { it.shortLabel() }, listOf(ChartSeries(name, color, trend.values)))
    ChartCard(ChartCardHeader(name, stringResource(R.string.commercial_total_months, trend.months.size, formatCount(trend.total))),
        Modifier.testTag("commercial_trend_$title")) {
        countDelta(trend, polarity, comparison(trend))?.let {
            com.dscorp.ispadmin.presentation.designsystem.components.DeltaText(it)
        }
        if (data.isEmpty) EmptyChart(stringResource(R.string.dashboard_no_data)) else IspColumnChart(data)
    }
}

private fun timeLabel(millis: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))

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
