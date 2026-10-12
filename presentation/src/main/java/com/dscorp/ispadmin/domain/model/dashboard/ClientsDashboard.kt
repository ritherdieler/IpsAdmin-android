package com.dscorp.ispadmin.domain.model.dashboard

data class PlanStats(val name: String, val subscriptions: Int, val averageRevenue: Double?)

data class PlanAnalysis(val plans: List<PlanStats>, val mostPopularPlan: String?, val averageDownloadSpeedMbps: Double?) {
    val totalSubscriptions: Int get() = plans.sumOf { it.subscriptions }
}

data class TicketSummary(
    val total: Int,
    val pending: Int,
    val assigned: Int,
    val inProgress: Int,
    val reopened: Int,
    val averageResolutionHours: Double?,
    val reopenRatePercent: Double?,
    val byCategory: List<Pair<String, Int>>,
    /** Counts by Core priority: index 0 = low, 1 = medium, 2 = high. */
    val byPriority: List<Int>,
    /** Unfinished tickets of high priority right now; null when the Core does not send it. */
    val openHighPriority: Int? = null,
) {
    val open: Int get() = pending + assigned + inProgress + reopened
    val highPriority: Int? get() = byPriority.getOrNull(HIGH_PRIORITY)

    companion object {
        const val HIGH_PRIORITY = 2
    }
}

data class ClientQuality(val averageAntiquityMonths: Double?)

/** Incidence = tickets per 100 active subscriptions of the zone. */
data class ZonePerformance(val zone: String, val clients: Int, val revenue: Double?, val incidencePer100: Double?)

data class ClientLifecycle(
    val averageDaysToFirstIssue: Double?,
    val averageDaysToCancellation: Double?,
    val cancellationsByPlan: List<Pair<String, Int>>,
    val cancellationsByZone: List<Pair<String, Int>>,
)

/** Each section loads on its own; a null section failed to load. */
data class ClientsDashboard(
    val plans: PlanAnalysis?,
    val tickets: TicketSummary?,
    val quality: ClientQuality?,
    val zones: List<ZonePerformance>?,
    val lifecycle: ClientLifecycle?,
) {
    val hasFailures: Boolean get() = listOf(plans, tickets, quality, zones, lifecycle).any { it == null }
}
