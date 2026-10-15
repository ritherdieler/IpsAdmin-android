package com.dscorp.ispadmin.presentation.ui.features.dashboard

/** Period of the commercial tab: KPIs add up [months]; charts show [chartMonths]. */
enum class CommercialPeriod(val months: Int, val chartMonths: Int) {
    MONTH(1, 6),
    SIX_MONTHS(6, 6),
    TWELVE_MONTHS(12, 12),
}

internal fun MonthlyTrend.takeLastMonths(n: Int): MonthlyTrend = MonthlyTrend(months.takeLast(n), values.takeLast(n))

/** Sum of the period's months; null without data. */
internal fun periodTotal(trend: MonthlyTrend, period: CommercialPeriod): Double? =
    trend.values.takeIf { it.isNotEmpty() }?.takeLast(period.months)?.sum()

/** Sum of the same number of months right before the period; null when they are not all known. */
internal fun previousPeriodTotal(trend: MonthlyTrend, period: CommercialPeriod): Double? =
    previousMonths(trend, period)?.sum()

internal fun periodAverage(trend: MonthlyTrend, period: CommercialPeriod): Double? =
    trend.values.takeIf { it.isNotEmpty() }?.takeLast(period.months)?.average()

internal fun previousPeriodAverage(trend: MonthlyTrend, period: CommercialPeriod): Double? =
    previousMonths(trend, period)?.average()

private fun previousMonths(trend: MonthlyTrend, period: CommercialPeriod): List<Double>? =
    trend.values.dropLast(period.months).takeLast(period.months).takeIf { it.size == period.months }
