package com.dscorp.ispadmin.presentation.ui.features.supportTicket.inbox

import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.FICHA_ZONE
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.dayMonth
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.toTitleCase
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail.relativeLabel
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail.sentenceCase
import java.time.Instant

enum class TicketTab(val label: String) { PENDING("Pendientes"), IN_PROGRESS("En curso"), CLOSED("Cerrados") }

data class TicketFilters(val highPriority: Boolean = false, val unassigned: Boolean = false, val category: String? = null, val place: String? = null)

data class TicketInboxRow(
    val id: Int,
    val priority: StatusBadge?,
    val number: String,
    val time: String,
    val title: String,
    val subtitle: String,
    val tag: StatusBadge?,
    val assignment: String,
    val assigned: Boolean,
)

/** Rows of a tab (a-tic-01): high priority first, then the newest; the chips narrow them. */
fun ticketRows(tickets: List<AssistanceTicketResponse>, filters: TicketFilters, now: Instant): List<TicketInboxRow> = tickets
    .filter { ticket ->
        (!filters.highPriority || ticket.priority.equals("Alta", ignoreCase = true)) &&
            (!filters.unassigned || ticket.assignedTo.isNullOrBlank()) &&
            (filters.category == null || sentenceCase(ticket.category) == filters.category) &&
            (filters.place == null || ticket.place?.toTitleCase() == filters.place)
    }
    .sortedWith(compareBy<AssistanceTicketResponse> { priorityRank(it.priority) }.thenByDescending { it.createdAt })
    .map { ticket ->
        val assignee = ticket.assignedTo?.takeIf { it.isNotBlank() }?.toTitleCase()
        val slot = (ticket.scheduledAt ?: ticket.assignedAt)?.toInstant()
        TicketInboxRow(
            id = ticket.id,
            priority = priorityBadge(ticket.priority),
            number = "#${ticket.id}",
            time = relativeLabel(ticket.createdAt.toInstant(), now),
            title = sentenceCase(ticket.category),
            subtitle = listOfNotNull(ticket.name.takeIf { it.isNotBlank() }?.toTitleCase(), ticket.place?.takeIf { it.isNotBlank() }?.toTitleCase())
                .joinToString(" · "),
            tag = if (ticket.isExternalCustomer) StatusBadge("No abonado", BadgeTone.INFO) else null,
            assignment = assignee?.let { name -> listOfNotNull(name, slot?.let { slotLabel(it, now) }).joinToString(" · ") } ?: "Sin asignar",
            assigned = assignee != null,
        )
    }

fun categoryOptions(tickets: List<AssistanceTicketResponse>): List<String> =
    tickets.map { sentenceCase(it.category) }.filter { it.isNotBlank() }.distinct().sorted()

fun placeOptions(tickets: List<AssistanceTicketResponse>): List<String> =
    tickets.mapNotNull { it.place?.takeIf { place -> place.isNotBlank() }?.toTitleCase() }.distinct().sorted()

fun inboxSummary(tab: TicketTab, count: Int): String = when (tab) {
    TicketTab.PENDING -> (if (count == 1) "1 pendiente" else "$count pendientes") + " · prioridad alta primero"
    TicketTab.IN_PROGRESS -> "$count en curso"
    TicketTab.CLOSED -> if (count == 1) "1 cerrado este mes" else "$count cerrados este mes"
}

/** Open tickets per tab from the Core counts; reopened ones are pending again. */
fun tabCounts(byStatus: Map<String, Long>): Map<TicketTab, Long> = mapOf(
    TicketTab.PENDING to (byStatus["PENDING"] ?: 0L) + (byStatus["REOPEN"] ?: 0L),
    TicketTab.IN_PROGRESS to (byStatus["ASSIGNED"] ?: 0L) + (byStatus["IN_PROGRESS"] ?: 0L),
)

internal fun priorityBadge(priority: String): StatusBadge? = when (priority.trim().lowercase()) {
    "alta" -> StatusBadge("▲ Alta", BadgeTone.ERROR)
    "media" -> StatusBadge("Media", BadgeTone.INFO)
    "baja" -> StatusBadge("Baja", BadgeTone.NEUTRAL)
    else -> null
}

private fun priorityRank(priority: String): Int = when (priority.trim().lowercase()) {
    "alta" -> 0
    "media" -> 1
    "baja" -> 2
    else -> 3
}

/** "hoy 10:30", "ayer 16:20", "jue 15, 09:00" (later this week or after), "9 oct, 12:16" (earlier). */
private fun slotLabel(at: Instant, now: Instant): String {
    val local = at.atZone(FICHA_ZONE)
    val day = local.toLocalDate()
    val today = now.atZone(FICHA_ZONE).toLocalDate()
    val time = "%02d:%02d".format(local.hour, local.minute)
    return when {
        day == today -> "hoy $time"
        day == today.minusDays(1) -> "ayer $time"
        day.isAfter(today) -> "${WEEKDAYS[day.dayOfWeek.value - 1]} ${day.dayOfMonth}, $time"
        else -> "${dayMonth(day)}, $time"
    }
}

private val WEEKDAYS = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")
