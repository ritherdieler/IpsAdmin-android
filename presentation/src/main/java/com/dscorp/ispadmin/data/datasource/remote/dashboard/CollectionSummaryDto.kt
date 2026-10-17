package com.dscorp.ispadmin.data.datasource.remote.dashboard

import com.dscorp.ispadmin.domain.model.dashboard.CollectionSummary
import java.math.BigDecimal

data class CollectionSummaryDto(
    val dueSoonDays: Int? = null,
    val dueSoonAmount: BigDecimal? = null,
    val dueSoonInvoices: Long? = null,
    val activeDeferrals: Long? = null,
    val deferralsExpiringToday: Long? = null,
    val deferralWindowDays: Int? = null,
    val deferralsKeptPercent: Double? = null,
    val cutsForDebt: Long? = null,
    val restoredAfterPayment: Long? = null,
    val overdueAmount: BigDecimal? = null,
    val delinquentSubscriptions: Long? = null,
    val overduePerDelinquent: BigDecimal? = null,
) {
    fun toDomain() = CollectionSummary(
        dueSoonDays = dueSoonDays ?: 0,
        dueSoonAmount = dueSoonAmount ?: BigDecimal.ZERO,
        dueSoonInvoices = dueSoonInvoices ?: 0,
        activeDeferrals = activeDeferrals ?: 0,
        deferralsExpiringToday = deferralsExpiringToday ?: 0,
        deferralWindowDays = deferralWindowDays ?: 0,
        deferralsKeptPercent = deferralsKeptPercent,
        cutsForDebt = cutsForDebt ?: 0,
        restoredAfterPayment = restoredAfterPayment ?: 0,
        overdueAmount = overdueAmount ?: BigDecimal.ZERO,
        delinquentSubscriptions = delinquentSubscriptions ?: 0,
        overduePerDelinquent = overduePerDelinquent,
    )
}
