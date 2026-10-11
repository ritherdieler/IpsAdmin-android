package com.dscorp.ispadmin.presentation.ui.features.dashboard

import com.dscorp.ispadmin.domain.model.DashBoardDataResponse
import com.dscorp.ispadmin.domain.model.SubscriptionsResumeStatics
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date

private const val WINDOW_MONTHS = 6L
private const val PERCENT = 100.0
private const val CANCEL_ACTION = "TOTAL_CANCEL_SUBSCRIPTION"
private const val RECONNECT_ACTION = "RECONNECT_CANCELLED_SUBSCRIPTION"
private const val MIGRATE_ACTION = "MIGRATE_SUBSCRIPTION"

/** Values aligned by index with [months], oldest first. */
data class MonthlyTrend(val months: List<YearMonth>, val values: List<Double>) {
    val current: Double? get() = values.lastOrNull()
    val previous: Double? get() = values.getOrNull(values.size - 2)
    val total: Double get() = values.sum()
}

/** What the commercial tab shows, derived from the legacy dashboard endpoint. */
data class CommercialDashboard(
    val activeSubscriptions: Int,
    val active: MonthlyTrend,
    val newSubscriptions: MonthlyTrend,
    val cancellations: MonthlyTrend,
    val loggedCancellations: MonthlyTrend,
    val reconnections: MonthlyTrend,
    val migrations: MonthlyTrend,
    val churnPercent: MonthlyTrend,
    val arpu: MonthlyTrend,
    val cancelledByUser: Int,
    val cancelledBySystem: Int,
    val paymentMethods: Map<String, Double>,
    val installations: SubscriptionsResumeStatics,
)

fun DashBoardDataResponse.toCommercialDashboard(zone: ZoneId = ZoneId.of("America/Lima")): CommercialDashboard {
    val history = subscriptionsHistoryStatics.sortedBy { it.date }.takeLast(WINDOW_MONTHS.toInt() + 1)
    val historyMonths = history.map { it.date.toYearMonth(zone) }
    val window = windowMonths(historyMonths.lastOrNull() ?: YearMonth.now(zone))
    val activeByMonth = historyMonths.zip(history.map { it.totalActiveSubscriptions.toDouble() }).toMap()
    return CommercialDashboard(
        activeSubscriptions = activeSubscriptions,
        active = trend(window, activeByMonth, fillMissing = false),
        newSubscriptions = trend(window, historyMonths.zip(history.map { it.newSubscriptions.toDouble() }).toMap(), fillMissing = true),
        cancellations = trend(window, historyMonths.zip(history.map { it.cancelledSubscriptions.toDouble() }).toMap(), fillMissing = true),
        loggedCancellations = logTrend(window, CANCEL_ACTION),
        reconnections = logTrend(window, RECONNECT_ACTION),
        migrations = logTrend(window, MIGRATE_ACTION),
        churnPercent = churn(window, history.map { it.date.toYearMonth(zone) to it }, activeByMonth),
        arpu = arpu(window, zone, activeByMonth),
        cancelledByUser = cancellationsResume.cancelledByUsers,
        cancelledBySystem = cancellationsResume.cancelledBySystem,
        paymentMethods = paymentResume,
        installations = subscriptionsResume,
    )
}

private fun windowMonths(end: YearMonth): List<YearMonth> = (WINDOW_MONTHS - 1 downTo 0).map { end.minusMonths(it) }

private fun trend(window: List<YearMonth>, byMonth: Map<YearMonth, Double>, fillMissing: Boolean): MonthlyTrend {
    val months = if (fillMissing) window else window.filter { it in byMonth }
    return MonthlyTrend(months, months.map { byMonth[it] ?: 0.0 })
}

private fun DashBoardDataResponse.logTrend(window: List<YearMonth>, action: String): MonthlyTrend {
    val details = subscriptionLogSummary?.get(action)?.monthlyDetails.orEmpty()
    val byMonth = details.mapNotNull { detail ->
        runCatching { YearMonth.parse(detail.period.take(7)) }.getOrNull()?.let { it to detail.count.toDouble() }
    }.toMap()
    return trend(window, byMonth, fillMissing = true)
}

/** Cancellations of a month ÷ active subscriptions at the end of the previous month. */
private fun churn(
    window: List<YearMonth>,
    history: List<Pair<YearMonth, com.dscorp.ispadmin.domain.model.MonthlySubscriptionResume>>,
    activeByMonth: Map<YearMonth, Double>,
): MonthlyTrend {
    val byMonth = history.mapNotNull { (month, resume) ->
        val activeAtStart = activeByMonth[month.minusMonths(1)]?.takeIf { it > 0 } ?: return@mapNotNull null
        month to resume.cancelledSubscriptions / activeAtStart * PERCENT
    }.toMap()
    return trend(window, byMonth, fillMissing = false)
}

/** Collected in the month ÷ active subscriptions of that month. */
private fun DashBoardDataResponse.arpu(window: List<YearMonth>, zone: ZoneId, activeByMonth: Map<YearMonth, Double>): MonthlyTrend {
    val byMonth = monthlyCollects.mapNotNull { collect ->
        val month = collect.date.toYearMonth(zone)
        val active = activeByMonth[month]?.takeIf { it > 0 } ?: return@mapNotNull null
        month to collect.totalRaised / active
    }.toMap()
    return trend(window, byMonth, fillMissing = false)
}

private fun Date.toYearMonth(zone: ZoneId): YearMonth = YearMonth.from(toInstant().atZone(zone))
