package com.dscorp.ispadmin.data.datasource.remote

import retrofit2.http.GET

/** Core home summary for collection roles (GET dashboard/home). */
interface HomeApi {
    @GET("dashboard/home")
    suspend fun summary(): HomeSummaryResponse
}

data class MoneyCountResponse(val amount: Double = 0.0, val count: Long = 0)

data class DailyCollectionResponse(val date: String = "", val amount: Double = 0.0, val count: Long = 0)

data class HomeCollectionsResponse(
    val today: MoneyCountResponse = MoneyCountResponse(),
    val sameWeekdayLastWeek: MoneyCountResponse = MoneyCountResponse(),
    val lastWeekDate: String = "",
    val monthToDate: MoneyCountResponse = MoneyCountResponse(),
    val previousMonthToDate: MoneyCountResponse = MoneyCountResponse(),
    val daily: List<DailyCollectionResponse> = emptyList(),
)

data class HomeSubscriptionsResponse(val active: Long = 0, val cutOff: Long = 0)

data class HomeCutResponse(val nextCutDate: String = "", val candidates: Long = 0, val automatic: Boolean = false)

data class OwingSubscriptionResponse(val id: Int, val name: String?, val plan: String?, val place: String?, val debt: Double = 0.0)

data class HomeOwingResponse(val count: Int = 0, val top: List<OwingSubscriptionResponse> = emptyList())

data class HomeTicketsResponse(val pending: Long = 0, val highPriority: Long = 0)

data class HomeSummaryResponse(
    val asOf: String = "",
    val collections: HomeCollectionsResponse = HomeCollectionsResponse(),
    val subscriptions: HomeSubscriptionsResponse = HomeSubscriptionsResponse(),
    val overdue: MoneyCountResponse = MoneyCountResponse(),
    val cut: HomeCutResponse = HomeCutResponse(),
    val owingTwoMonths: HomeOwingResponse = HomeOwingResponse(),
    val tickets: HomeTicketsResponse = HomeTicketsResponse(),
)
