package com.dscorp.ispadmin.data.repository

import androidx.paging.PagingData
import com.dscorp.ispadmin.domain.model.InstallationOrder
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface InstallationOrderRepository {

    suspend fun getInstallationOrderById(id: Int): InstallationOrder?
    suspend fun createInstallationOrder(installationOrder: InstallationOrder): InstallationOrder
    suspend fun assignTechnician(
        orderId: Int,
        technicianId: Int,
        assignedById: Int,
        scheduledDate: LocalDateTime
    ): InstallationOrder
    suspend fun closeInstallationOrder(orderId: Int): InstallationOrder
    suspend fun cancelInstallationOrder(orderId: Int, cancellationReason: String?): InstallationOrder

    /**
     * Obtiene todas las órdenes de instalación sin filtros de forma paginada
     * @return Flow de PagingData con todas las órdenes de instalación
     */
    fun getAllInstallationOrdersPaginated(): Flow<PagingData<InstallationOrder>>
    
    /**
     * Obtiene las órdenes de instalación de un vendedor específico de forma paginada
     * @param sellerId ID del vendedor
     * @return Flow de PagingData con las órdenes de instalación del vendedor
     */
    fun getInstallationOrdersBySellerPaginated(sellerId: Int): Flow<PagingData<InstallationOrder>>
    
    /**
     * Obtiene las órdenes de instalación de un técnico específico de forma paginada
     * @param technicianId ID del técnico
     * @return Flow de PagingData con las órdenes de instalación del técnico
     */
    fun getInstallationOrdersByTechnicianPaginated(technicianId: Int): Flow<PagingData<InstallationOrder>>

} 