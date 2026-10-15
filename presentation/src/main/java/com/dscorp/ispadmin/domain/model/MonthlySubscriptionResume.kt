package com.dscorp.ispadmin.domain.model

import java.util.*

data class MonthlySubscriptionResume(
    val totalActiveSubscriptions: Int,
    val newSubscriptions: Int,
    val cancelledSubscriptions: Int,
    val date: Date,
    /** ACTIVE only (without cut off); null for snapshots taken before the Core recorded it. */
    val activeSubscriptions: Int? = null,
)