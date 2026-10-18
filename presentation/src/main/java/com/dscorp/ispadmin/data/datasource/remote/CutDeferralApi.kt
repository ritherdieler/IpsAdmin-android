package com.dscorp.ispadmin.data.datasource.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** Core cut deferral of one subscription: the open one (204 when none) and a new one. */
interface CutDeferralApi {
    @GET("subscription/{id}/cut-deferral")
    suspend fun current(@Path("id") subscriptionId: Int): Response<CutDeferralResponse>

    @POST("subscription/{id}/cut-deferral")
    suspend fun defer(@Path("id") subscriptionId: Int, @Body body: CutDeferralBody): Response<CutDeferralResponse>
}

data class CutDeferralResponse(val id: Long? = null, val untilDate: String = "", val reason: String = "", val createdBy: Int? = null)
