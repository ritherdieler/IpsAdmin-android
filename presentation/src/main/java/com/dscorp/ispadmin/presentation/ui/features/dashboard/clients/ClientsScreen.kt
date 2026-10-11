package com.dscorp.ispadmin.presentation.ui.features.dashboard.clients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.domain.model.dashboard.ClientLifecycle
import com.dscorp.ispadmin.domain.model.dashboard.ClientQuality
import com.dscorp.ispadmin.domain.model.dashboard.ClientsDashboard
import com.dscorp.ispadmin.domain.model.dashboard.PlanAnalysis
import com.dscorp.ispadmin.domain.model.dashboard.PlanStats
import com.dscorp.ispadmin.domain.model.dashboard.TicketSummary
import com.dscorp.ispadmin.domain.model.dashboard.ZonePerformance
import com.dscorp.ispadmin.presentation.designsystem.components.BadgeTone
import com.dscorp.ispadmin.presentation.designsystem.components.BarItem
import com.dscorp.ispadmin.presentation.designsystem.components.ChartCard
import com.dscorp.ispadmin.presentation.designsystem.components.ChartCardHeader
import com.dscorp.ispadmin.presentation.designsystem.components.DashboardSkeleton
import com.dscorp.ispadmin.presentation.designsystem.components.EmptyChart
import com.dscorp.ispadmin.presentation.designsystem.components.HeroBand
import com.dscorp.ispadmin.presentation.designsystem.components.HeroTop
import com.dscorp.ispadmin.presentation.designsystem.components.HeroFigure
import com.dscorp.ispadmin.presentation.designsystem.components.HeroFigureModel
import com.dscorp.ispadmin.presentation.designsystem.components.HeroBadge
import com.dscorp.ispadmin.presentation.designsystem.components.DeltaTone
import com.dscorp.ispadmin.presentation.designsystem.components.HorizontalBarList
import com.dscorp.ispadmin.presentation.designsystem.components.KpiModel
import com.dscorp.ispadmin.presentation.designsystem.components.ProportionBar
import com.dscorp.ispadmin.presentation.designsystem.components.SectionHeader
import com.dscorp.ispadmin.presentation.designsystem.components.Segment
import com.dscorp.ispadmin.presentation.designsystem.components.StatusBadge
import com.dscorp.ispadmin.presentation.designsystem.components.Swatch
import com.dscorp.ispadmin.presentation.theme.IspSpacing
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.theme.IspType
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.features.dashboard.KpiGrid
import com.dscorp.ispadmin.presentation.ui.features.dashboard.Padded
import com.dscorp.ispadmin.presentation.ui.features.dashboard.SegmentRow
import com.dscorp.ispadmin.presentation.ui.features.dashboard.formatCount
import com.dscorp.ispadmin.presentation.ui.features.dashboard.formatDecimal
import com.dscorp.ispadmin.presentation.ui.features.dashboard.formatPercent
import com.dscorp.ispadmin.presentation.ui.features.dashboard.formatSoles
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

private const val PLAN_SLOTS = 4

@Composable
fun ClientsScreen(header: @Composable () -> Unit, viewModel: ClientsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.onIntent(ClientsIntent.Load) }
    ClientsContent(state, viewModel::onIntent, header)
}

