package com.dscorp.ispadmin.domain.usecase

import androidx.paging.PagingData
import com.dscorp.ispadmin.domain.model.InstallationOrder
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface InstallationOrderUseCase {
    suspend fun createInstallationOrder(installationOrder: InstallationOrder): InstallationOrder
    suspend fun assignTechnician(
        orderId: Int,
        technicianId: Int,
        assignedById: Int,
        scheduledDate: LocalDateTime
    ): InstallationOrder

    suspend fun getInstallationOrderById(id: Int): InstallationOrder
    suspend fun closeInstallationOrder(orderId: Int): InstallationOrder
    suspend fun cancelInstallationOrder(
        orderId: Int,
        cancellationReason: String?
    ): InstallationOrder
    
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

    /**
     * Transfiere una orden de instalación a otro técnico
     * @param orderId ID de la orden a transferir
     * @param newTechnicianId ID del nuevo técnico
     * @param transferredById ID del usuario que realiza la transferencia
     * @param scheduledDate Nueva fecha programada para la instalación
     * @return La orden de instalación actualizada
     */
    suspend fun transferInstallationOrder(
        orderId: Int,
        newTechnicianId: Int,
        transferredById: Int,
        scheduledDate: LocalDateTime
    ): InstallationOrder
}