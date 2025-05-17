package com.example.data2.data.apirequestmodel

data class AssignTechnicianRequest(
    val technicianId: Int,
    val assignedById: Int,
    val scheduledDate: String  // Formato: "yyyy-MM-dd"
) 