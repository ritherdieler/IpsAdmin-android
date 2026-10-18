package com.dscorp.ispadmin.presentation.ui.features.payment.cash

import com.dscorp.ispadmin.data.datasource.remote.CashDayResponse
import com.dscorp.ispadmin.data.datasource.remote.CashReceiptResponse
import com.dscorp.ispadmin.presentation.ui.features.home.monthShort
import com.dscorp.ispadmin.presentation.ui.features.home.weekdayDayMonth
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.formatSoles
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.toTitleCase
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

private const val TOP_COLLECTORS = 3
private const val SAME_CHARGE_MINUTES = 2L

data class CashMethod(val label: String, val amount: String, val count: String, val share: Float)
data class CashRow(val subscriptionId: Int?, val time: String, val name: String, val detail: String, val amount: String)

/** Caja (a-caj-01): what came in on one day, by method, and the charges newest first. */
data class CashDay(
    val date: LocalDate,
    val title: String,
    val amount: String,
    val subtitle: String,
    val methods: List<CashMethod>,
    val rows: List<CashRow>,
) {
    companion object {
        fun from(response: CashDayResponse, today: LocalDate): CashDay {
            val date = runCatching { LocalDate.parse(response.date) }.getOrDefault(today)
            val total = response.total
            val collectors = response.byCollector.take(TOP_COLLECTORS).joinToString("") { " · ${shortName(it.name)} ${it.count}" }
            return CashDay(
                date = date,
                title = if (date == today) "Cobrado hoy · ${weekdayDayMonth(date)}" else "Cobrado el ${weekdayDayMonth(date)}",
                amount = formatSoles(total.amount),
                subtitle = "${total.count} ${if (total.count == 1L) "cobro" else "cobros"}$collectors",
                methods = response.byMethod.map {
                    CashMethod(it.label, formatSoles(it.amount), "· ${it.count}", if (total.amount > 0) (it.amount / total.amount).toFloat() else 0f)
                },
                rows = charges(response.receipts).map { charge ->
                    val first = charge.first()
                    val method = first.method?.trim()?.lowercase()?.replaceFirstChar { it.uppercase() }
                    val detail = first.payerName?.takeIf { it.isNotBlank() }?.let { listOfNotNull(method, "pagó $it").joinToString(" · ") }
                        ?: listOfNotNull(
                            method,
                            charge.mapNotNull { it.serviceMonth }.takeIf { it.isNotEmpty() }?.let(::monthsLabel),
                            first.collector?.let(::shortName),
                        ).joinToString(" · ")
                    CashRow(
                        subscriptionId = first.subscriptionId,
                        time = first.paidAt.take(16).takeLast(5),
                        name = first.subscriber.orEmpty().toTitleCase().ifBlank { "Sin titular" },
                        detail = detail,
                        amount = formatSoles(charge.sumOf { it.amount }),
                    )
                },
            )
        }

        /** Receipts of one charge (one per month) arrive together: same subscription, method, collector and payer. */
        private fun charges(receipts: List<CashReceiptResponse>): List<List<CashReceiptResponse>> {
            val groups = mutableListOf<MutableList<CashReceiptResponse>>()
            receipts.forEach { receipt ->
                val last = groups.lastOrNull()?.last()
                if (last != null && sameCharge(last, receipt)) groups.last() += receipt else groups += mutableListOf(receipt)
            }
            return groups
        }

        private fun sameCharge(a: CashReceiptResponse, b: CashReceiptResponse): Boolean {
            if (a.subscriptionId == null || a.subscriptionId != b.subscriptionId) return false
            if (a.method != b.method || a.collector != b.collector || a.payerName != b.payerName) return false
            val gap = runCatching { Duration.between(LocalDateTime.parse(b.paidAt), LocalDateTime.parse(a.paidAt)).abs().toMinutes() }.getOrDefault(Long.MAX_VALUE)
            return gap <= SAME_CHARGE_MINUTES
        }
    }
}

/** "oct", "ago y set", "jul, ago y set" from YYYY-MM months. */
internal fun monthsLabel(months: List<String>): String {
    val names = months.mapNotNull { runCatching { YearMonth.parse(it) }.getOrNull() }.distinct().sorted().map { monthShort(it.atDay(1)) }
    return when (names.size) {
        0 -> ""
        1 -> names[0]
        else -> names.dropLast(1).joinToString(", ") + " y " + names.last()
    }
}

/** "Rosa C." for the collector line. */
internal fun shortName(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.size < 2) return parts.firstOrNull()?.toTitleCase().orEmpty()
    return "${parts[0].toTitleCase()} ${parts[1].first().uppercaseChar()}."
}
