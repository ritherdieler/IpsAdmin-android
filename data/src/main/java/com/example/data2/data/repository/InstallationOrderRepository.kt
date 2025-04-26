package com.example.data2.data.repository

import com.dscorp.ispadmin.domain.model.InstallationOrder
import com.dscorp.ispadmin.domain.model.InstallationOrderStatus
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
} 