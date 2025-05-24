package com.dscorp.ispadmin.domain.model

import java.util.*

data class MonthlyCollectsResume(
    val grossIncome:Double,
    val totalRaised: Double,
    val totalDiscount: Double,
    val totalReceivables: Double,
    val date: Date
)