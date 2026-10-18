package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions

import com.dscorp.ispadmin.data.datasource.remote.CutDeferralResponse
import com.dscorp.ispadmin.domain.model.Payment
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.dscorp.ispadmin.presentation.ui.features.home.monthShort
import com.dscorp.ispadmin.presentation.ui.features.home.weekdayDayMonth
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.FICHA_ZONE
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.HubHeader
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.formatSoles
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.toTitleCase
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

data class DeferOption(val date: LocalDate, val title: String, val hint: String)

val DEFER_REASONS = listOf("Promesa de pago", "Viaje", "Reclamo en curso", "Otro")

private const val SOON_DAYS = 2L
private const val LATER_DAYS = 5L

/** a-sus-12 «Postergar corte»: the Core only accepts a date after today and inside the current month. */
data class DeferCut(
    val header: HubHeader,
    val summary: String,
    val currentNote: String?,
    val options: List<DeferOption>,
    /** First and last date the picker allows; null when the month has no day left. */
    val pickRange: Pair<LocalDate, LocalDate>?,
    private val restores: Boolean,
) {
    val defaultDate: LocalDate? get() = options.firstOrNull { it.hint == "en $LATER_DAYS días" }?.date ?: options.firstOrNull()?.date

    fun note(date: LocalDate): String =
        "Queda fuera del corte automático hasta el ${weekdayDayMonth(date)} inclusive." + if (restores) " Se restablece el servicio ahora." else ""

    fun action(date: LocalDate): String = "Postergar hasta el ${weekdayDayMonth(date)}"

    companion object {
        fun from(subscription: SubscriptionResponse, receipts: List<Payment>, current: CutDeferralResponse?, today: LocalDate): DeferCut {
            val monthEnd = YearMonth.from(today).atEndOfMonth()
            val first = subscription.firstName?.trim()?.split(Regex("\\s+"))?.firstOrNull()?.toTitleCase().orEmpty()
            val months = receipts.filter { !it.paid }.sortedBy { it.billingDate }
                .map { monthShort(Instant.ofEpochMilli(it.billingDate).atZone(FICHA_ZONE).toLocalDate()) }.distinct()
            val debt = if (subscription.totalDebt > 0) {
                "$first debe ${formatSoles(subscription.totalDebt)}" + (months.takeIf { it.isNotEmpty() }?.let { " (${it.joinToString(", ")})" } ?: "") + "."
            } else "$first no tiene deuda vencida."
            val cut = subscription.serviceStatus == ServiceStatus.CUT_OFF
            val consequence = if (cut) " Está cortada: al postergar se le restablece el servicio." else " Sin postergación entra en el próximo corte automático."
            val options = listOf(
                today.plusDays(SOON_DAYS) to "en $SOON_DAYS días",
                today.plusDays(LATER_DAYS) to "en $LATER_DAYS días",
                monthEnd to "fin de mes",
            ).filter { (date, _) -> date.isAfter(today) && !date.isAfter(monthEnd) }
                .groupBy({ it.first }, { it.second }).map { (date, hints) -> DeferOption(date, title(date), hints.last()) }
            return DeferCut(
                header = HubHeader.from(subscription),
                summary = debt + consequence,
                currentNote = current?.let { deferral ->
                    val until = runCatching { LocalDate.parse(deferral.untilDate) }.getOrNull() ?: return@let null
                    "Ya tiene postergación hasta el ${weekdayDayMonth(until)} (${deferral.reason}). Al guardar se reemplaza."
                },
                options = options,
                pickRange = today.plusDays(1).takeIf { !it.isAfter(monthEnd) }?.let { it to monthEnd },
                restores = cut,
            )
        }

        /** "Vie 16 oct". */
        private fun title(date: LocalDate) = weekdayDayMonth(date).replaceFirstChar { it.uppercase() }
    }
}
