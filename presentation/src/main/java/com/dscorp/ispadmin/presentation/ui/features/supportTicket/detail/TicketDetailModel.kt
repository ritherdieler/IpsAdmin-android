package com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail

import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.TicketRow
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.FICHA_ZONE
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.FichaTickets
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.dayMonth
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.phoneLabel
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.ticketBadge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.badge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.displayName
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.formatSoles
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.toTitleCase
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.planLabel
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose.TicketAction
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose.ticketActions
import java.time.Duration
import java.time.Instant

data class DebtNotice(val text: String, val chargeLabel: String)
data class SubscriberCard(val initials: String, val name: String, val subtitle: String, val status: StatusBadge, val debt: String, val owes: Boolean)

data class TicketDetail(
    val id: Int,
    val title: String,
    val badges: List<StatusBadge>,
    val category: String,
    val meta: String,
    val debtNotice: DebtNotice?,
    val subscriber: SubscriberCard?,
    val description: String,
    val contact: String?,
    val assignment: String,
    val previous: List<TicketRow>,
    val primary: TicketAction?,
    val primaryHint: String?,
    val canAssign: Boolean,
    val canReschedule: Boolean,
    val canCancel: Boolean,
    /** Raw contact number for the call and WhatsApp buttons. */
    val phone: String? = null,
) {
    companion object {
        fun from(ticket: AssistanceTicketResponse, subscription: SubscriptionResponse?, previous: List<AssistanceTicketResponse>, user: User, now: Instant): TicketDetail {
            val actions = ticketActions(ticket, user)
            val taken = ticket.status in TAKEN_STATES
            val assignee = ticket.assignedTo?.takeIf { it.isNotBlank() }?.toTitleCase()
            val assignedAt = ticket.assignedAt?.toInstant()
            val meta = if (taken && assignedAt != null) {
                listOfNotNull(assignee?.let { "Tomado por $it" } ?: "Tomado", "a las ${hourMinute(assignedAt)} · ${elapsedLabel(assignedAt, now)}")
                    .joinToString(" ")
            } else "Creado ${relativeLabel(ticket.createdAt.toInstant(), now)}"
            val owes = subscription != null && subscription.totalDebt > 0
            return TicketDetail(
                id = ticket.id,
                title = "Ticket #${ticket.id}",
                badges = listOfNotNull(
                    ticket.status.ticketBadge(),
                    priorityBadge(ticket.priority),
                    if (ticket.isExternalCustomer) StatusBadge("No abonado", BadgeTone.INFO) else null,
                ),
                category = sentenceCase(ticket.category),
                meta = meta,
                debtNotice = subscription?.takeIf { it.serviceStatus == ServiceStatus.CUT_OFF && owes }?.let { cut ->
                    val firstName = cut.firstName.orEmpty().trim().split(Regex("\\s+")).first().toTitleCase()
                    val amount = formatSoles(cut.totalDebt)
                    DebtNotice("$firstName está cortado por deuda: debe $amount. Cobrar restablece el servicio y podría resolver el ticket.", "Cobrar $amount")
                },
                subscriber = subscription?.let { sub ->
                    val name = displayName(sub.firstName, sub.lastName)
                    SubscriberCard(
                        initials = name.split(' ').filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() },
                        name = name,
                        subtitle = listOfNotNull(sub.plan?.name?.takeIf { it.isNotBlank() }?.let(::planLabel), sub.place?.name?.toTitleCase())
                            .joinToString(" · "),
                        status = sub.serviceStatus.badge(),
                        debt = if (owes) formatSoles(sub.totalDebt) else "Al día",
                        owes = owes,
                    )
                },
                description = ticket.description,
                contact = ticket.phone.takeIf { it.isNotBlank() }?.let { phone ->
                    "Contacto: ${phoneLabel(phone)}" + if (subscription?.phone == phone) " · titular" else ""
                },
                assignment = assignee?.let { name -> listOfNotNull(name, assignedAt?.let { dayTime(it, now) }).joinToString(" · ") } ?: "Sin asignar",
                previous = FichaTickets.from(previous.filter { it.id != ticket.id }).rows,
                primary = actions.primary,
                primaryHint = if (actions.primary == TicketAction.TAKE) "Pasa a En curso y queda a tu nombre." else null,
                canAssign = !taken && ticket.status in OPEN_STATES && user.type in ASSIGNERS,
                canReschedule = TicketAction.RESCHEDULE in actions.overflow,
                canCancel = TicketAction.CANCEL in actions.overflow,
                phone = ticket.phone.takeIf { it.isNotBlank() },
            )
        }

        private val TAKEN_STATES = setOf(AssistanceTicketStatus.ASSIGNED, AssistanceTicketStatus.IN_PROGRESS)
        private val OPEN_STATES = setOf(AssistanceTicketStatus.PENDING, AssistanceTicketStatus.REOPEN)
        private val ASSIGNERS = setOf(User.UserType.ADMIN, User.UserType.SECRETARY)

        private fun priorityBadge(priority: String): StatusBadge? = when (priority.trim().lowercase()) {
            "alta" -> StatusBadge("▲ Alta", BadgeTone.ERROR)
            "media" -> StatusBadge("Media", BadgeTone.INFO)
            "baja" -> StatusBadge("Baja", BadgeTone.NEUTRAL)
            else -> null
        }
    }
}

/** "Sin Conexión a Internet" → "Sin conexión a internet"; short all-caps words such as TV or ONU stay. */
internal fun sentenceCase(text: String): String = text.trim().split(' ').filter { it.isNotEmpty() }.mapIndexed { index, word ->
    when {
        word.length <= 3 && word.all { it.isUpperCase() } && word.length > 1 -> word
        index == 0 -> word.lowercase().replaceFirstChar { it.uppercase() }
        else -> word.lowercase()
    }
}.joinToString(" ")

fun relativeLabel(at: Instant, now: Instant): String {
    val minutes = Duration.between(at, now).toMinutes()
    val day = at.atZone(FICHA_ZONE).toLocalDate()
    val today = now.atZone(FICHA_ZONE).toLocalDate()
    return when {
        minutes < 1 -> "hace un momento"
        minutes < 60 -> "hace $minutes min"
        day == today -> "hace ${minutes / 60} h"
        day == today.minusDays(1) -> "ayer ${hourMinute(at)}"
        else -> "${dayMonth(day)} ${hourMinute(at)}"
    }
}

fun elapsedLabel(since: Instant, now: Instant): String {
    val minutes = Duration.between(since, now).toMinutes().coerceAtLeast(0)
    return if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"
}

private fun dayTime(at: Instant, now: Instant): String {
    val day = at.atZone(FICHA_ZONE).toLocalDate()
    val today = now.atZone(FICHA_ZONE).toLocalDate()
    val prefix = when (day) {
        today -> "hoy"
        today.minusDays(1) -> "ayer"
        else -> dayMonth(day)
    }
    return "$prefix ${hourMinute(at)}"
}

private fun hourMinute(at: Instant): String = at.atZone(FICHA_ZONE).let { "%02d:%02d".format(it.hour, it.minute) }
