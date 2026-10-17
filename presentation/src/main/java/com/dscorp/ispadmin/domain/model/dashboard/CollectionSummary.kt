package com.dscorp.ispadmin.domain.model.dashboard

import java.math.BigDecimal

/** Operational collection follow-up from the Core (`/dashboard/collections`). Amounts are net of discounts. */
data class CollectionSummary(
    val dueSoonDays: Int,
    val dueSoonAmount: BigDecimal,
    val dueSoonInvoices: Long,
    val activeDeferrals: Long,
    val deferralsExpiringToday: Long,
    val deferralWindowDays: Int,
    val deferralsKeptPercent: Double?,
    val cutsForDebt: Long,
    val restoredAfterPayment: Long,
    val overdueAmount: BigDecimal,
    val delinquentSubscriptions: Long,
    val overduePerDelinquent: BigDecimal?,
)
