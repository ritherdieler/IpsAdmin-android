package com.dscorp.ispadmin.presentation.ui.features.dashboard

import com.dscorp.ispadmin.presentation.designsystem.components.DeltaTone
import com.dscorp.ispadmin.presentation.designsystem.components.KpiDelta
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

private val PERU = Locale.forLanguageTag("es-PE")

internal fun YearMonth.shortLabel(): String = month.getDisplayName(TextStyle.SHORT, PERU).trimEnd('.')

internal fun formatCount(value: Number): String = NumberFormat.getIntegerInstance(PERU).format(value)

internal fun formatSoles(value: Double): String = NumberFormat.getCurrencyInstance(PERU).apply {
    currency = java.util.Currency.getInstance("PEN")
}.format(value)

internal fun formatPercent(value: Double): String = "%.1f%%".format(PERU, value)

internal fun formatDecimal(value: Double): String = "%.1f".format(PERU, value)

private const val COMPACT_FROM = 10_000.0

/** Soles shortened from ten thousand so they fit a table column: "S/ 13.6k", "S/ 986.7M". */
internal fun formatSolesCompact(value: Double): String = when {
    kotlin.math.abs(value) >= 1_000_000 -> "S/ %.1fM".format(PERU, value / 1_000_000)
    kotlin.math.abs(value) >= COMPACT_FROM -> "S/ %.1fk".format(PERU, value / 1_000)
    else -> formatSoles(value)
}

/** Rounds to the precision shown, so a change that prints as zero is also treated as no change. */
private fun Double.roundTo(decimals: Int): Double = java.math.BigDecimal(this).setScale(decimals, java.math.RoundingMode.HALF_UP).toDouble()

/** Which direction of change is good for this metric. */
enum class Polarity { HigherIsBetter, LowerIsBetter, Neutral }

private fun tone(change: Double, polarity: Polarity): DeltaTone = when {
    change == 0.0 || polarity == Polarity.Neutral -> DeltaTone.Neutral
    (change > 0) == (polarity == Polarity.HigherIsBetter) -> DeltaTone.Positive
    else -> DeltaTone.Negative
}

private fun arrow(change: Double) = when {
    change > 0 -> "▲"
    change < 0 -> "▼"
    else -> "="
}

/** "▲ 5" / "▼ 3" / "= 0", compared with [comparison] (e.g. "vs sep"). */
internal fun countDelta(trend: MonthlyTrend, polarity: Polarity, comparison: String?): KpiDelta? {
    val current = trend.current ?: return null
    val previous = trend.previous ?: return null
    val change = (current - previous).roundTo(0)
    return KpiDelta("${arrow(change)} ${formatCount(abs(change))}", tone(change, polarity), comparison)
}

/** Difference in percentage points: "▼ 0.3 pp". */
internal fun pointsDelta(trend: MonthlyTrend, polarity: Polarity, comparison: String?): KpiDelta? {
    val current = trend.current ?: return null
    val previous = trend.previous ?: return null
    val change = (current - previous).roundTo(1)
    return KpiDelta("${arrow(change)} ${formatDecimal(abs(change))} pp", tone(change, polarity), comparison)
}

internal fun solesDelta(trend: MonthlyTrend, polarity: Polarity, comparison: String?): KpiDelta? {
    val current = trend.current ?: return null
    val previous = trend.previous ?: return null
    val change = (current - previous).roundTo(2)
    return KpiDelta("${arrow(change)} ${formatSoles(abs(change))}", tone(change, polarity), comparison)
}

/** Change from [from] to [to] in percent; null when [from] is zero. */
internal fun percentChange(from: Double, to: Double): Double? = if (from == 0.0) null else (to - from) / kotlin.math.abs(from) * 100

/** Each value as a percentage of the total; all zero when there is no total. */
internal fun shares(values: List<Double>): List<Double> {
    val total = values.sum()
    return values.map { if (total > 0) it / total * 100 else 0.0 }
}

/**
 * Chronological history of billed months: the month in progress ([currentMonth]) is left out because the monthly
 * billing runs mid-month, and so are trailing months with nothing billed.
 */
internal fun billedMonths(history: List<com.dscorp.ispadmin.domain.model.MonthlyGrossRevenueResume>, currentMonth: YearMonth? = null) =
    history.sortedBy { it.billingMonth }
        .filter { month -> currentMonth == null || runCatching { YearMonth.parse(month.billingMonth.take(7)) < currentMonth }.getOrDefault(true) }
        .dropLastWhile { it.totalCharged <= 0.0 }

/** Change in hours with one decimal: "▼ 2.1 h". */
internal fun hoursDelta(trend: MonthlyTrend, polarity: Polarity, comparison: String?): KpiDelta? {
    val current = trend.current ?: return null
    val previous = trend.previous ?: return null
    val change = (current - previous).roundTo(1)
    return KpiDelta("${arrow(change)} ${formatDecimal(abs(change))} h", tone(change, polarity), comparison)
}

/** Change between two counts: "▼ 5"; null without a previous count. */
internal fun countChange(current: Int, previous: Int?, polarity: Polarity, comparison: String?): KpiDelta? {
    previous ?: return null
    val change = (current - previous).toDouble()
    return KpiDelta("${arrow(change)} ${formatCount(abs(change))}", tone(change, polarity), comparison)
}
