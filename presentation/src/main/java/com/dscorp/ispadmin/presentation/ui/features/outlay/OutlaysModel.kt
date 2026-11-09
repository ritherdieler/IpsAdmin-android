package com.dscorp.ispadmin.presentation.ui.features.outlay

import com.dscorp.ispadmin.data.model.OutlayDTO
import com.dscorp.ispadmin.domain.model.OutlayType
import com.dscorp.ispadmin.presentation.ui.features.home.dayMonth
import com.dscorp.ispadmin.presentation.ui.features.home.monthShort
import com.dscorp.ispadmin.presentation.ui.features.payment.cash.shortName
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.formatSoles
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val LIMA: ZoneId = ZoneId.of("America/Lima")
private const val BREAKDOWN_CATEGORIES = 3
private val HOUR_MINUTE: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

data class OutlayRow(
    val id: Int?,
    val description: String,
    val amount: String,
    val detail: String,
    val isFixed: Boolean,
    val category: String?,
)

private fun OutlayDTO.outlayType() = if (type == OutlayType.FIXED.name) OutlayType.FIXED else OutlayType.VARIABLE

/** Egresos (a-caj-08): the current month, its total split by category, newest first. */
data class OutlayMonth(
    val title: String,
    /** The month before [title], named for the comparison of fixed outlays. */
    val previousTitle: String,
    val total: String,
    val count: String,
    val breakdown: String,
    val categories: List<String>,
    val variableCount: Int,
    val fixedCount: Int,
    val rows: List<OutlayRow>,
) {
    companion object {
        fun from(outlays: List<OutlayDTO>, category: String?, today: LocalDate, type: OutlayType? = null): OutlayMonth {
            val byCategory = outlays.groupBy { it.category?.takeIf(String::isNotBlank) ?: "Otros" }
                .mapValues { (_, items) -> items.sumOf { it.amount } }
                .entries.sortedByDescending { it.value }
            val shown = outlays.filter { category == null || (it.category ?: "Otros") == category }
                .filter { type == null || it.outlayType() == type }
                .sortedByDescending { it.date }
            val previous = today.withDayOfMonth(1).minusMonths(1)
            return OutlayMonth(
                title = "${monthShort(today)} ${today.year}",
                previousTitle = "${monthShort(previous)} ${previous.year}",
                total = formatSoles(outlays.sumOf { it.amount }),
                count = "· ${outlays.size} ${if (outlays.size == 1) "egreso" else "egresos"}",
                breakdown = byCategory.take(BREAKDOWN_CATEGORIES).joinToString(" · ") { "${it.key} ${formatSoles(it.value)}" },
                categories = byCategory.map { it.key },
                variableCount = outlays.count { it.outlayType() == OutlayType.VARIABLE },
                fixedCount = outlays.count { it.outlayType() == OutlayType.FIXED },
                rows = shown.map { outlay ->
                    val date = outlay.date.toInstant().atZone(LIMA).toLocalDate()
                    val fixed = outlay.outlayType() == OutlayType.FIXED
                    OutlayRow(
                        id = outlay.id,
                        description = outlay.description,
                        amount = formatSoles(outlay.amount),
                        detail = listOfNotNull(
                            if (fixed) "Fijo" else "Variable",
                            outlay.category,
                            outlay.responsibleName?.let(::shortName),
                            dayMonth(date),
                        ).filter { it.isNotBlank() }.joinToString(" · "),
                        isFixed = fixed,
                        category = outlay.category,
                    )
                },
            )
        }
    }
}

/** What the detail of an outlay shows (a-caj-09), already formatted so it can travel as navigation arguments. */
data class OutlayDetail(
    val description: String,
    val amount: String,
    val isFixed: Boolean,
    val category: String,
    val date: String,
    val registeredAt: String,
    val documentCode: String,
    val responsible: String,
    val receiptUrls: List<String>,
) {
    companion object {
        fun from(outlay: OutlayDTO): OutlayDetail {
            val moment = outlay.date.toInstant().atZone(LIMA)
            val date = "${dayMonth(moment.toLocalDate())} ${moment.year}"
            return OutlayDetail(
                description = outlay.description,
                amount = formatSoles(outlay.amount),
                isFixed = outlay.outlayType() == OutlayType.FIXED,
                category = outlay.category?.takeIf(String::isNotBlank) ?: "Otros",
                date = date,
                registeredAt = "Registrado el $date, ${HOUR_MINUTE.format(moment)}",
                documentCode = outlay.document_code.orEmpty(),
                responsible = outlay.responsibleName.orEmpty(),
                receiptUrls = outlay.receipt_urls,
            )
        }
    }
}

data class OutlaysUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val month: OutlayMonth? = null,
    val filter: String? = null,
    val typeFilter: OutlayType? = null,
    /** Null on a Core without the summary endpoint: the screen then shows the plain month total. */
    val summary: OutlaySummary? = null,
    /** Only administrators and accountants register FIXED outlays, so only they get that action when there are none. */
    val canRegisterFixed: Boolean = false,
)

object OutlayTestTags {
    const val NEW = "outlays_new"
    const val EMPTY_ACTION = "outlays_empty_action"
}

/** Keys under which the form hands the result of a saved outlay back to the list. */
object OutlayResultKeys {
    const val SAVED_ID = "outlay_saved_id"
    const val TEAM_NOTIFIED = "outlay_team_notified"
}
