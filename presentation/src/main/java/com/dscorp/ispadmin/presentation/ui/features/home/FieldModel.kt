package com.dscorp.ispadmin.presentation.ui.features.home

import com.dscorp.ispadmin.data.model.InstallationOrderDto
import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.data.model.InstallationOrderStatus
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.FICHA_ZONE
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.toTitleCase
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.create.categoryLabel
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail.relativeLabel
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.inbox.priorityBadge
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date

sealed interface TodayTarget {
    data class Ticket(val id: Int) : TodayTarget
    data class Order(val id: Int) : TodayTarget
}

data class UploadBanner(val title: String, val detail: String, val allSent: Boolean)
data class NowJob(
    val target: TodayTarget, val label: String, val priority: StatusBadge?, val time: String?, val title: String,
    val name: String, val address: String, val registers: Boolean, val mapsQuery: String,
)
data class TodayRow(
    val target: TodayTarget, val number: String, val priority: StatusBadge?, val trailing: String?, val trailingBadge: StatusBadge?,
    val title: String, val subtitle: String,
)

/** Technician home «Hoy» (a-ini-03): my jobs today, the open tickets and what I closed. */
data class TechnicianToday(val subtitle: String, val uploads: UploadBanner, val now: NowJob?, val pending: List<TodayRow>, val done: List<TodayRow>) {
    companion object {
        fun from(
            user: User, open: List<AssistanceTicketResponse>, closed: List<AssistanceTicketResponse>,
            orders: List<InstallationOrderDto>, pendingUploads: Int, now: Instant,
        ): TechnicianToday {
            val nowLocal = LocalDateTime.ofInstant(now, FICHA_ZONE)
            val today = nowLocal.toLocalDate()
            val fullName = listOfNotNull(user.name, user.lastName).joinToString(" ").trim()
            fun mine(ticket: AssistanceTicketResponse) = ticket.assignedTo?.trim().equals(fullName, ignoreCase = true)

            val ticketJobs = open.filter { mine(it) && it.status in WORKING && it.plannedAt().toLocalDate() == today }
            val orderJobs = orders.filter {
                it.technician?.id == user.id && it.status in OPEN_ORDERS && it.scheduledDate?.toLocalDate() == today
            }
            val jobs = (ticketJobs.map { Job(it.plannedAt(), ticket = it) } + orderJobs.map { Job(it.scheduledDate!!, order = it) })
                .sortedBy { it.time }
            val jobTickets = ticketJobs.map { it.id }.toSet()
            val others = open.filter { it.id !in jobTickets }
                .sortedWith(compareBy<AssistanceTicketResponse> { priorityRank(it.priority) }.thenBy { it.createdAt })
            return TechnicianToday(
                subtitle = "${weekdayDayMonth(today)} · " + when (jobs.size) {
                    0 -> "sin trabajos asignados"
                    1 -> "1 trabajo"
                    else -> "${jobs.size} trabajos"
                },
                uploads = if (pendingUploads == 0) {
                    UploadBanner("Todo enviado", "sin pendientes de envío", allSent = true)
                } else {
                    UploadBanner("$pendingUploads ${if (pendingUploads == 1) "registro" else "registros"} sin enviar", "se envían al volver la conexión", allSent = false)
                },
                now = jobs.firstOrNull()?.toNow(),
                pending = jobs.drop(1).map { it.toRow() } + others.map { it.toRow(now) },
                done = closed.filter { mine(it) && it.closedAt?.toLocal()?.toLocalDate() == today }
                    .sortedByDescending { it.closedAt }
                    .map { it.toRow(now).copy(trailing = it.closedAt?.toLocal()?.let(::hourMinute), trailingBadge = null) },
            )
        }

        private data class Job(val time: LocalDateTime, val ticket: AssistanceTicketResponse? = null, val order: InstallationOrderDto? = null) {
            fun toNow(): NowJob = ticket?.let {
                NowJob(
                    TodayTarget.Ticket(it.id), "Ticket #${it.id}", priorityBadge(it.priority), hourMinute(time), categoryLabel(it.category),
                    it.name.toTitleCase(), joinPlace(it.address, it.place, " · "), it.category.equals(INSTALL_CATEGORY, ignoreCase = true),
                    joinPlace(it.address, it.place, ", "),
                )
            } ?: order!!.let {
                NowJob(
                    TodayTarget.Order(it.id), "Orden #${it.id}", null, hourMinute(time), INSTALL_TITLE, it.customerName(),
                    joinPlace(it.customerAddress, it.place?.name, " · "), true, joinPlace(it.customerAddress, it.place?.name, ", "),
                )
            }

            fun toRow(): TodayRow = toNow().let { job ->
                TodayRow(job.target, if (order != null) job.label else "#${ticket!!.id}", job.priority, job.time, null, job.title,
                    listOf(job.name, (ticket?.place ?: order?.place?.name)?.toTitleCase().orEmpty()).filter { it.isNotEmpty() }.joinToString(" · "))
            }
        }
    }
}

private const val INSTALL_CATEGORY = "Instalación de Internet"
private const val INSTALL_TITLE = "Instalación de internet"
private val WORKING = setOf(AssistanceTicketStatus.ASSIGNED, AssistanceTicketStatus.IN_PROGRESS)
private val OPEN_TICKETS = setOf(AssistanceTicketStatus.PENDING, AssistanceTicketStatus.REOPEN) + WORKING
private val OPEN_ORDERS = setOf(InstallationOrderStatus.SOLICITADO, InstallationOrderStatus.EN_CURSO)

