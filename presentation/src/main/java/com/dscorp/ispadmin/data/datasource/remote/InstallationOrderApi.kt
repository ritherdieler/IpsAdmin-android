package com.dscorp.ispadmin.data.datasource.remote

import com.dscorp.ispadmin.data.model.InstallationOrderDto
import com.dscorp.ispadmin.data.model.InstallationOrderStatus
import com.dscorp.ispadmin.data.model.PageResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface InstallationOrderApi {
    @GET("installation-order/paginated")
    suspend fun getPaginatedInstallationOrders(
        @Query("userId") userId: Int,
        @Query("status") status: InstallationOrderStatus?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): PageResponse<InstallationOrderDto>
}