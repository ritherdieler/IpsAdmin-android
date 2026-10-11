package com.dscorp.ispadmin.data.datasource.remote.dashboard

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/** Core sub-endpoints of /dashboard used by the Clients tab. Each one is independent. */
interface ClientsDashboardApi {
    @GET("dashboard/plan-analysis")
    suspend fun planAnalysis(): Response<PlanAnalysisDto>

    /** [months] limits ticket history to the last N months; open tickets are always current. */
    @GET("dashboard/assistance-tickets")
    suspend fun assistanceTickets(@Query("months") months: Int?): Response<AssistanceTicketsDto>

    @GET("dashboard/client-quality")
    suspend fun clientQuality(): Response<ClientQualityDto>

    @GET("dashboard/geographic-performance")
    suspend fun geographicPerformance(): Response<GeographicPerformanceDto>

    @GET("dashboard/client-lifecycle")
    suspend fun clientLifecycle(): Response<ClientLifecycleDto>

    @GET("dashboard/collections")
    suspend fun collections(): Response<CollectionSummaryDto>
}
