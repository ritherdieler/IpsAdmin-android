package com.example.data2.data.model

import java.time.LocalDate

data class InstallationOrder(
    val id: Int = 0,
    val clientName: String = "",
    val clientId: Int = 0,
    val address: String = "",
    val type: String = "",
    val status: InstallationOrderStatus = InstallationOrderStatus.PENDING,
    val creationDate: LocalDate = LocalDate.now(),
    val scheduledDate: LocalDate? = null,
    val technicianName: String? = null,
    val technicianId: Int? = null,
    val assignedById: Int? = null,
    val completionDate: LocalDate? = null,
    val cancellationReason: String? = null,
    val comments: String? = null
)

enum class InstallationOrderStatus {
    PENDING,
    SCHEDULED,
    COMPLETED,
    CANCELLED
} 