package com.dscorp.ispadmin.data.extensions

import com.dscorp.ispadmin.data.model.InstallationOrderDto
import com.dscorp.ispadmin.domain.model.InstallationOrder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest


fun String.encryptWithSHA384(): String {
    val bytes = MessageDigest
        .getInstance("SHA-384")
        .digest(this.toByteArray(StandardCharsets.UTF_8))

    return bytes.fold("") { str, it -> str + "%02x".format(it) }
}

/**
 * Extensión para convertir un DTO a un modelo de dominio
 */
fun InstallationOrderDto.toDomain(): InstallationOrder {
    return InstallationOrder(
        id = id,
        customerFirstName = customerFirstName,
        customerLastName = customerLastName,
        customerAddress = customerAddress,
        customerPhone = customerPhone,
        status = status,
        scheduledDate = scheduledDate,
        assignedBy = assignedBy?.toDomain(),
        technician = technician?.toDomain(),
        seller = seller?.toDomain(),
    )
}

fun main() {
    println("cintia123".encryptWithSHA384())
}