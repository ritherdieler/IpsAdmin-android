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
    val change = current - previous
    return KpiDelta("${arrow(change)} ${formatCount(abs(change))}", tone(change, polarity), comparison)
}

/** Difference in percentage points: "▼ 0.3 pp". */
internal fun pointsDelta(trend: MonthlyTrend, polarity: Polarity, comparison: String?): KpiDelta? {
    val current = trend.current ?: return null
    val previous = trend.previous ?: return null
    val change = current - previous
    return KpiDelta("${arrow(change)} ${formatDecimal(abs(change))} pp", tone(change, polarity), comparison)
}

internal fun solesDelta(trend: MonthlyTrend, polarity: Polarity, comparison: String?): KpiDelta? {
    val current = trend.current ?: return null
    val previous = trend.previous ?: return null
    val change = current - previous
    return KpiDelta("${arrow(change)} ${formatSoles(abs(change))}", tone(change, polarity), comparison)
}
