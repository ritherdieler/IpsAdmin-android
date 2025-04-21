package com.example.data2.data.apirequestmodel

import java.time.LocalDate

data class AssignTechnicianRequest(
    val technicianId: Int,
    val assignedById: Int,
    val scheduledDate: String  // Formato: "yyyy-MM-dd"
) 