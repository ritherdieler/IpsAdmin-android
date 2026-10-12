package com.dscorp.ispadmin.presentation.ui.features.dashboard

import com.dscorp.ispadmin.domain.model.DashBoardDataResponse
import com.dscorp.ispadmin.domain.model.SubscriptionsResumeStatics
import java.text.Normalizer
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date

private const val WINDOW_MONTHS = 6L
private const val PERCENT = 100.0
private const val NEW_ACTION = "NEW_SUBSCRIPTION"
private const val CANCEL_ACTION = "TOTAL_CANCEL_SUBSCRIPTION"
private const val RECONNECT_ACTION = "RECONNECT_CANCELLED_SUBSCRIPTION"
private const val MIGRATE_ACTION = "MIGRATE_SUBSCRIPTION"

/** Values aligned by index with [months], oldest first. */
data class MonthlyTrend(val months: List<YearMonth>, val values: List<Double>) {
    val current: Double? get() = values.lastOrNull()
    val previous: Double? get() = values.getOrNull(values.size - 2)
    val total: Double get() = values.sum()
    val average: Double? get() = values.takeIf { it.isNotEmpty() }?.average()
}

/** New + reconnected − cancelled subscriptions of the current month. */
internal fun netAdds(new: MonthlyTrend, reconnections: MonthlyTrend, cancellations: MonthlyTrend): Int? {
    val added = (new.current ?: return null) + (reconnections.current ?: return null)
    return (added - (cancellations.current ?: return null)).toInt()
}

/** Last month of [trend] when it is not [currentMonth], so the figure can say which month it is. */
internal fun staleMonth(trend: MonthlyTrend, currentMonth: YearMonth): YearMonth? = trend.months.lastOrNull()?.takeIf { it != currentMonth }

/**
 * What the commercial tab shows, derived from the legacy dashboard endpoint.
 *
 * Event counts (new, cancelled, reconnected, migrated) all come from the subscription event log so the
 * tiles and charts agree. The monthly snapshot only provides the non-cancelled base (active + cut off).
 */
data class CommercialDashboard(
    /** Subscriptions in ACTIVE status right now. */
    val activeSubscriptions: Int,
    /** Non-cancelled subscriptions (active + cut off) at each monthly snapshot. */
    val nonCancelled: MonthlyTrend,
    val newSubscriptions: MonthlyTrend,
    val cancellations: MonthlyTrend,
    val reconnections: MonthlyTrend,
    val migrations: MonthlyTrend,
    val churnPercent: MonthlyTrend,
    val arpu: MonthlyTrend,
    val cancelledByUser: Int,
    val cancelledBySystem: Int,
    /** Share of payments made by digital methods, per month. */
    val digitalPaymentsPercent: MonthlyTrend,
    val installations: SubscriptionsResumeStatics,
)

fun DashBoardDataResponse.toCommercialDashboard(
    zone: ZoneId = ZoneId.of("America/Lima"),
    currentMonth: YearMonth = YearMonth.now(zone),
): CommercialDashboard {
    val window = (WINDOW_MONTHS - 1 downTo 0).map { currentMonth.minusMonths(it) }
    val baseByMonth = subscriptionsHistoryStatics
        .associate { it.date.toYearMonth(zone) to it.totalActiveSubscriptions.toDouble() }
    val cancellations = logTrend(window, CANCEL_ACTION)
    return CommercialDashboard(
        activeSubscriptions = activeSubscriptions,
        nonCancelled = trend(window, baseByMonth, fillMissing = false),
        newSubscriptions = logTrend(window, NEW_ACTION),
        cancellations = cancellations,
        reconnections = logTrend(window, RECONNECT_ACTION),
        migrations = logTrend(window, MIGRATE_ACTION),
        churnPercent = churn(cancellations, baseByMonth),
        arpu = arpu(window, zone, baseByMonth),
        cancelledByUser = cancellationsResume.cancelledByUsers,
        cancelledBySystem = cancellationsResume.cancelledBySystem,
        digitalPaymentsPercent = digitalPayments(paymentResume, currentMonth),
        installations = subscriptionsResume,
    )
}

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

/** Cancellations of a month ÷ non-cancelled subscriptions at the previous month's snapshot. */
private fun churn(cancellations: MonthlyTrend, baseByMonth: Map<YearMonth, Double>): MonthlyTrend {
    val byMonth = cancellations.months.zip(cancellations.values).mapNotNull { (month, cancelled) ->
        val base = baseByMonth[month.minusMonths(1)]?.takeIf { it > 0 } ?: return@mapNotNull null
        month to cancelled / base * PERCENT
    }.toMap()
    return trend(cancellations.months, byMonth, fillMissing = false)
}

/**
 * Collected in soles ÷ non-cancelled subscriptions of the month.
 * `/dashboard` sends `totalRaised` as the collected percentage of `grossIncome`.
 */
private fun DashBoardDataResponse.arpu(window: List<YearMonth>, zone: ZoneId, baseByMonth: Map<YearMonth, Double>): MonthlyTrend {
    val byMonth = monthlyCollects.mapNotNull { collect ->
        val month = collect.date.toYearMonth(zone)
        val base = baseByMonth[month]?.takeIf { it > 0 } ?: return@mapNotNull null
        month to collectedSoles(collect.grossIncome, collect.totalRaised) / base
    }.toMap()
    return trend(window, byMonth, fillMissing = false)
}

internal fun collectedSoles(grossIncome: Double, collectedPercent: Double): Double = grossIncome * collectedPercent / PERCENT

private val SPANISH_MONTHS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/**
 * `paymentResume` is keyed by short Spanish month name ("May.", "Set.") without a year. Each key is placed on
 * its most recent occurrence up to [currentMonth]; only the months of the dashboard window are kept, sorted chronologically.
 */
internal fun digitalPayments(paymentResume: Map<String, Double>, currentMonth: YearMonth): MonthlyTrend {
    val byMonth = paymentResume.mapNotNull { (key, percent) ->
        val number = monthNumber(key) ?: return@mapNotNull null
        val yearsBack = if (number > currentMonth.monthValue) 1L else 0L
        YearMonth.of(currentMonth.year, number).minusYears(yearsBack) to percent
    }.toMap()
    val firstMonth = currentMonth.minusMonths(WINDOW_MONTHS - 1)
    val months = byMonth.keys.filter { it >= firstMonth }.sorted()
    return MonthlyTrend(months, months.map { byMonth.getValue(it) })
}

private fun monthNumber(key: String): Int? {
    val plain = Normalizer.normalize(key, Normalizer.Form.NFD).replace("\\p{M}".toRegex(), "")
        .trim().trimEnd('.').lowercase().take(3).let { if (it == "set") "sep" else it }
    return SPANISH_MONTHS.indexOf(plain).takeIf { it >= 0 }?.plus(1)
}

private fun Date.toYearMonth(zone: ZoneId): YearMonth = YearMonth.from(toInstant().atZone(zone))
