package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha

import com.dscorp.ispadmin.domain.model.Payment
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.badge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.displayName
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.formatSoles
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.label
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.toTitleCase
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.planLabel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Business zone: billing months and due days are Lima calendar days. */
internal val FICHA_ZONE: ZoneId = ZoneId.of("America/Lima")
private val MONTHS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "set", "oct", "nov", "dic")
private const val MAX_PENDING_TO_RESTORE = 1

data class DebtCard(val amount: String, val detail: String?)
data class InfoRow(val label: String, val value: String)

/** Navy band of the ficha (a-sus-04): name, code line, state badges and the debt card. */
data class FichaHeader(val name: String, val subtitle: String, val badges: List<StatusBadge>, val debt: DebtCard?) {
    companion object {
        fun from(subscription: SubscriptionResponse, receipts: List<Payment>, today: LocalDate): FichaHeader {
            val pending = subscription.pendingInvoiceQuantity
            val hasDebt = pending > 0 && subscription.totalDebt > 0
            val status = subscription.cutSince()
                ?.let { StatusBadge("Cortado desde ${dayMonth(it)}", BadgeTone.WARNING) }
                ?: subscription.serviceStatus.badge()
            return FichaHeader(
                name = displayName(subscription.firstName, subscription.lastName),
                subtitle = listOfNotNull(
                    "Cód. ${subscription.id}",
                    subscription.installationType.label(),
                    subscription.place?.name?.toTitleCase(),
                ).joinToString(" · "),
                badges = listOfNotNull(
                    status,
                    if (hasDebt) StatusBadge(if (pending == 1) "Debe 1 mes" else "Debe $pending meses", BadgeTone.ERROR) else null,
                ),
                debt = if (hasDebt) {
                    DebtCard(formatSoles(subscription.totalDebt), debtDetail(receipts.filter { !it.paid }, today))
                } else null,
            )
        }
    }
}

enum class FichaAction(val label: String) {
    CALL("Llamar"), WHATSAPP("WhatsApp"), RESTORE("Restablecer"), TICKET("Ticket"), MAP("Mapa")
}

/** "ago y set 2026 · oct vence mañana": overdue months, then the next receipt to fall due. */
fun debtDetail(unpaid: List<Payment>, today: LocalDate): String? {
    val (overdue, upcoming) = unpaid.sortedBy { it.billingDate }.partition { receipt ->
        receipt.dueDay()?.isBefore(today) ?: true
    }
    val overdueText = overdue.map { it.billingMonth() }.takeIf { it.isNotEmpty() }?.let(::monthsLabel)
    val nextText = upcoming.minByOrNull { it.dueDay()!! }?.let { receipt ->
        val due = receipt.dueDay()!!
        val month = MONTHS[receipt.billingMonth().monthValue - 1]
        when (due) {
            today -> "$month vence hoy"
            today.plusDays(1) -> "$month vence mañana"
            else -> "$month vence el ${dayMonth(due)}"
        }
    }
    return listOfNotNull(overdueText, nextText).joinToString(" · ").ifEmpty { null }
}

fun restoreHint(status: ServiceStatus, pending: Int): String? {
    if (status != ServiceStatus.CUT_OFF || pending <= MAX_PENDING_TO_RESTORE) return null
    val toCharge = pending - MAX_PENDING_TO_RESTORE
    val months = if (toCharge == 1) "1 mes" else "$toCharge meses"
    return "Para restablecer debe quedar con menos de 2 recibos pendientes: cobra al menos $months."
}

fun canRestore(status: ServiceStatus, pending: Int): Boolean =
    status == ServiceStatus.CUT_OFF && pending <= MAX_PENDING_TO_RESTORE

