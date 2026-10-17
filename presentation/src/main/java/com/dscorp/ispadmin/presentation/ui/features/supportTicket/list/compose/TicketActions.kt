package com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose

import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.model.User.UserType
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

enum class TicketAction(val label: String) { TAKE("Tomar"), CLOSE("Cerrar"), RESCHEDULE("Reagendar"), CANCEL("Cancelar") }

/** One primary action per ticket state; the secondary ones live in the card's menu. */
data class TicketActions(val primary: TicketAction?, val overflow: List<TicketAction>)

private val MONTHS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "set", "oct", "nov", "dic")

fun ticketActions(ticket: AssistanceTicketResponse, user: User): TicketActions {
    val type = user.type
    // A reopened ticket is handled like a new one.
    val status = ticket.status.takeUnless { it == AssistanceTicketStatus.REOPEN } ?: AssistanceTicketStatus.PENDING
    val canTake = status == AssistanceTicketStatus.PENDING && type in setOf(UserType.TECHNICIAN, UserType.ADMIN)
    val canClose = status == AssistanceTicketStatus.ASSIGNED && type in setOf(UserType.TECHNICIAN, UserType.ADMIN)
    val canCancel = status == AssistanceTicketStatus.PENDING && type in setOf(UserType.SECRETARY, UserType.ADMIN)
    val canReschedule = type in setOf(UserType.SECRETARY, UserType.ADMIN) &&
        status in setOf(AssistanceTicketStatus.PENDING, AssistanceTicketStatus.ASSIGNED, AssistanceTicketStatus.IN_PROGRESS)
    return TicketActions(
        primary = when {
            canTake -> TicketAction.TAKE
            canClose -> TicketAction.CLOSE
            else -> null
        },
        overflow = listOfNotNull(
            TicketAction.RESCHEDULE.takeIf { canReschedule },
            TicketAction.CANCEL.takeIf { canCancel },
        ),
    )
}

/** «9 oct 2026 · 12:16», independent of the device language. */
fun ticketDateLabel(date: Date, zone: TimeZone = TimeZone.getDefault()): String {
    val calendar = Calendar.getInstance(zone).apply { time = date }
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val month = MONTHS[calendar.get(Calendar.MONTH)]
    val time = "%02d:%02d".format(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
    return "$day $month ${calendar.get(Calendar.YEAR)} · $time"
}