/** Clients tab: plans, support tickets, zones and customer lifecycle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsContent(state: ClientsUiState, onIntent: (ClientsIntent) -> Unit, header: @Composable () -> Unit) {
    val dashboard = state.dashboard
    Column(Modifier.fillMaxSize()) {
        ClientsHeroTop(state, onIntent, header)
        PullToRefreshBox(isRefreshing = state.isLoading && dashboard != null, onRefresh = { onIntent(ClientsIntent.Load) },
            modifier = Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                Modifier.fillMaxSize().testTag("clients_list"),
                contentPadding = PaddingValues(bottom = IspSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(IspSpacing.md),
            ) {
                item("hero") { ClientsHeroBody(dashboard) }
                if (dashboard != null) item("kpis") { ClientKpis(dashboard.tickets, dashboard.quality) }
                item("status") { ClientsStatus(state, onIntent) }
                if (dashboard != null) clientItems(dashboard)
            }
        }
    }
}

@Composable
private fun ClientsHeroTop(state: ClientsUiState, onIntent: (ClientsIntent) -> Unit, header: @Composable () -> Unit) {
    HeroTop {
        header()
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            IconButton(onClick = { onIntent(ClientsIntent.Load) }, enabled = !state.isLoading, modifier = Modifier.size(48.dp).testTag("clients_refresh")) {
                Icon(Icons.Default.Refresh, stringResource(R.string.dashboard_refresh), tint = IspTheme.colors.onHero)
            }
        }
    }
}

@Composable
private fun ClientsHeroBody(dashboard: ClientsDashboard?) {
    HeroBand {
        dashboard?.tickets?.let { tickets ->
            val badge = tickets.highPriority?.takeIf { it > 0 }?.let {
                HeroBadge(stringResource(R.string.clients_high_priority, formatCount(it)), DeltaTone.Negative)
            }
            HeroFigure(HeroFigureModel(stringResource(R.string.clients_open_tickets), formatCount(tickets.open), "clients_open_tickets",
                badge = badge, supporting = stringResource(R.string.clients_tickets_caption, formatCount(tickets.total))))
        }
    }
}

@Composable
private fun ClientsStatus(state: ClientsUiState, onIntent: (ClientsIntent) -> Unit) {
    Column(Modifier.padding(horizontal = IspSpacing.lg)) {
        if (state.isLoading && state.dashboard == null) DashboardSkeleton()
        val message = when {
            state.failed -> if (state.dashboard == null) R.string.clients_initial_failed else R.string.dashboard_refresh_failed
            state.dashboard?.hasFailures == true -> R.string.clients_partial_failed
            else -> null
        }
        message?.let {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(it), Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { onIntent(ClientsIntent.Load) }, modifier = Modifier.testTag("clients_retry")) { Text(stringResource(R.string.retry)) }
            }
        }
    }
}

private fun LazyListScope.clientItems(dashboard: ClientsDashboard) {
    dashboard.plans?.let { plans ->
        item("plans_header") { PlansHeader(plans) }
        item("plan_mix") { Padded { PlanMixCard(plans) } }
    }
    dashboard.tickets?.let { tickets ->
        item("tickets_header") { SectionHeader(stringResource(R.string.clients_tickets), Modifier.padding(horizontal = IspSpacing.lg),
            stringResource(R.string.clients_tickets_caption, formatCount(tickets.total))) }
        item("tickets_status") { Padded { TicketStatusCard(tickets) } }
        item("tickets_category") { Padded { CountBarsCard(stringResource(R.string.clients_tickets_by_category), tickets.byCategory, "clients_tickets_category") } }
    }
    dashboard.zones?.let { zones ->
        item("zones") { Padded { ZonesTable(zones) } }
    }
    dashboard.lifecycle?.let { lifecycle ->
        item("lifecycle_header") { SectionHeader(stringResource(R.string.clients_lifecycle), Modifier.padding(horizontal = IspSpacing.lg), stringResource(R.string.clients_lifecycle_caption)) }
        item("lifecycle_kpis") { LifecycleKpis(lifecycle) }
        item("cancel_by_plan") { Padded { CountBarsCard(stringResource(R.string.clients_cancellations_by_plan), lifecycle.cancellationsByPlan, "clients_cancel_by_plan", accent = true) } }
        item("cancel_by_zone") { Padded { CountBarsCard(stringResource(R.string.clients_cancellations_by_zone), lifecycle.cancellationsByZone, "clients_cancel_by_zone", accent = true) } }
    }
}

@Composable
private fun ClientKpis(tickets: TicketSummary?, quality: ClientQuality?) {
    val unavailable = stringResource(R.string.dashboard_unavailable)
    KpiGrid(listOf(
        KpiModel(stringResource(R.string.clients_resolution_time),
            tickets?.averageResolutionHours?.let { stringResource(R.string.clients_hours, formatDecimal(it)) } ?: unavailable, "clients_resolution_time"),
        KpiModel(stringResource(R.string.clients_reopen_rate), tickets?.reopenRatePercent?.let(::formatPercent) ?: unavailable, "clients_reopen_rate"),
        KpiModel(stringResource(R.string.clients_antiquity),
            quality?.averageAntiquityMonths?.let { stringResource(R.string.clients_months, it.roundToInt()) } ?: unavailable, "clients_antiquity",
            supporting = stringResource(R.string.clients_active_clients)),
        // Not computed by the Core yet: overdue debt ÷ delinquent subscriptions.
        KpiModel(stringResource(R.string.clients_debt_per_delinquent), unavailable, "clients_debt_per_delinquent",
            info = stringResource(R.string.clients_debt_per_delinquent_info)),
    ), overlapHero = true)
}

@Composable
private fun PlansHeader(plans: PlanAnalysis) {
    val speed = plans.averageDownloadSpeedMbps?.roundToInt()?.toString() ?: stringResource(R.string.dashboard_unavailable)
    SectionHeader(stringResource(R.string.clients_plans), Modifier.padding(horizontal = IspSpacing.lg),
        stringResource(R.string.clients_plans_caption, formatCount(plans.totalSubscriptions), speed))
}

@Composable
private fun PlanMixCard(plans: PlanAnalysis) {
    val palette = IspTheme.colors.chartCategorical
    val other = IspTheme.colors.chartOther
    val total = plans.totalSubscriptions.coerceAtLeast(1)
    val colored = plans.plans.mapIndexed { index, plan -> plan to if (index < PLAN_SLOTS) palette[index] else other }
    ChartCard(ChartCardHeader(stringResource(R.string.clients_plan_mix)), Modifier.testTag("clients_plan_mix")) {
        if (plans.plans.isEmpty()) {
            EmptyChart(stringResource(R.string.dashboard_no_data))
        } else {
            ProportionBar(colored.map { (plan, color) -> Segment(plan.name, plan.subscriptions.toDouble(), color) })
            PlanRowHeader()
            colored.forEach { (plan, color) -> PlanRow(plan, color, total, plan.name == plans.mostPopularPlan) }
        }
    }
}

@Composable
private fun PlanRowHeader() {
    Row(horizontalArrangement = Arrangement.spacedBy(IspSpacing.sm)) {
        Text(stringResource(R.string.clients_plan_header), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.clients_subs_header), Modifier.width(44.dp), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
        Text("%", Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
        Text(stringResource(R.string.clients_arpu_header), Modifier.width(72.dp), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
    }
}

@Composable
private fun PlanRow(plan: PlanStats, color: androidx.compose.ui.graphics.Color, total: Int, top: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(IspSpacing.sm)) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(IspSpacing.sm)) {
            Swatch(color)
            Text(plan.name, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (top) StatusBadge(stringResource(R.string.clients_top_plan), BadgeTone.Info)
        }
        Text(formatCount(plan.subscriptions), Modifier.width(44.dp), style = IspType.number, textAlign = TextAlign.End)
        Text(formatPercent(plan.subscriptions * 100.0 / total), Modifier.width(40.dp), style = IspType.number, textAlign = TextAlign.End)
        Text(plan.averageRevenue?.let(::formatSoles) ?: "—", Modifier.width(72.dp), style = IspType.number, textAlign = TextAlign.End)
    }
}

@Composable
private fun TicketStatusCard(tickets: TicketSummary) {
    val palette = IspTheme.colors.chartCategorical
    val segments = listOf(
        Segment(stringResource(R.string.clients_ticket_pending), tickets.pending.toDouble(), palette[0]),
        Segment(stringResource(R.string.clients_ticket_assigned), tickets.assigned.toDouble(), palette[1]),
        Segment(stringResource(R.string.clients_ticket_in_progress), tickets.inProgress.toDouble(), palette[2]),
    )
    ChartCard(ChartCardHeader(stringResource(R.string.clients_tickets_by_status), formatCount(tickets.open)), Modifier.testTag("clients_tickets_status")) {
        if (segments.all { it.value <= 0 }) {
            EmptyChart(stringResource(R.string.dashboard_no_data))
        } else {
            ProportionBar(segments)
            segments.forEach { SegmentRow(it.label, formatCount(it.value), it.color) }
        }
        if (tickets.byPriority.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(IspSpacing.sm), verticalArrangement = Arrangement.spacedBy(IspSpacing.xs)) {
                tickets.byPriority.withIndex().reversed().forEach { (priority, count) -> PriorityBadge(priority, count) }
            }
        }
    }
}

/** Core priority scale: 0 = low, 1 = medium, 2 = high; shown from the most urgent. */
@Composable
private fun PriorityBadge(priority: Int, count: Int) {
    val (label, tone) = when (priority) {
        0 -> R.string.clients_priority_low to BadgeTone.Neutral
        1 -> R.string.clients_priority_medium to BadgeTone.Warning
        else -> R.string.clients_priority_high to BadgeTone.Error
    }
    StatusBadge(stringResource(label, formatCount(count)), tone)
}

