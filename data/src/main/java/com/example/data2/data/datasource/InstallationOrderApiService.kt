package com.example.data2.data.datasource

import com.dscorp.ispadmin.domain.model.InstallationOrder
import com.dscorp.ispadmin.domain.model.InstallationOrderStatus
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Interfaz para los servicios de API relacionados con órdenes de instalación
 */
interface InstallationOrderApiService {
    @GET("installation-order")
    suspend fun getAllInstallationOrders(): Response<List<InstallationOrder>>
    
    @GET("installation-order/{id}")
    suspend fun getInstallationOrderById(@Path("id") id: Int): Response<InstallationOrder>
    
    @GET("installation-order/status/{status}")
    suspend fun getInstallationOrdersByStatus(@Path("status") status: InstallationOrderStatus): Response<List<InstallationOrder>>
    
    @POST("installation-order")
    suspend fun createInstallationOrder(@Body installationOrder: InstallationOrder): Response<InstallationOrder>
    
    @PUT("installation-order/{id}/assign")
    suspend fun assignTechnician(
        @Path("id") orderId: Int,
        @Query("technicianId") technicianId: Int,
        @Query("assignedById") assignedById: Int,
        @Query("scheduledDateTime") scheduledDateTime: String
    ): Response<InstallationOrder>
    
    @PUT("installation-order/{id}/close")
    suspend fun closeInstallationOrder(@Path("id") orderId: Int): Response<InstallationOrder>
    
    @PUT("installation-order/{id}/cancel")
    suspend fun cancelInstallationOrder(
        @Path("id") orderId: Int,
        @Query("reason") cancellationReason: String?
    ): Response<InstallationOrder>
} 
