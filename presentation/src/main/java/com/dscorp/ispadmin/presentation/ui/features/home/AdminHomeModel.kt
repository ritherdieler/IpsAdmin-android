package com.dscorp.ispadmin.presentation.ui.features.home

import com.dscorp.ispadmin.data.datasource.remote.HomeSummaryResponse
import com.dscorp.ispadmin.presentation.designsystem.components.DeltaTone
import com.dscorp.ispadmin.presentation.designsystem.components.KpiDelta
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.formatSoles
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

enum class AdminTileKind { WITH_SERVICE, CUT_OFF, RECEIVABLE, TICKETS }
data class AdminTile(val kind: AdminTileKind, val label: String, val value: String, val supporting: String)

sealed interface AdminHomeAction {
    data class Tile(val kind: AdminTileKind) : AdminHomeAction
    data object Cash : AdminHomeAction
}

/** Admin and accountant home (a-ini-01): summary on top of the financial tab. */
data class AdminHome(
    val subtitle: String,
    val monthTitle: String,
    val monthAmount: String,
    val monthDelta: KpiDelta?,
    val tiles: List<AdminTile>,
    val dailyTitle: String,
    val dailyLabels: List<String>,
    val dailyValues: List<Double>,
    val todayCaption: String,
) {
    companion object {
        fun from(summary: HomeSummaryResponse): AdminHome {
            val asOf = LocalDateTime.parse(summary.asOf)
            val today = asOf.toLocalDate()
            val collections = summary.collections
            val cutDate = runCatching { LocalDate.parse(summary.cut.nextCutDate) }.getOrNull()
            val updated = "actualizado %02d:%02d".format(asOf.hour, asOf.minute)
            val previousMonth = monthShort(today.minusMonths(1))
            val subscriptions = summary.subscriptions
            val tickets = summary.tickets
            val daily = collections.daily.mapNotNull { day -> runCatching { LocalDate.parse(day.date) to day.amount }.getOrNull() }
            return AdminHome(
                subtitle = listOfNotNull(cutDate?.let { "Corte ${dayMonth(it)}" }, updated).joinToString(" · "),
                monthTitle = "Cobrado en ${monthShort(today)} (al día ${today.dayOfMonth})",
                monthAmount = formatSoles(collections.monthToDate.amount),
                monthDelta = delta(collections.monthToDate.amount - collections.previousMonthToDate.amount, "vs $previousMonth al día ${today.dayOfMonth}"),
                tiles = listOf(
                    AdminTile(AdminTileKind.WITH_SERVICE, "Con servicio", subscriptions.active.toString(), "de ${subscriptions.active + subscriptions.cutOff} activos"),
                    AdminTile(AdminTileKind.CUT_OFF, "Cortados", subscriptions.cutOff.toString(), "con deuda vencida"),
                    AdminTile(AdminTileKind.RECEIVABLE, "Por cobrar", formatSoles(summary.overdue.amount), "${summary.overdue.count} ${if (summary.overdue.count == 1L) "abonado" else "abonados"} con deuda"),
                    AdminTile(
                        AdminTileKind.TICKETS, "Tickets pendientes", tickets.pending.toString(),
                        if (tickets.highPriority > 0) "${tickets.highPriority} de prioridad alta" else "Ninguno de prioridad alta",
                    ),
                ),
                dailyTitle = "Cobros por día · ${monthShort(today)}",
                dailyLabels = daily.map { it.first.dayOfMonth.toString() },
                dailyValues = daily.map { it.second },
                todayCaption = "Hoy · ${collections.today.count} ${if (collections.today.count == 1L) "cobro" else "cobros"}",
            )
        }

        private fun delta(diff: Double, comparison: String): KpiDelta? = when {
            abs(diff) < 0.005 -> null
            diff > 0 -> KpiDelta("▲ ${formatSoles(diff)}", DeltaTone.Positive, comparison)
            else -> KpiDelta("▼ ${formatSoles(-diff)}", DeltaTone.Negative, comparison)
        }
    }
}
