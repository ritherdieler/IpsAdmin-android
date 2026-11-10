package com.dscorp.ispadmin.data.response

/** Core `GET assistanceTicket/{id}/comments`: the customer side of a ticket reported from Mi GigaFiber (R2.5). */
data class TicketThreadResponse(
    val ticketId: Int = 0,
    val origin: String? = null,
    val slot: String? = null,
    val preferredDate: String? = null,
    val previousTicketId: Int? = null,
    val comments: List<TicketThreadCommentResponse> = emptyList(),
)

data class TicketThreadCommentResponse(
    val id: Long = 0,
    val author: String = "",
    val kind: String = "COMMENT",
    val authorName: String? = null,
    val body: String = "",
    val visibleToCustomer: Boolean = true,
    /** ISO-8601 with the Lima offset, e.g. 2026-11-02T19:40:00-05:00. */
    val at: String = "",
)
