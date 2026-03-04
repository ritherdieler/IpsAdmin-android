package com.dscorp.ispadmin.domain.model

import java.text.NumberFormat
import java.util.Locale

class DashBoardDataResponse(
    val economicResume: EconomicResume,
    val activeSubscriptions: Int,
    val subscriptionsResume: SubscriptionsResumeStatics,
    val cancellationsResume: CancellationResumeDto,
    val paymentResume: Map<String, Double>,
    val subscriptionsHistoryStatics: List<MonthlySubscriptionResume>,
    val monthlyCollects: List<MonthlyCollectsResume>,
    val grossRevenueHistoryStatics: List<MonthlyGrossRevenueResume>,
    val reconnections: Int,
    val assistanceTicketResume: Any? = null,
    val planAnalysisResume: Any? = null,
    val clientQualityResume: Any? = null,
    val fixedCostAnalysisResume: Any? = null,
    val installationOrdersResume: Any? = null,
    val geographicPerformanceResume: Any? = null,
    val teamPerformanceResume: Any? = null,
    val networkHealthResume: Any? = null,
    val clientLifecycleResume: Any? = null,
    val subscriptionLogSummary: Map<String, SubscriptionLogSummary>? = null
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

data class SubscriptionLogSummary(
    val actionType: String,
    val totalCount: Int,
    val monthlyDetails: List<MonthlyDetail>
)

data class MonthlyDetail(
    val count: Int,
    val period: String
)