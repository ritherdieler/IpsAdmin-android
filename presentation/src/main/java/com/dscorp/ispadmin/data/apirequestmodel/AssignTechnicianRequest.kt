package com.dscorp.ispadmin.data.apirequestmodel

data class AssignTechnicianRequest(
    val technicianId: Int,
    val assignedById: Int,
    val scheduledDate: String  // Formato: "yyyy-MM-dd"
) 