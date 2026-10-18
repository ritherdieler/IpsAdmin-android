package com.dscorp.ispadmin.presentation.ui.features.home

import com.dscorp.ispadmin.data.model.InstallationOrderDto
import com.dscorp.ispadmin.data.model.InstallationOrderStatus
import com.dscorp.ispadmin.data.model.UserDto
import com.dscorp.ispadmin.data.response.AssistanceTicketResponse
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus
import com.dscorp.ispadmin.domain.model.Place
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

internal val LIMA: ZoneId = ZoneId.of("America/Lima")

/** Wednesday 14 Oct 2026, 10:00 in Lima. */
internal val NOW: LocalDateTime = LocalDateTime.of(2026, 10, 14, 10, 0)

internal fun at(time: LocalDateTime): Date = Date.from(time.atZone(LIMA).toInstant())

internal fun ticket(
    id: Int,
    category: String = "Sin Conexión a Internet",
    status: AssistanceTicketStatus = AssistanceTicketStatus.PENDING,
    priority: String = "Media",
    name: String = "PEDRO SANCHEZ CHAVEZ",
    place: String? = "las colinas",
    address: String? = null,
    assignedTo: String? = null,
    created: LocalDateTime = NOW.minusHours(2),
    scheduled: LocalDateTime? = null,
    closed: LocalDateTime? = null,
) = AssistanceTicketResponse(
    id = id, name = name, category = category, description = "", status = status, priority = priority,
    createdAt = at(created), scheduledAt = scheduled?.let(::at), assignedTo = assignedTo, place = place, address = address,
    sheetImageUrl = null, closedAt = closed?.let(::at),
)

internal fun order(
    id: Int,
    status: InstallationOrderStatus = InstallationOrderStatus.EN_CURSO,
    technician: UserDto? = null,
    scheduled: LocalDateTime? = null,
    created: LocalDateTime = NOW.minusDays(2),
    first: String = "Tomas",
    last: String = "Vega Salas",
    address: String = "Av. Principal 510",
    place: String = "9 de octubre",
) = InstallationOrderDto(
    id = id, customerFirstName = first, customerLastName = last, customerAddress = address, customerPhone = "",
    status = status, scheduledDate = scheduled, technician = technician, place = Place(name = place), createdAt = created,
)
