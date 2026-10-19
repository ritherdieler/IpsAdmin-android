package com.dscorp.ispadmin.data.datasource.remote

import com.dscorp.ispadmin.domain.model.InstallationType
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Core plan catalog with how many active subscriptions each plan has (GET plan/all). */
interface PlanApi {
    @GET("plan/all")
    suspend fun allWithCounts(): List<PlanCountResponse>

    /** Updates name, price and speeds (the type is kept); the Core also updates the plan's MikroTik queues. */
    @PUT("plan")
    suspend fun update(@Body plan: PlanSaveRequest): Response<Unit>

    @POST("plan")
    suspend fun create(@Body plan: PlanSaveRequest): Response<Unit>

    @PUT("plan/{id}/activate")
    suspend fun activate(@Path("id") planId: Int): Response<Unit>

    @PUT("plan/{id}/deactivate")
    suspend fun deactivate(@Path("id") planId: Int): Response<Unit>
}

data class PlanSaveRequest(
    val id: Int?,
    val name: String,
    val price: Double,
    val downloadSpeed: Int,
    val uploadSpeed: Int,
    val type: InstallationType,
)

data class PlanCountResponse(
    val id: Int? = null,
    val name: String? = null,
    val price: Double? = null,
    val downloadSpeed: Int? = null,
    val uploadSpeed: Int? = null,
    val type: InstallationType? = null,
    @SerializedName(value = "isActive", alternate = ["active"]) val isActive: Boolean = true,
    val activeSubscriptionsCount: Int = 0,
)