private fun Date.toLocal(): LocalDateTime = LocalDateTime.ofInstant(toInstant(), FICHA_ZONE)
private fun AssistanceTicketResponse.plannedAt(): LocalDateTime = (scheduledAt ?: createdAt).toLocal()
private fun hourMinute(time: LocalDateTime) = "%02d:%02d".format(time.hour, time.minute)
private fun InstallationOrderDto.customerName() = "$customerFirstName $customerLastName".toTitleCase()

private fun joinPlace(address: String?, place: String?, separator: String) =
    listOfNotNull(address?.trim(), place?.toTitleCase()).filter { it.isNotEmpty() }.joinToString(separator)

private fun priorityRank(priority: String): Int = when (priority.trim().lowercase()) {
    "alta" -> 0
    "media" -> 1
    "baja" -> 2
    else -> 3
}

private fun AssistanceTicketResponse.toRow(now: Instant): TodayRow {
    val working = status in WORKING && !assignedTo.isNullOrBlank()
    return TodayRow(
        target = TodayTarget.Ticket(id),
        number = "#$id",
        priority = priorityBadge(priority),
        trailing = if (working) null else relativeLabel(createdAt.toInstant(), now),
        trailingBadge = if (working) StatusBadge("En curso · ${assignedTo!!.toTitleCase()}", BadgeTone.INFO) else null,
        title = categoryLabel(category),
        subtitle = listOf(name.toTitleCase(), place?.toTitleCase().orEmpty()).filter { it.isNotEmpty() }.joinToString(" · "),
    )
}

enum class AgendaFilter { ALL, UNASSIGNED, SCHEDULED }
data class AgendaItem(
    val target: TodayTarget, val whenLabel: String, val reference: String, val name: String, val address: String,
    val detail: String, val scheduled: Boolean,
)
/** Sales home «Instalaciones» (a-ini-04): orders and installation tickets, soonest first. */
data class SalesAgenda(val items: List<AgendaItem>, val unassignedLabel: String, val scheduledLabel: String, val summary: String, val doneLabel: String) {
    companion object {
        fun from(orders: List<InstallationOrderDto>, tickets: List<AssistanceTicketResponse>, now: Instant, filter: AgendaFilter): SalesAgenda {
            val today = LocalDateTime.ofInstant(now, FICHA_ZONE).toLocalDate()
            val installs = tickets.filter { it.category.equals(INSTALL_CATEGORY, ignoreCase = true) }
            val entries = orders.filter { it.status in OPEN_ORDERS }.map { order ->
                val technician = order.technician?.let { listOfNotNull(it.name, it.lastName).joinToString(" ").toTitleCase() }
                Entry(
                    AgendaItem(TodayTarget.Order(order.id), "", "Orden #${order.id}", order.customerName(),
                        joinPlace(order.customerAddress, order.place?.name, " · "), technician.orEmpty(), scheduled = technician != null),
                    order.scheduledDate, order.createdAt,
                )
            } + installs.filter { it.status in OPEN_TICKETS }.map { ticket ->
                val scheduled = ticket.status in WORKING
                Entry(
                    AgendaItem(TodayTarget.Ticket(ticket.id), "", "Ticket #${ticket.id}", ticket.name.toTitleCase(),
                        joinPlace(ticket.address, ticket.place, " · "), ticket.assignedTo?.toTitleCase().orEmpty(), scheduled = scheduled),
                    if (scheduled) ticket.plannedAt() else null, ticket.createdAt.toLocal(),
                )
            }
            val all = entries.filter { it.item.scheduled }.sortedWith(compareBy(nullsLast()) { it.time }) +
                entries.filter { !it.item.scheduled }.sortedBy { it.created }
            val items = all.map { entry ->
                val item = entry.item
                if (item.scheduled) {
                    item.copy(whenLabel = entry.time?.let { whenLabel(it, today) } ?: "Sin fecha")
                } else {
                    item.copy(whenLabel = "Por asignar", detail = "Sin técnico ni fecha" + (entry.created?.let { " · creado el ${dayMonth(it.toLocalDate())}" } ?: ""))
                }
            }
            val visible = items.filter {
                when (filter) {
                    AgendaFilter.ALL -> true
                    AgendaFilter.UNASSIGNED -> !it.scheduled
                    AgendaFilter.SCHEDULED -> it.scheduled
                }
            }
            val weekStart = today.with(DayOfWeek.MONDAY).atStartOfDay()
            val done = orders.count { it.status == InstallationOrderStatus.CERRADO && (it.scheduledDate ?: it.createdAt)?.let { at -> at >= weekStart } == true } +
                installs.count { it.status == AssistanceTicketStatus.CLOSED && it.closedAt?.toLocal()?.let { at -> at >= weekStart } == true }
            return SalesAgenda(
                items = visible,
                unassignedLabel = "Por asignar · ${items.count { !it.scheduled }}",
                scheduledLabel = "Programadas · ${items.count { it.scheduled }}",
                summary = "${visible.size} ${if (visible.size == 1) "instalación" else "instalaciones"} · tickets y órdenes, la más próxima primero",
                doneLabel = "Realizadas esta semana ($done)",
            )
        }

        private data class Entry(val item: AgendaItem, val time: LocalDateTime?, val created: LocalDateTime?)

        private fun whenLabel(time: LocalDateTime, today: LocalDate): String {
            val day = if (time.toLocalDate() == today) "Hoy" else weekdayDayMonth(time.toLocalDate()).replaceFirstChar { it.uppercase() }
            return "$day ${hourMinute(time)}"
        }
    }
}