@Composable
private fun CountBarsCard(title: String, counts: List<Pair<String, Int>>, tag: String, accent: Boolean = false) {
    val color = if (accent) IspTheme.colors.chartCategorical[1] else IspTheme.colors.chartPrimary
    ChartCard(ChartCardHeader(title, formatCount(counts.sumOf { it.second })), Modifier.testTag(tag)) {
        if (counts.isEmpty()) {
            EmptyChart(stringResource(R.string.dashboard_no_data))
        } else {
            HorizontalBarList(counts.map { (label, count) -> BarItem(label, formatCount(count), count.toDouble()) }, barColor = color)
        }
    }
}

@Composable
private fun ZonesTable(zones: List<ZonePerformance>) {
    val median = zones.mapNotNull { it.incidencePer100 }.sorted().let { if (it.isEmpty()) null else it[it.size / 2] }
    ChartCard(ChartCardHeader(stringResource(R.string.clients_zones), stringResource(R.string.clients_zones_caption)), Modifier.testTag("clients_zones")) {
        if (zones.isEmpty()) EmptyChart(stringResource(R.string.dashboard_no_data))
        ZoneRowHeader()
        zones.forEach { zone ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ZoneRow(zone, median)
        }
    }
}

@Composable
private fun ZoneRowHeader() {
    val style = MaterialTheme.typography.labelSmall
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(horizontalArrangement = Arrangement.spacedBy(IspSpacing.sm)) {
        Text(stringResource(R.string.clients_zone_header), Modifier.weight(1f), style = style, color = color)
        Text(stringResource(R.string.clients_clients_header), Modifier.width(52.dp), style = style, color = color, textAlign = TextAlign.End)
        Text(stringResource(R.string.clients_revenue_header), Modifier.width(76.dp), style = style, color = color, textAlign = TextAlign.End)
        Text(stringResource(R.string.clients_incidence_header), Modifier.width(64.dp), style = style, color = color, textAlign = TextAlign.End)
    }
}

