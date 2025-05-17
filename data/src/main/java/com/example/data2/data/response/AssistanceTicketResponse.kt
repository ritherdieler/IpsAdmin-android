package com.example.data2.data.response

import java.io.Serializable
import java.util.Date

data class AssistanceTicketResponse(
    val id: Int = 0,
    val name: String,
    val phone: String = "",
    val category: String,
    val description: String,
    var status: AssistanceTicketStatus,
    val comments: String? = null,
    var priority: String,
    var createdAt: Date = Date(),
    val resolvedAt: Date? = null,
    val assignedTo: String? = null,
    val place: String? = null,
    val address: String? = null,
    val sheetImageUrl:String,
):Serializable {
    fun getCreatedAtDateAsString(): String {
        val dateFormatter =
            java.text.SimpleDateFormat("dd MMMM yyyy - hh:mm", java.util.Locale.getDefault())
        return dateFormatter.format(createdAt)
    }

    fun getStatusAsString(): String {
        return when (status) {
            AssistanceTicketStatus.PENDING -> "Pendiente"
            AssistanceTicketStatus.ASSIGNED -> "Asignado"
            AssistanceTicketStatus.IN_PROGRESS -> "En progreso"
            AssistanceTicketStatus.RESOLVED -> "Resuelto"
            AssistanceTicketStatus.CLOSED -> "Cerrado"
            AssistanceTicketStatus.CANCELLED -> "Cancelado"
        }
    }
}

enum class AssistanceTicketStatus {
    PENDING,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    CANCELLED
}