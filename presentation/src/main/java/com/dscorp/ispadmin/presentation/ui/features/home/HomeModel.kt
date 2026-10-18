package com.dscorp.ispadmin.presentation.ui.features.home

import com.dscorp.ispadmin.data.datasource.remote.HomeSummaryResponse
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.formatSoles
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.toTitleCase
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private val WEEKDAYS = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")
private val MONTHS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "set", "oct", "nov", "dic")

/** "oct" */
internal fun monthShort(date: LocalDate): String = MONTHS[date.monthValue - 1]

/** "14 oct" */
internal fun dayMonth(date: LocalDate): String = "${date.dayOfMonth} ${MONTHS[date.monthValue - 1]}"

/** "mié 14 oct" */
internal fun weekdayDayMonth(date: LocalDate): String = "${WEEKDAYS[date.dayOfWeek.value - 1]} ${dayMonth(date)}"

data class Delta(val text: String, val positive: Boolean)
enum class AttentionKind { CUT, OWING, TICKETS }
data class AttentionRow(val kind: AttentionKind, val title: String, val subtitle: String)
data class OwingRow(val id: Int, val initials: String, val name: String, val detail: String, val debt: String)

/** Secretary home (a-ini-02): what came in today and what needs a call before the next cut. */
data class SecretaryHome(
    val todayTitle: String,
    val todayAmount: String,
    val todayCount: String,
    val delta: Delta?,
    val deltaVs: String,
    val attention: List<AttentionRow>,
    val owingTitle: String,
    val owing: List<OwingRow>,
) {
    companion object {
        fun from(summary: HomeSummaryResponse): SecretaryHome {
            val today = LocalDateTime.parse(summary.asOf).toLocalDate()
            val collections = summary.collections
            val lastWeek = runCatching { LocalDate.parse(collections.lastWeekDate) }.getOrElse { today.minusDays(7) }
            val owing = summary.owingTwoMonths
            return SecretaryHome(
                todayTitle = "Cobrado hoy · ${weekdayDayMonth(today)}",
                todayAmount = formatSoles(collections.today.amount),
                todayCount = plural(collections.today.count, "cobro", "cobros"),
                delta = delta(collections.today.amount - collections.sameWeekdayLastWeek.amount),
                deltaVs = "vs ${weekdayDayMonth(lastWeek)}",
                attention = attention(summary, today),
                owingTitle = "Deben 2 meses · ${owing.count}",
                owing = owing.top.map { row ->
                    val name = row.name.orEmpty().toTitleCase()
                    OwingRow(
                        id = row.id,
                        initials = name.split(' ').filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() },
                        name = name,
                        detail = listOfNotNull(row.plan?.trim(), row.place?.toTitleCase()).filter { it.isNotEmpty() }.joinToString(" · "),
                        debt = formatSoles(row.debt),
                    )
                },
            )
        }

        private fun delta(diff: Double): Delta? = when {
            abs(diff) < 0.005 -> null
            diff > 0 -> Delta("▲ ${formatSoles(diff)}", positive = true)
            else -> Delta("▼ ${formatSoles(-diff)}", positive = false)
        }

        private fun attention(summary: HomeSummaryResponse, today: LocalDate): List<AttentionRow> {
            val cut = summary.cut
            val cutDate = runCatching { LocalDate.parse(cut.nextCutDate) }.getOrNull()
            val rows = mutableListOf<AttentionRow>()
            if (cutDate != null && cut.candidates > 0) {
                val days = ChronoUnit.DAYS.between(today, cutDate)
                val title = when {
                    !cut.automatic -> "Corte del ${weekdayDayMonth(cutDate)}"
                    days == 1L -> "Corte automático esta medianoche"
                    else -> "Corte automático el ${weekdayDayMonth(cutDate)}"
                }
                rows += AttentionRow(AttentionKind.CUT, title, "${plural(cut.candidates, "suscripción", "suscripciones")} con recibo pendiente")
            }
            val owing = summary.owingTwoMonths.count
            if (owing > 0) {
                val subtitle = cutDate?.let { "Cobra antes del corte del ${dayMonth(it)}" } ?: "Recibos vencidos de 2 meses"
                rows += AttentionRow(AttentionKind.OWING, "$owing deben 2 meses", subtitle)
            }
            val tickets = summary.tickets
            if (tickets.pending > 0) {
                val high = if (tickets.highPriority > 0) "${tickets.highPriority} de prioridad alta" else "Ninguno de prioridad alta"
                rows += AttentionRow(AttentionKind.TICKETS, plural(tickets.pending, "ticket pendiente", "tickets pendientes"), high)
            }
            return rows
        }

        private fun plural(count: Long, one: String, many: String) = "$count ${if (count == 1L) one else many}"
    }
}
