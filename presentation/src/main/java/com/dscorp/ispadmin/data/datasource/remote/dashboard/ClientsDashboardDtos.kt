package com.dscorp.ispadmin.data.datasource.remote.dashboard

import com.dscorp.ispadmin.domain.model.dashboard.ClientLifecycle
import com.dscorp.ispadmin.domain.model.dashboard.ClientQuality
import com.dscorp.ispadmin.domain.model.dashboard.PlanAnalysis
import com.dscorp.ispadmin.domain.model.dashboard.PlanStats
import com.dscorp.ispadmin.domain.model.dashboard.TicketSummary
import com.dscorp.ispadmin.domain.model.dashboard.TicketMonthQuality
import com.dscorp.ispadmin.domain.model.dashboard.ZonePerformance

// Gson may leave non-null fields null when the server omits them, so every field is nullable here.

data class PlanAnalysisDto(
    val planDistribution: Map<String, Int>? = null,
    val mostPopularPlan: String? = null,
    val averageDownloadSpeed: Double? = null,
    val averageRevenuePerPlan: Map<String, Double>? = null,
) {
    fun toDomain(): PlanAnalysis {
        val revenue = averageRevenuePerPlan.orEmpty()
        val plans = planDistribution.orEmpty().map { (name, count) -> PlanStats(name, count, revenue[name]) }
            .sortedByDescending { it.subscriptions }
        return PlanAnalysis(plans, mostPopularPlan, averageDownloadSpeed?.takeUnless { it.isNaN() })
    }
}

data class AssistanceTicketsDto(
    val totalTickets: Int? = null,
    val pendingTickets: Int? = null,
    val assignedTickets: Int? = null,
    val inProgressTickets: Int? = null,
    val reopenedTickets: Int? = null,
    val averageResolutionTimeHours: Double? = null,
    val reopenRate: Double? = null,
    val ticketsByCategory: Map<String, Int>? = null,
    val ticketsByPriority: List<Int>? = null,
    val openTicketsByPriority: List<Int>? = null,
    val monthlyQuality: List<TicketMonthQualityDto>? = null,
    val openTicketsWeekAgo: Int? = null,
) {
    fun toDomain() = TicketSummary(
        total = totalTickets ?: 0,
        pending = pendingTickets ?: 0,
        assigned = assignedTickets ?: 0,
        inProgress = inProgressTickets ?: 0,
        reopened = reopenedTickets ?: 0,
        averageResolutionHours = averageResolutionTimeHours?.takeUnless { it.isNaN() },
        reopenRatePercent = reopenRate?.takeUnless { it.isNaN() },
        byCategory = ticketsByCategory.orEmpty().entries.sortedByDescending { it.value }.map { it.key to it.value },
        byPriority = ticketsByPriority.orEmpty(),
        openHighPriority = openTicketsByPriority?.getOrNull(TicketSummary.HIGH_PRIORITY),
        monthly = monthlyQuality.orEmpty().mapNotNull { it.toDomain() },
        openWeekAgo = openTicketsWeekAgo,
    )
}

data class ClientQualityDto(val averageAntiquityMonths: Double? = null) {
    fun toDomain() = ClientQuality(averageAntiquityMonths?.takeUnless { it.isNaN() })
}

data class GeographicPerformanceDto(
    val clientDensityByPlace: Map<String, Int>? = null,
    val revenueByPlace: Map<String, Double>? = null,
    val incidenceRateByPlace: Map<String, Double>? = null,
) {
    fun toDomain(): List<ZonePerformance> = clientDensityByPlace.orEmpty().map { (zone, clients) ->
        ZonePerformance(zone, clients, revenueByPlace?.get(zone), incidenceRateByPlace?.get(zone))
    }.sortedByDescending { it.clients }
}

data class ClientLifecycleDto(
    val averageDaysToFirstIssue: Double? = null,
    val averageDaysToCancellation: Double? = null,
    val cancellationsByPlan: Map<String, Int>? = null,
    val cancellationsByZone: Map<String, Int>? = null,
) {
    fun toDomain() = ClientLifecycle(
        averageDaysToFirstIssue = averageDaysToFirstIssue?.takeUnless { it.isNaN() },
        averageDaysToCancellation = averageDaysToCancellation?.takeUnless { it.isNaN() },
        cancellationsByPlan = cancellationsByPlan.orEmpty().entries.sortedByDescending { it.value }.map { it.key to it.value },
        cancellationsByZone = cancellationsByZone.orEmpty().entries.sortedByDescending { it.value }.map { it.key to it.value },
    )
}

data class TicketMonthQualityDto(val month: String? = null, val averageResolutionTimeHours: Double? = null, val reopenRate: Double? = null) {
    fun toDomain(): TicketMonthQuality? {
        val parsed = month?.let { runCatching { java.time.YearMonth.parse(it) }.getOrNull() } ?: return null
        return TicketMonthQuality(parsed, averageResolutionTimeHours?.takeUnless { it.isNaN() }, reopenRate?.takeUnless { it.isNaN() })
    }
}
