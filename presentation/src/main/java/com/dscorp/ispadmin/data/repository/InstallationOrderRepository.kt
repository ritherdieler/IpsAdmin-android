package com.dscorp.ispadmin.data.repository

import androidx.paging.PagingData
import com.dscorp.ispadmin.data.model.InstallationOrderStatus
import com.dscorp.ispadmin.domain.model.InstallationOrder
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface InstallationOrderRepository {

    suspend fun getAllInstallationOrders(): List<InstallationOrder>
    suspend fun getInstallationOrderById(id: Int): InstallationOrder?
    suspend fun createInstallationOrder(installationOrder: InstallationOrder): InstallationOrder
    suspend fun updateInstallationOrder(installationOrder: InstallationOrder): InstallationOrder
    suspend fun deleteInstallationOrder(id: Int): Boolean
    suspend fun searchInstallationOrders(query: String): Flow<List<InstallationOrder>>
    suspend fun getInstallationOrdersByStatus(status: InstallationOrderStatus): List<InstallationOrder>
    suspend fun assignTechnician(
        orderId: Int,
        technicianId: Int,
        assignedById: Int,
        scheduledDate: LocalDateTime
    ): InstallationOrder
    suspend fun closeInstallationOrder(orderId: Int): InstallationOrder
    suspend fun cancelInstallationOrder(orderId: Int, cancellationReason: String?): InstallationOrder
    suspend fun getInstallationOrdersByTechnicianAndStatus(userId: Int, status: InstallationOrderStatus): List<InstallationOrder>
    suspend fun getInstallationOrdersBySellerAndStatus(userId: Int, status: InstallationOrderStatus): List<InstallationOrder>
    
    /**
     * Obtiene un flujo de PagingData para las órdenes de instalación.
     * Este método utiliza Paging 3 para cargar las órdenes de instalación de forma paginada.
     *
     * @param userId ID del usuario (técnico, vendedor, etc.)
     * @param status Estado de las órdenes de instalación a recuperar (opcional)
     * @return Flow de PagingData con órdenes de instalación
     */
    fun getPaginatedInstallationOrders(
        userId: Int,
        status: InstallationOrderStatus? = null
    ): Flow<PagingData<InstallationOrder>>
} 