fun serviceRows(subscription: SubscriptionResponse): List<InfoRow> = listOfNotNull(
    subscription.plan?.let { plan ->
        val speed = plan.downloadSpeed?.let { down -> " · $down/${plan.uploadSpeed ?: down} Mbps" }.orEmpty()
        InfoRow("Plan", planLabel(plan.name.orEmpty()) + speed)
    },
    subscription.price?.let { InfoRow("Mensualidad", formatSoles(it)) },
    InfoRow("Tipo", listOfNotNull(subscription.installationType.label(), accessLabel(subscription.accessMode)).joinToString(" · ")),
    subscription.subscriptionDate?.let { millis ->
        val day = Instant.ofEpochMilli(millis).atZone(FICHA_ZONE).toLocalDate()
        InfoRow("Instalada", "${dayMonth(day)} ${day.year}")
    },
    when (subscription.equipmentCondition) {
        "LOAN" -> InfoRow("Equipo", "En préstamo")
        "SOLD" -> InfoRow("Equipo", "Vendido")
        else -> null
    },
    subscription.technician
        ?.let { "${it.name} ${it.lastName}".toTitleCase() }
        ?.takeIf { it.isNotBlank() }
        ?.let { InfoRow("Técnico", it) },
)

fun holderRows(subscription: SubscriptionResponse): List<InfoRow> = listOfNotNull(
    subscription.dni?.takeIf { it.isNotBlank() }?.let { InfoRow("DNI", it) },
    subscription.phone?.takeIf { it.isNotBlank() }?.let { InfoRow("Teléfono", phoneLabel(it)) },
    subscription.address?.takeIf { it.isNotBlank() }?.let { InfoRow("Dirección", it) },
    subscription.place?.name?.takeIf { it.isNotBlank() }?.let { InfoRow("Lugar", it.toTitleCase()) },
)

fun networkRows(subscription: SubscriptionResponse): List<InfoRow> = listOfNotNull(
    subscription.napBox?.code?.takeIf { it.isNotBlank() }?.let { code ->
        InfoRow("Caja NAP", code + subscription.borneNumber?.takeIf { it.isNotBlank() }?.let { " · puerto $it" }.orEmpty())
    },
    subscription.ip?.takeIf { it.isNotBlank() }?.let { InfoRow("IP", it) },
)

fun quickActions(subscription: SubscriptionResponse): List<FichaAction> = buildList {
    if (!subscription.phone.isNullOrBlank()) addAll(listOf(FichaAction.CALL, FichaAction.WHATSAPP))
    if (subscription.serviceStatus == ServiceStatus.CUT_OFF) add(FichaAction.RESTORE)
    add(FichaAction.TICKET)
    val location = subscription.location
    if (location != null && (location.latitude != 0.0 || location.longitude != 0.0)) add(FichaAction.MAP)
}

internal fun SubscriptionResponse.cutSince(): LocalDate? =
    if (serviceStatus == ServiceStatus.CUT_OFF) lastCutOffDate?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } else null

internal fun Payment.dueDay(): LocalDate? = dueDate?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

internal fun Payment.billingMonth(): YearMonth = YearMonth.from(Instant.ofEpochMilli(billingDate).atZone(FICHA_ZONE))

internal fun dayMonth(day: LocalDate): String = "${day.dayOfMonth} ${MONTHS[day.monthValue - 1]}"

internal fun monthName(month: YearMonth): String = MONTHS[month.monthValue - 1]

private fun monthsLabel(months: List<YearMonth>): String {
    val distinct = months.distinct().sorted()
    val sameYear = distinct.map { it.year }.distinct().size == 1
    val parts = if (sameYear) distinct.map(::monthName) else distinct.map { "${monthName(it)} ${it.year}" }
    val joined = if (parts.size == 1) parts.first() else parts.dropLast(1).joinToString(", ") + " y " + parts.last()
    return if (sameYear) "$joined ${distinct.first().year}" else joined
}

private fun accessLabel(accessMode: String?): String? = when {
    accessMode == null -> null
    accessMode.startsWith("PPPOE") -> "PPPoE"
    accessMode == "STATIC_IP" -> "IP fija"
    else -> null
}

private fun phoneLabel(phone: String): String =
    if (phone.length == 9 && phone.all(Char::isDigit)) phone.chunked(3).joinToString(" ") else phone

