package com.example.data2.data.network.model

import com.example.data2.data.model.InstallationOrder
import com.example.data2.data.model.InstallationOrderStatus
import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class InstallationOrderDTO(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("client_name") val clientName: String = "",
    @SerializedName("client_id") val clientId: Int = 0,
    @SerializedName("address") val address: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("status") val status: String = "",
    @SerializedName("creation_date") val creationDate: String = "",
    @SerializedName("scheduled_date") val scheduledDate: String? = null,
    @SerializedName("technician_name") val technicianName: String? = null,
    @SerializedName("technician_id") val technicianId: Int? = null,
    @SerializedName("assigned_by") val assignedById: Int? = null,
    @SerializedName("completion_date") val completionDate: String? = null,
    @SerializedName("cancellation_reason") val cancellationReason: String? = null,
    @SerializedName("comments") val comments: String? = null
) {
    fun toInstallationOrder(): InstallationOrder {
        val dateFormatter = DateTimeFormatter.ISO_DATE
        
        return InstallationOrder(
            id = id,
            clientName = clientName,
            clientId = clientId,
            address = address,
            type = type,
            status = parseStatus(status),
            creationDate = LocalDate.parse(creationDate, dateFormatter),
            scheduledDate = scheduledDate?.let { LocalDate.parse(it, dateFormatter) },
            technicianName = technicianName,
            technicianId = technicianId,
            assignedById = assignedById,
            completionDate = completionDate?.let { LocalDate.parse(it, dateFormatter) },
            cancellationReason = cancellationReason,
            comments = comments
        )
    }
    
    private fun parseStatus(status: String): InstallationOrderStatus {
        return when (status.uppercase()) {
            "PENDING" -> InstallationOrderStatus.PENDING
            "SCHEDULED" -> InstallationOrderStatus.SCHEDULED
            "COMPLETED" -> InstallationOrderStatus.COMPLETED
            "CANCELLED" -> InstallationOrderStatus.CANCELLED
            else -> InstallationOrderStatus.PENDING
        }
    }
    
    companion object {
        fun fromInstallationOrder(order: InstallationOrder): InstallationOrderDTO {
            val dateFormatter = DateTimeFormatter.ISO_DATE
            
            return InstallationOrderDTO(
                id = order.id,
                clientName = order.clientName,
                clientId = order.clientId,
                address = order.address,
                type = order.type,
                status = order.status.name,
                creationDate = order.creationDate.format(dateFormatter),
                scheduledDate = order.scheduledDate?.format(dateFormatter),
                technicianName = order.technicianName,
                technicianId = order.technicianId,
                assignedById = order.assignedById,
                completionDate = order.completionDate?.format(dateFormatter),
                cancellationReason = order.cancellationReason,
                comments = order.comments
            )
        }
    }
} 