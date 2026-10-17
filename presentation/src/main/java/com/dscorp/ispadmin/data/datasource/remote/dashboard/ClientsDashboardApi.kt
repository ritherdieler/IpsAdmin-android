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

    /** [months] limits the tickets behind incidents per 100 clients to the last N months. */
    @GET("dashboard/geographic-performance")
    suspend fun geographicPerformance(@Query("months") months: Int?): Response<GeographicPerformanceDto>

    /** [months] limits cancellations by plan and zone to the last N months. */
    @GET("dashboard/client-lifecycle")
    suspend fun clientLifecycle(@Query("months") months: Int?): Response<ClientLifecycleDto>

    @GET("dashboard/collections")
    suspend fun collections(): Response<CollectionSummaryDto>
}
