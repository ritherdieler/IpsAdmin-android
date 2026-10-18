package com.dscorp.ispadmin.data.datasource.remote

import com.dscorp.ispadmin.data.model.OutlayDTO
import retrofit2.http.GET
import retrofit2.http.Query

/** Core outlays (GET outLay), newest first. */
interface OutlayApi {
    @GET("outLay")
    suspend fun page(@Query("page") page: Int, @Query("size") size: Int, @Query("currentMonthOnly") currentMonthOnly: Boolean): OutlayPageResponse
}

data class OutlayPageResponse(val content: List<OutlayDTO> = emptyList(), val totalElements: Long = 0)
