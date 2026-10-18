package com.dscorp.ispadmin.data.datasource.remote

import com.dscorp.ispadmin.domain.model.InstallationType
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET

/** Core plan catalog with how many active subscriptions each plan has (GET plan/all). */
interface PlanApi {
    @GET("plan/all")
    suspend fun allWithCounts(): List<PlanCountResponse>
}

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
