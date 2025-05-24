package com.dscorp.ispadmin.data.model

import java.time.LocalDateTime

data class InstallationOrderDto(
    val id: Int,
    val customerFirstName: String,
    val customerLastName: String,
    val customerAddress: String,
    val customerPhone: String,
    val status: InstallationOrderStatus,
    val scheduledDate: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val sellerId: Int?,
    val technicianId: Int?,
    val assignedById: Int?
) 