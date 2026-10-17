package com.dscorp.ispadmin.data.response

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AssistanceTicketResponse(
    val id: Int = 0,
    val name: String,
    val phone: String = "",
    val ip: String? = null,
    val category: String,
    val description: String,
    var status: AssistanceTicketStatus,
    val comments: String? = null,
    var priority: String,
    var createdAt: Date = Date(),
    var scheduledAt: Date? = null,
    val resolvedAt: Date? = null,
    val assignedTo: String? = null,
    val place: String? = null,
    val address: String? = null,
    val sheetImageUrl:String?,
    val assignedAt: Date? = null,
    val closedAt: Date? = null,
    val subscriptionId: Int? = null,
    val placeId: Int? = null,
    val isExternalCustomer: Boolean = false,
):Serializable {
    fun getCreatedAtDateAsString(): String {
        val dateFormatter =
            SimpleDateFormat("dd MMMM yyyy - hh:mm", Locale.getDefault())
        return dateFormatter.format(createdAt)
    }

    fun getScheduledAtDateAsString(): String {
        val dateFormatter =
            SimpleDateFormat("dd MMMM yyyy - hh:mm", Locale.getDefault())
        return dateFormatter.format(scheduledAt ?: createdAt)
    }

    fun getStatusAsString(): String {
        return when (status) {
            AssistanceTicketStatus.PENDING -> "Pendiente"
            AssistanceTicketStatus.ASSIGNED -> "Asignado"
            AssistanceTicketStatus.IN_PROGRESS -> "En progreso"
            AssistanceTicketStatus.RESOLVED -> "Resuelto"
            AssistanceTicketStatus.CLOSED -> "Cerrado"
            AssistanceTicketStatus.CANCELLED -> "Cancelado"
            AssistanceTicketStatus.REOPEN -> "Reabierto"
        }
    }

    fun displayableIp(): String? = ip?.takeIf { it.isNotBlank() }
}

enum class AssistanceTicketStatus {
    PENDING,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    CANCELLED,
    REOPEN,
}