/** Incidence above 1.5× the median is flagged red, above the median amber. */
@Composable
private fun ZoneRow(zone: ZonePerformance, median: Double?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(IspSpacing.sm)) {
        Text(zone.zone, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(formatCount(zone.clients), Modifier.width(52.dp), style = IspType.number, textAlign = TextAlign.End)
        Text(zone.revenue?.let(::formatSoles) ?: "—", Modifier.width(76.dp), style = IspType.number, textAlign = TextAlign.End, maxLines = 1)
        Row(Modifier.width(64.dp), horizontalArrangement = Arrangement.End) {
            zone.incidencePer100?.let { StatusBadge(formatDecimal(it), incidenceTone(it, median)) } ?: Text("—", style = IspType.number)
        }
    }
}

private fun incidenceTone(value: Double, median: Double?): BadgeTone = when {
    median == null || median <= 0 -> BadgeTone.Neutral
    value > median * 1.5 -> BadgeTone.Error
    value > median -> BadgeTone.Warning
    else -> BadgeTone.Success
}

@Composable
private fun LifecycleKpis(lifecycle: ClientLifecycle) {
    val unavailable = stringResource(R.string.dashboard_unavailable)
    KpiGrid(listOf(
        KpiModel(stringResource(R.string.clients_days_to_first_issue),
            lifecycle.averageDaysToFirstIssue?.let { stringResource(R.string.clients_days, it.roundToInt()) } ?: unavailable,
            "clients_days_first_issue", supporting = stringResource(R.string.clients_average)),
        KpiModel(stringResource(R.string.clients_days_to_cancellation),
            lifecycle.averageDaysToCancellation?.let { stringResource(R.string.clients_days, it.roundToInt()) } ?: unavailable,
            "clients_days_cancellation", supporting = stringResource(R.string.clients_average)),
    ))
}

@Preview(heightDp = 1600)
@Composable
private fun ClientsContentPreview() {
    val dashboard = ClientsDashboard(
        plans = PlanAnalysis(listOf(PlanStats("Fibra 60 Mbps", 486, 28.4), PlanStats("Fibra 100 Mbps", 398, 37.9), PlanStats("Inalámbrico 10 Mbps", 154, 24.1)),
            "Fibra 60 Mbps", 92.0),
        tickets = TicketSummary(112, 9, 6, 8, 0, 18.4, 6.2, listOf("Sin internet" to 46, "Lentitud" to 28), listOf(7, 11, 5)),
        quality = ClientQuality(26.0),
        zones = listOf(ZonePerformance("Zona 1", 412, 13600.0, 5.3), ZonePerformance("Zona 3", 276, 8700.0, 14.1)),
        lifecycle = ClientLifecycle(47.0, 412.0, listOf("Fibra 60 Mbps" to 41), listOf("Zona 3" to 38)),
    )
    MyTheme(darkTheme = true) { ClientsContent(ClientsUiState(dashboard = dashboard), {}, {}) }
}
