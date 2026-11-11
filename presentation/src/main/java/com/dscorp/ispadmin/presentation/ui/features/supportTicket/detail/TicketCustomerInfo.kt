package com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail

import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import com.dscorp.ispadmin.data.response.TicketThreadResponse
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.FICHA_ZONE
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.dayMonth
import com.dscorp.ispadmin.presentation.util.clock12
import java.time.LocalDate
import java.time.OffsetDateTime

data class TicketCustomerComment(val author: String, val body: String, val at: String, val fromCustomer: Boolean)

/** What the customer said in Mi GigaFiber and the visit slot they prefer; the technician only reads it. */
data class TicketCustomerInfo(val slot: String?, val comments: List<TicketCustomerComment>) {
    companion object {
        const val ORIGIN_APP = "APP"

        /** Null for staff tickets: they have no customer side. */
        fun from(ticket: AssistanceTicketResponse, thread: TicketThreadResponse?): TicketCustomerInfo? {
            if (ticket.origin != ORIGIN_APP && thread == null) return null
            val slot = thread?.slot ?: ticket.preferredSlot
            val day = thread?.preferredDate ?: ticket.preferredDate
            return TicketCustomerInfo(
                slot = slotLabel(slot, day),
                comments = thread?.comments.orEmpty().filter { it.visibleToCustomer }.map { comment ->
                    val customer = comment.author == CUSTOMER
                    TicketCustomerComment(
                        author = if (customer) "Cliente" else comment.authorName?.takeIf { it.isNotBlank() } ?: "GigaFiber",
                        body = comment.body,
                        at = timeLabel(comment.at),
                        fromCustomer = customer,
                    )
                },
            )
        }

        /** «Prefiere la tarde (13 a 18 h) · mar 10 nov», the wording of the ticket text. */
        fun slotLabel(slot: String?, day: String?): String? {
            val parts = listOfNotNull(
                SLOT_LABELS[slot] ?: slot,
                day?.let { runCatching { LocalDate.parse(it) }.getOrNull() }?.let { "${weekday(it)} ${dayMonth(it)}" },
            )
            return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
        }

        private fun timeLabel(iso: String): String = runCatching {
            val at = OffsetDateTime.parse(iso).atZoneSameInstant(FICHA_ZONE)
            "${weekday(at.toLocalDate())} ${dayMonth(at.toLocalDate())}, ${clock12(at.hour, at.minute)}"
        }.getOrDefault("")

        private fun weekday(day: LocalDate) = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")[day.dayOfWeek.value - 1]

        private const val CUSTOMER = "CUSTOMER"
        private val SLOT_LABELS = mapOf(
            "MORNING" to "Prefiere la mañana (8 a 13 h)",
            "AFTERNOON" to "Prefiere la tarde (13 a 18 h)",
            "ANY" to "Cualquier horario",
        )
    }
}
