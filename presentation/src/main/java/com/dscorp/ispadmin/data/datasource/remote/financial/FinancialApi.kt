package com.dscorp.ispadmin.data.datasource.remote.financial

import com.dscorp.ispadmin.domain.model.DownloadDocumentResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FinancialApi {
    @GET("report/financial")
    suspend fun report(@Query("serviceMonth") serviceMonth: String, @Query("movementsMonth") movementsMonth: String): Response<FinancialReportDto>
    @GET("report/financial/cuts/{id}/xlsx")
    suspend fun export(@Path("id") id: String): Response<DownloadDocumentResponse>
}
