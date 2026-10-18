package com.dscorp.ispadmin.presentation.ui.features.outlay

import com.dscorp.ispadmin.data.model.OutlayDTO
import com.dscorp.ispadmin.presentation.ui.features.home.dayMonth
import com.dscorp.ispadmin.presentation.ui.features.home.monthShort
import com.dscorp.ispadmin.presentation.ui.features.payment.cash.shortName
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.formatSoles
import java.time.LocalDate
import java.time.ZoneId

private val LIMA: ZoneId = ZoneId.of("America/Lima")
private const val BREAKDOWN_CATEGORIES = 3

data class OutlayRow(val id: Int?, val description: String, val amount: String, val detail: String)

/** Egresos (a-caj-08): the current month, its total split by category, newest first. */
data class OutlayMonth(
    val title: String,
    val total: String,
    val count: String,
    val breakdown: String,
    val categories: List<String>,
    val rows: List<OutlayRow>,
) {
    companion object {
        fun from(outlays: List<OutlayDTO>, category: String?, today: LocalDate): OutlayMonth {
            val byCategory = outlays.groupBy { it.category?.takeIf(String::isNotBlank) ?: "Otros" }
                .mapValues { (_, items) -> items.sumOf { it.amount } }
                .entries.sortedByDescending { it.value }
            val shown = outlays.filter { category == null || (it.category ?: "Otros") == category }
                .sortedByDescending { it.date }
            return OutlayMonth(
                title = "${monthShort(today)} ${today.year}",
                total = formatSoles(outlays.sumOf { it.amount }),
                count = "· ${outlays.size} ${if (outlays.size == 1) "egreso" else "egresos"}",
                breakdown = byCategory.take(BREAKDOWN_CATEGORIES).joinToString(" · ") { "${it.key} ${formatSoles(it.value)}" },
                categories = byCategory.map { it.key },
                rows = shown.map { outlay ->
                    val date = outlay.date.toInstant().atZone(LIMA).toLocalDate()
                    OutlayRow(
                        id = outlay.id,
                        description = outlay.description,
                        amount = formatSoles(outlay.amount),
                        detail = listOfNotNull(outlay.category, outlay.cost_center, outlay.responsibleName?.let(::shortName), dayMonth(date))
                            .filter { it.isNotBlank() }.joinToString(" · "),
                    )
                },
            )
        }
    }
}

data class OutlaysUiState(val loading: Boolean = false, val error: String? = null, val month: OutlayMonth? = null, val filter: String? = null)

object OutlayTestTags { const val NEW = "outlays_new" }
