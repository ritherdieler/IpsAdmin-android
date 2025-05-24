package com.dscorp.ispadmin.domain.model

import java.util.*

data class MonthlySubscriptionResume(
    val totalActiveSubscriptions: Int,
    val newSubscriptions: Int,
    val cancelledSubscriptions: Int,
    val date: Date,
)