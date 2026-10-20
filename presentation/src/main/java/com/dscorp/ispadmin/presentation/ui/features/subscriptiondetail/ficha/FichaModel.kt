package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha

import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.Payment
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.badge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.displayName
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.formatSoles
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.label
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.owedDebt
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
data class InfoRow(val label: String, val value: String, val note: String? = null, val badge: StatusBadge? = null)

/** Navy band of the ficha (a-sus-04): name, code line, state badges and the debt card. */
data class FichaHeader(
    val name: String,
    val subtitle: String,
    val badges: List<StatusBadge>,
    val debt: DebtCard?,
    /** Unpaid receipts; decides whether «Restablecer» is allowed. */
    val pending: Int = 0,
) {
    companion object {
        fun from(subscription: SubscriptionResponse, receipts: List<Payment>, today: LocalDate): FichaHeader {
            val owed = owedDebt(subscription, receipts)
            val pending = owed.pending
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
                    if (owed.hasDebt) StatusBadge(if (pending == 1) "Debe 1 mes" else "Debe $pending meses", BadgeTone.ERROR) else null,
                ),
                debt = if (owed.hasDebt) {
                    DebtCard(formatSoles(owed.amount), debtDetail(receipts.filter { !it.paid }, today))
                } else null,
                pending = pending,
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

fun restoreHint(cut: Boolean, pending: Int): String? {
    if (!cut || pending <= MAX_PENDING_TO_RESTORE) return null
    val toCharge = pending - MAX_PENDING_TO_RESTORE
    val months = if (toCharge == 1) "1 mes" else "$toCharge meses"
    return "Para restablecer debe quedar con menos de 2 recibos pendientes: cobra al menos $months."
}

fun canRestore(cut: Boolean, pending: Int): Boolean =
    cut && pending <= MAX_PENDING_TO_RESTORE

/**
 * «Restablecer» applies: CUT_OFF on the new Core; the current production Core keeps cut services ACTIVE and only
 * raises isServiceCutOff. The flag is never a «Cortado» badge: that Core never clears it.
 */
internal fun SubscriptionResponse.offersRestore(): Boolean = serviceStatus == ServiceStatus.CUT_OFF || isServiceCutOff

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
    if (subscription.offersRestore()) add(FichaAction.RESTORE)
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

internal fun phoneLabel(phone: String): String =
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
            // The current production Core sends a zero total; then the unpaid receipts add it up.
            val owed = totalDebt.takeIf { it > 0 } ?: unpaid.sumOf { it.amountToPay }
            val pending = if (owed > 0 && unpaid.isNotEmpty()) {
                val amount = formatSoles(owed)
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

data class CpeReading(val online: Boolean, val rxDbm: Double?, val lastInform: Instant?, val model: String?)

data class FichaNetwork(
    val cutNotice: String?,
    val equipment: List<InfoRow>,
    val equipmentUnavailable: Boolean,
    val access: List<InfoRow>,
    val fiber: List<InfoRow>,
    val wifi: List<InfoRow>,
) {
    companion object {
        fun from(subscription: SubscriptionResponse, cpe: CpeReading?, cpeFailed: Boolean, mufaName: String?, now: Instant): FichaNetwork {
            val isFiber = subscription.installationType != InstallationType.WIRELESS
            val equipment = if (!isFiber) emptyList() else listOfNotNull(
                subscription.fiberOnuSn?.takeIf { it.isNotBlank() }?.let { InfoRow("Serie", it) },
                cpe?.model?.takeIf { it.isNotBlank() }?.let { InfoRow("Modelo", it) },
                cpe?.let {
                    InfoRow(
                        "Estado", "",
                        badge = if (it.online) StatusBadge("En línea", BadgeTone.SUCCESS) else StatusBadge("Sin conexión", BadgeTone.ERROR),
                    )
                },
                cpe?.rxDbm?.let { InfoRow("Potencia", powerLabel(it)) },
                cpe?.lastInform?.let { InfoRow("Último reporte", ageLabel(it, now)) },
            )
            val nap = subscription.napBox
            val fiber = if (!isFiber || nap == null) emptyList() else listOfNotNull(
                nap.code.takeIf { it.isNotBlank() }?.let { code ->
                    InfoRow(
                        "Caja NAP",
                        code + subscription.borneNumber?.takeIf { it.isNotBlank() }?.let { " · puerto $it" }.orEmpty(),
                        note = nap.address.takeIf { it.isNotBlank() },
                    )
                },
                mufaName?.takeIf { it.isNotBlank() }?.let { InfoRow("Mufa", it) },
                if (nap.oltBoard != null && nap.oltPort != null) InfoRow("OLT", "tarjeta ${nap.oltBoard} · puerto ${nap.oltPort}") else null,
            )
            return FichaNetwork(
                cutNotice = if (subscription.serviceStatus == ServiceStatus.CUT_OFF) {
                    val since = subscription.cutSince()?.let { " desde ${dayMonth(it)}" }.orEmpty()
                    "Acceso cortado por deuda$since (lista de deudores del MikroTik). Se restablece al cobrar."
                } else null,
                equipment = equipment,
                equipmentUnavailable = isFiber && cpe == null && cpeFailed,
                access = listOfNotNull(
                    accessLabel(subscription.accessMode)?.let { InfoRow("Modo", it) },
                    subscription.pppoeUsername?.takeIf { it.isNotBlank() && subscription.accessMode?.startsWith("PPPOE") == true }
                        ?.let { InfoRow("Usuario", it) },
                    subscription.ip?.takeIf { it.isNotBlank() }?.let { InfoRow("IP", it) },
                    subscription.hostDevice?.name?.takeIf { it.isNotBlank() }?.let { InfoRow("Router núcleo", it) },
                ),
                fiber = fiber,
                wifi = listOfNotNull(
                    subscription.wifiSsid24?.takeIf { it.isNotBlank() }?.let { InfoRow("2.4 GHz", it) },
                    subscription.wifiSsid5?.takeIf { it.isNotBlank() }?.let { InfoRow("5 GHz", it) },
                ),
            )
        }
    }
}

/** GPON receive power as the design reads it: −24 dBm or better is good, down to −27 acceptable, below that low. */
fun powerLabel(rxDbm: Double): String {
    val quality = when {
        rxDbm >= -24.0 -> "buena"
        rxDbm >= -27.0 -> "aceptable"
        else -> "baja"
    }
    val value = String.format(java.util.Locale.US, "%.1f", rxDbm).replace("-", "−")
    return "$value dBm · $quality"
}

fun ageLabel(at: Instant, now: Instant): String {
    val minutes = java.time.Duration.between(at, now).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> "hace un momento"
        minutes < 60 -> "hace $minutes min"
        minutes < 24 * 60 -> "hace ${minutes / 60} h"
        else -> "hace ${minutes / (24 * 60)} d"
    }
}

fun mufaLabel(mufa: com.dscorp.ispadmin.domain.model.Mufa): String =
    mufa.reference.orEmpty() + (mufa.threads?.let { " ($it hilos)" }.orEmpty())

data class TicketRow(val id: Int, val title: String, val detail: String, val badge: StatusBadge)

/** «Tickets» tab: the subscription's tickets newest first; open ones label the tab. */
data class FichaTickets(val openCount: Int, val rows: List<TicketRow>) {
    companion object {
        fun from(tickets: List<AssistanceTicketResponse>): FichaTickets = FichaTickets(
            openCount = tickets.count { it.status in OPEN_TICKET_STATES },
            rows = tickets.sortedByDescending { it.createdAt }.map { ticket ->
                val created = ticket.createdAt.toInstant().atZone(FICHA_ZONE).toLocalDate()
                TicketRow(
                    id = ticket.id,
                    title = ticket.category,
                    detail = listOfNotNull("#${ticket.id}", "${dayMonth(created)} ${created.year}", ticket.assignedTo?.takeIf { it.isNotBlank() }?.toTitleCase())
                        .joinToString(" · "),
                    badge = ticket.status.ticketBadge(),
                )
            },
        )

        private val OPEN_TICKET_STATES = setOf(
            AssistanceTicketStatus.PENDING, AssistanceTicketStatus.ASSIGNED, AssistanceTicketStatus.IN_PROGRESS, AssistanceTicketStatus.REOPEN,
        )
    }
}

enum class MoreAction(val label: String) {
    CHANGE_PLAN("Cambiar plan"),
    CHANGE_NAP("Cambiar caja NAP"),
    MIGRATE_TO_FIBER("Migrar a fibra"),
    UPDATE_LOCATION("Actualizar ubicación"),
    NEW_SERVICE("Nuevo servicio para el titular"),
    DEFER_CUT("Postergar corte"),
    EDIT_HOLDER("Editar datos del titular"),
    UPDATE_PHOTO("Actualizar foto de fachada"),
    CANCEL("Cancelar suscripción"),
    REACTIVATE("Reactivar servicio"),
}

/** A titled group of the «Más acciones» sheet (a-sus-08); the last, untitled one holds the destructive action. */
data class MoreSection(val title: String?, val actions: List<MoreAction>)

fun moreActions(subscription: SubscriptionResponse): List<MoreSection> {
    val cancelled = subscription.serviceStatus == ServiceStatus.CANCELLED
    val wireless = subscription.installationType == InstallationType.WIRELESS
    val service = buildList {
        if (!cancelled) add(MoreAction.CHANGE_PLAN)
        if (!cancelled) add(if (wireless) MoreAction.MIGRATE_TO_FIBER else MoreAction.CHANGE_NAP)
        add(MoreAction.UPDATE_LOCATION)
        if (!cancelled && subscription.customerId != null) add(MoreAction.NEW_SERVICE)
    }
    return listOfNotNull(
        MoreSection("Servicio", service),
        if (!cancelled) MoreSection("Cobranza", listOf(MoreAction.DEFER_CUT)) else null,
        MoreSection("Titular", listOf(MoreAction.EDIT_HOLDER, MoreAction.UPDATE_PHOTO)),
        MoreSection(null, listOf(if (cancelled) MoreAction.REACTIVATE else MoreAction.CANCEL)),
    )
}

/** "Cód. 1842 · Basico · La Villa" under the holder in the «Más acciones» sheet. */
fun moreActionsSubtitle(subscription: SubscriptionResponse): String = listOfNotNull(
    "Cód. ${subscription.id}",
    subscription.plan?.name?.takeIf { it.isNotBlank() }?.let(::planLabel),
    subscription.place?.name?.takeIf { it.isNotBlank() }?.toTitleCase(),
).joinToString(" · ")

/** One subscription_log row from the Core (GET subscription/{id}/history). */
data class HistoryLog(
    val date: Long,
    val actionType: String,
    val planName: String? = null,
    val planPrice: Double? = null,
    val previousPlanPrice: Double? = null,
    val responsibleName: String? = null,
    /** Ready-to-show line from the Core, e.g. the cancellation reason or the NAP move. */
    val detail: String? = null,
)

enum class HistoryKind { PAYMENT, TICKET, NETWORK, CHANGE }

enum class HistoryFilter(val label: String) {
    ALL("Todo"), PAYMENTS("Pagos"), TICKETS("Tickets"), NETWORK("Red"), CHANGES("Cambios")
}

data class HistoryItem(val day: String, val title: String, val detail: String?, val actor: String?, val kind: HistoryKind)
data class HistoryMonth(val title: String, val items: List<HistoryItem>)

/** «Historial» tab (a-sus-07): service changes, payments and tickets in one timeline. */
class FichaHistory private constructor(private val events: List<Pair<Long, HistoryItem>>) {
    fun months(filter: HistoryFilter): List<HistoryMonth> = events
        .filter { (_, item) -> filter.matches(item.kind) }
        .sortedByDescending { it.first }
        .groupBy { (at, _) -> YearMonth.from(Instant.ofEpochMilli(at).atZone(FICHA_ZONE)) }
        .map { (month, items) ->
            HistoryMonth("${monthName(month).replaceFirstChar { it.uppercase() }} ${month.year}", items.map { it.second })
        }

    companion object {
        fun from(
            logs: List<HistoryLog>,
            receipts: List<Payment>,
            tickets: List<AssistanceTicketResponse>,
        ): FichaHistory {
            val changes = logs.map { log -> log.date to log.toItem() }
            val payments = receipts.filter { it.paid && it.paymentDate > 0 }.map { paid ->
                val amount = paid.amountPaid?.takeIf { it > 0 } ?: paid.amountToPay
                val month = paid.billingMonth()
                paid.paymentDate to HistoryItem(
                    day = dayOf(paid.paymentDate),
                    title = listOfNotNull("Pago ${formatSoles(amount)}", paid.method?.takeIf { it.isNotBlank() }).joinToString(" · "),
                    detail = "Recibo de ${monthName(month)} ${month.year}",
                    actor = paid.responsibleName?.takeIf { it.isNotBlank() }?.toTitleCase(),
                    kind = HistoryKind.PAYMENT,
                )
            }
            val ticketEvents = tickets.map { ticket ->
                val at = (ticket.resolvedAt ?: ticket.createdAt).time
                val opened = Instant.ofEpochMilli(ticket.createdAt.time).atZone(FICHA_ZONE).toLocalDate()
                at to HistoryItem(
                    day = dayOf(at),
                    title = "Ticket #${ticket.id} ${ticket.status.historyVerb()} · ${ticket.category}",
                    detail = "Prioridad ${ticket.priority} · abierto el ${dayMonth(opened)}",
                    actor = ticket.assignedTo?.takeIf { it.isNotBlank() }?.toTitleCase(),
                    kind = HistoryKind.TICKET,
                )
            }
            return FichaHistory(changes + payments + ticketEvents)
        }

        private fun dayOf(millis: Long): String = dayMonth(Instant.ofEpochMilli(millis).atZone(FICHA_ZONE).toLocalDate())

        private fun HistoryFilter.matches(kind: HistoryKind): Boolean = when (this) {
            HistoryFilter.ALL -> true
            HistoryFilter.PAYMENTS -> kind == HistoryKind.PAYMENT
            HistoryFilter.TICKETS -> kind == HistoryKind.TICKET
            HistoryFilter.NETWORK -> kind == HistoryKind.NETWORK
            HistoryFilter.CHANGES -> kind == HistoryKind.CHANGE
        }

        private fun AssistanceTicketStatus.historyVerb(): String = when (this) {
            AssistanceTicketStatus.PENDING -> "abierto"
            AssistanceTicketStatus.ASSIGNED, AssistanceTicketStatus.IN_PROGRESS -> "en curso"
            AssistanceTicketStatus.RESOLVED, AssistanceTicketStatus.CLOSED -> "cerrado"
            AssistanceTicketStatus.CANCELLED -> "cancelado"
            AssistanceTicketStatus.REOPEN -> "reabierto"
        }

        private fun HistoryLog.toItem(): HistoryItem {
            val plan = planName?.takeIf { it.isNotBlank() }?.let(::planLabel)
            val (title, kind) = when (actionType) {
                "CUT_FOR_DEBT" -> "Corte automático por deuda" to HistoryKind.NETWORK
                "RESTORED_AFTER_PAYMENT" -> "Restablecido tras el pago" to HistoryKind.NETWORK
                "RESTORE_INTERNET_CONNECTION" -> "Servicio restablecido" to HistoryKind.NETWORK
                "REBOOT_FIBER_ONU" -> "Reinicio de ONU" to HistoryKind.NETWORK
                "CHANGE_NAP_BOX" -> "Cambio de caja NAP" to HistoryKind.NETWORK
                "CHANGE_PLAN" -> (plan?.let { "Cambio de plan a $it" } ?: "Cambio de plan") to HistoryKind.CHANGE
                "NEW_SUBSCRIPTION" -> listOfNotNull("Alta del servicio", plan).joinToString(" · ") to HistoryKind.CHANGE
                "MIGRATE_SUBSCRIPTION" -> "Migración a fibra" to HistoryKind.CHANGE
                "UPDATE_LOCATION" -> "Ubicación actualizada" to HistoryKind.CHANGE
                "UPDATE_SUBSCRIPTION" -> "Datos actualizados" to HistoryKind.CHANGE
                "CUT_DEFERRED" -> "Corte postergado" to HistoryKind.CHANGE
                "RECONNECT_CANCELLED_SUBSCRIPTION" -> "Servicio reactivado" to HistoryKind.CHANGE
                "CANCEL_SUBSCRIPTION", "CANCELED_BY_STORED_PROCEDURE", "TOTAL_CANCEL_SUBSCRIPTION" ->
                    "Suscripción cancelada" to HistoryKind.CHANGE
                else -> actionType.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() } to HistoryKind.CHANGE
            }
            val detail = detail?.takeIf { it.isNotBlank() }
                ?: if (actionType == "CHANGE_PLAN" && planPrice != null && previousPlanPrice != null) {
                    "${formatSoles(previousPlanPrice)} → ${formatSoles(planPrice)} al mes"
                } else null
            return HistoryItem(dayOf(date), title, detail, responsibleName?.takeIf { it.isNotBlank() } ?: "Sistema", kind)
        }
    }
}

/** Ticket state as the app shows it everywhere (ficha, ticket list and detail). */
fun AssistanceTicketStatus.ticketBadge(): StatusBadge = when (this) {
    AssistanceTicketStatus.PENDING -> StatusBadge("Pendiente", BadgeTone.WARNING)
    AssistanceTicketStatus.REOPEN -> StatusBadge("Reabierto", BadgeTone.WARNING)
    AssistanceTicketStatus.ASSIGNED, AssistanceTicketStatus.IN_PROGRESS -> StatusBadge("En curso", BadgeTone.INFO)
    AssistanceTicketStatus.RESOLVED, AssistanceTicketStatus.CLOSED -> StatusBadge("Cerrado", BadgeTone.NEUTRAL)
    AssistanceTicketStatus.CANCELLED -> StatusBadge("Cancelado", BadgeTone.NEUTRAL)
}
