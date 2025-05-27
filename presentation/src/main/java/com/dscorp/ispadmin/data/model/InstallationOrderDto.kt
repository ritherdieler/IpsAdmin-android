package com.dscorp.ispadmin.data.model

import com.dscorp.ispadmin.domain.model.Place
import java.time.LocalDateTime

data class InstallationOrderDto(
    val id: Int,
    val customerFirstName: String,
    val customerLastName: String,
    val customerAddress: String,
    val customerPhone: String,
    val customerDni: String? = null,
    val status: InstallationOrderStatus,
    val scheduledDate: LocalDateTime?,
    val seller: UserDto? = null,
    val assignedBy: UserDto? = null,
    val technician: UserDto? = null,
    val place: Place?,
    val createdAt: LocalDateTime? = null,
) 