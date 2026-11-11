package com.dscorp.ispadmin.data.response

import com.dscorp.ispadmin.presentation.util.clock12
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
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
    /** "APP" when the customer reported it from Mi GigaFiber; preferred visit slot and day (yyyy-MM-dd). */
    val origin: String? = null,
    val preferredSlot: String? = null,
    val preferredDate: String? = null,
):Serializable {
    fun getCreatedAtDateAsString(): String = dateWithClock12(createdAt)

    fun getScheduledAtDateAsString(): String = dateWithClock12(scheduledAt ?: createdAt)

    /** «09 octubre 2026 - 4:20 p. m.» (12-hour clock, see [clock12]). */
    private fun dateWithClock12(date: Date): String {
        val calendar = Calendar.getInstance().apply { time = date }
        val day = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(date)
        return "$day - ${clock12(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))}"
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