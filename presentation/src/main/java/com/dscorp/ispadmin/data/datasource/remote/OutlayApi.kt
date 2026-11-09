package com.dscorp.ispadmin.data.datasource.remote

import com.dscorp.ispadmin.data.model.OutlayDTO
import retrofit2.http.GET
import retrofit2.http.Query

/** Core outlays (GET outLay), newest first. */
interface OutlayApi {
    @GET("outLay")
    suspend fun page(@Query("page") page: Int, @Query("size") size: Int, @Query("currentMonthOnly") currentMonthOnly: Boolean): OutlayPageResponse

    /** Variable-outlay cap and fixed totals of the current month. */
    @GET("outLay/variable-summary")
    suspend fun variableSummary(): OutlaySummaryResponse
}

data class OutlayPageResponse(val content: List<OutlayDTO> = emptyList(), val totalElements: Long = 0)

data class OutlaySummaryResponse(
    val spent: Double = 0.0,
    val limit: Double = 0.0,
    val available: Double = 0.0,
    val threshold: Double = 0.0,
    val fixedTotal: Double = 0.0,
    val previousFixedTotal: Double = 0.0,
)
