package com.example.cleanarchitecture.domain.entity

import java.text.NumberFormat
import java.util.Locale

class DashBoardDataResponse(
    val economicResume: EconomicResume,
    val subscriptionsResume: SubscriptionsResumeStatics,
    val cancellationsResume: CancellationResumeDto,
    val paymentResume: Map<String, Double>,
    val subscriptionsHistoryStatics: List<MonthlySubscriptionResume>,
    val grossRevenueHistoryStatics: List<MonthlyGrossRevenueResume>,
    val monthlyCollects: List<MonthlyCollectsResume>
) {
    fun grossRevenueAsString() = economicResume.grossRevenue.toCurrencyString()
    fun totalRaisedAsString() = economicResume.totalRaised.toCurrencyString()
    fun totalDiscountAsString() = economicResume.totalDiscount.toCurrencyString()
    fun totalToCollectAsString() = economicResume.totalToCollect.toCurrencyString()
    fun fixedCostsAsString() = economicResume.fixedCosts.toCurrencyString()
    fun variableCostsAsString() = economicResume.outLaysFromCurrentMonth.toCurrencyString()
    fun marginAsString() = economicResume.margin.toCurrencyString()
    fun freeCashAsString() = economicResume.freeCash.toCurrencyString()
}
data class CancellationResumeDto(
    val cancelledByUsers: Int,
    val cancelledBySystem: Int
)
fun Double.toCurrencyString(): String {
    val numberFormat = NumberFormat.getNumberInstance(Locale("es", "PE"))
    return numberFormat.format(this)
}

data class MonthlyGrossRevenueResume(
    val totalCharged: Double,
    val billingDate: Long
)

data class EconomicResume(
    val grossRevenue: Double,
    val totalRaised: Double,
    val totalDiscount: Double,
    val totalToCollect: Double,
    val outLaysFromCurrentMonth: Double,
    val fixedCosts: Double,
    val margin: Double,
    val freeCash: Double,
    val corporateGrossRevenue: Double,
)