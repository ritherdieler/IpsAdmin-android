package com.dscorp.ispadmin.data.datasource.remote

import com.dscorp.ispadmin.data.model.InstallationOrderDto
import com.dscorp.ispadmin.data.model.InstallationOrderStatus
import com.dscorp.ispadmin.data.model.PageResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface InstallationOrderApi {
    @GET("installation-order/paginated")
    suspend fun getPaginatedInstallationOrders(
        @Query("userId") userId: Int,
        @Query("status") status: InstallationOrderStatus?,
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("userType") userType: String?
    ): PageResponse<InstallationOrderDto>
    
    @GET("installation-order/all-paginated")
    suspend fun getAllInstallationOrdersPaginated(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): PageResponse<InstallationOrderDto>
    
    /**
     * Obtiene las órdenes de instalación de un vendedor específico paginadas
     */
    @GET("installation-order/seller/{sellerId}")
    suspend fun getInstallationOrdersBySellerPaginated(
        @Path("sellerId") sellerId: Int,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): PageResponse<InstallationOrderDto>
    
    /**
     * Obtiene las órdenes de instalación de un técnico específico paginadas
     */
    @GET("installation-order/technician/{technicianId}")
    suspend fun getInstallationOrdersByTechnicianPaginated(
        @Path("technicianId") technicianId: Int,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): PageResponse<InstallationOrderDto>
}