enum class FichaTab(val label: String) {
    RESUMEN("Resumen"), PAGOS("Pagos"), RED("Red"), TICKETS("Tickets"), HISTORIAL("Historial")
}

data class PendingCard(val amount: String, val detail: String?, val chargeLabel: String)
data class ReceiptRow(
    val id: Int?,
    val title: String,
    val detail: String,
    val badge: StatusBadge,
    val amount: String,
    val overdue: Boolean,
)

/** «Pagos» tab (a-sus-05): pending card, last payment and the receipts newest first. */
data class FichaPayments(
    val pending: PendingCard?,
    val lastPayment: String?,
    val dueRule: String?,
    val receipts: List<ReceiptRow>,
) {
    companion object {
        fun from(receipts: List<Payment>, totalDebt: Double, today: LocalDate): FichaPayments {
            val unpaid = receipts.filter { !it.paid }
            val overdue = unpaid.filter { it.dueDay()?.isBefore(today) ?: true }.sortedBy { it.billingDate }
            val pending = if (totalDebt > 0 && unpaid.isNotEmpty()) {
                val amount = formatSoles(totalDebt)
                val detail = if (overdue.isNotEmpty()) {
                    val count = if (overdue.size == 1) "1 recibo vencido" else "${overdue.size} recibos vencidos"
                    "$count · ${debtDetail(overdue, today)}"
                } else debtDetail(unpaid, today)
                PendingCard(amount, detail, "Cobrar $amount")
            } else null
            val lastPaid = receipts.filter { it.paid && it.paymentDate > 0 }.maxByOrNull { it.paymentDate }
            val dueDays = receipts.mapNotNull { it.dueDay()?.dayOfMonth }.distinct()
            return FichaPayments(
                pending = pending,
                lastPayment = lastPaid?.let { paid ->
                    listOfNotNull(formatSoles(paid.paidAmount()), dayMonth(paid.paidDay()), paid.method?.takeIf { it.isNotBlank() })
                        .joinToString(" · ")
                },
                dueRule = dueDays.singleOrNull()?.let { "Vencen el $it de cada mes" },
                receipts = receipts.sortedByDescending { it.billingDate }.map { it.toRow(today) },
            )
        }

        private fun Payment.toRow(today: LocalDate): ReceiptRow {
            val month = billingMonth()
            val title = "${monthName(month).replaceFirstChar { it.uppercase() }} ${month.year}"
            val due = dueDay()
            return when {
                paid -> ReceiptRow(
                    id, title,
                    listOfNotNull(
                        "Pagado ${dayMonth(paidDay())}",
                        method?.takeIf { it.isNotBlank() },
                        discountAmount?.takeIf { it > 0 }?.let { "desc. ${formatSoles(it)}" },
                    ).joinToString(" · "),
                    StatusBadge("Pagado", BadgeTone.SUCCESS), formatSoles(paidAmount()), overdue = false,
                )
                due == null || due.isBefore(today) -> ReceiptRow(
                    id, title, due?.let { "Venció ${dayMonth(it)}" } ?: "Vencido",
                    StatusBadge("Vencido", BadgeTone.ERROR), formatSoles(amountToPay), overdue = true,
                )
                else -> ReceiptRow(
                    id, title,
                    when (due) {
                        today -> "Vence hoy ${dayMonth(due)}"
                        today.plusDays(1) -> "Vence mañana ${dayMonth(due)}"
                        else -> "Vence el ${dayMonth(due)}"
                    },
                    StatusBadge("Pendiente", BadgeTone.WARNING), formatSoles(amountToPay), overdue = false,
                )
            }
        }

        private fun Payment.paidAmount(): Double =
            amountPaid?.takeIf { it > 0 } ?: (amountToPay - (discountAmount ?: 0.0))

        private fun Payment.paidDay(): LocalDate = Instant.ofEpochMilli(paymentDate).atZone(FICHA_ZONE).toLocalDate()
    }
}
