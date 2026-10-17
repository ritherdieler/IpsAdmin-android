package com.dscorp.ispadmin.presentation.ui.features.financial

import com.dscorp.ispadmin.domain.model.financial.FinancialReport
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

private const val PERCENT = 100.0

/** Change in percent of the money received against [previous]; up to [sameDay] of the month while the month is in progress. */
internal fun receivedChange(current: FinancialReport, previous: FinancialReport, sameDay: Int?): Double? {
    val now = receivedWithDate(current)?.toDouble() ?: return null
    val before = (if (sameDay == null) receivedWithDate(previous) else receivedUpToDay(previous, sameDay))?.toDouble() ?: return null
    if (before <= 0.0) return null
    return (now - before) / before * PERCENT
}

/** Receipts of the report's movements month paid on or before [day]; null without dated receipts. */
private fun receivedUpToDay(report: FinancialReport, day: Int): BigDecimal? {
    val month = runCatching { YearMonth.parse(report.movementsMonth) }.getOrNull()
    val amounts = report.receipts.mapNotNull { item ->
        val paidAt = item.receipt.paidAt?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() } ?: return@mapNotNull null
        item.receipt.amount.takeIf { (month == null || YearMonth.from(paidAt) == month) && paidAt.dayOfMonth <= day }
    }
    return amounts.takeIf { it.isNotEmpty() }?.fold(BigDecimal.ZERO, BigDecimal::add)
}

/** Money received with an effective date in the movements month, as the hero shows it. */
internal fun receivedWithDate(report: FinancialReport): BigDecimal? {
    val movements = report.movements ?: return null
    movements.receivedWithDate?.let { return it }
    movements.received?.let { return it }
    val month = runCatching { YearMonth.parse(report.movementsMonth) }.getOrNull() ?: return null
    return report.receipts.fold(BigDecimal.ZERO) { total, item ->
        val paidAt = item.receipt.paidAt?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
        if (paidAt != null && YearMonth.from(paidAt) == month) total + item.receipt.amount else total
    }
}
