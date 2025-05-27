package com.dscorp.ispadmin.domain.model

import com.dscorp.ispadmin.data.model.InstallationOrderStatus
import java.time.LocalDateTime

data class InstallationOrder(
    val id: Int = 0,
    val customerFirstName: String = "",
    val customerLastName: String = "",
    val customerAddress: String = "",
    val customerPhone: String = "",
    val customerDni: String = "",
    val seller: User? = null,
    val assignedBy: User? = null,
    val technician: User? = null,
    val scheduledDate: LocalDateTime? = null,
    val status: InstallationOrderStatus = InstallationOrderStatus.SOLICITADO,
    val cancellationReason: String? = null,
    val subscription: Subscription? = null,
    val place: Place? = null,
    val createdAt: LocalDateTime? = null,
){
    data class Subscription(
        val id: Int = 0,
        val clientName: String = "",
        val status: String = ""
    )
}
