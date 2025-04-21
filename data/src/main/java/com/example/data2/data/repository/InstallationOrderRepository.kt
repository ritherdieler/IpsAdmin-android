package com.example.data2.data.repository

import com.dscorp.ispadmin.domain.model.InstallationOrder
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface InstallationOrderRepository {
    suspend fun getAllInstallationOrders(): List<InstallationOrder>
    suspend fun getInstallationOrderById(id: Int): InstallationOrder?
    suspend fun createInstallationOrder(installationOrder: InstallationOrder): InstallationOrder
    suspend fun updateInstallationOrder(installationOrder: InstallationOrder): InstallationOrder
    suspend fun deleteInstallationOrder(id: Int): Boolean
    suspend fun searchInstallationOrders(query: String): Flow<List<InstallationOrder>>
    suspend fun getInstallationOrdersByStatus(status: String): Flow<List<InstallationOrder>>
    suspend fun assignTechnician(
        orderId: Int,
        technicianId: Int,
        assignedById: Int,
        scheduledDate: LocalDate
    ): InstallationOrder
    suspend fun closeInstallationOrder(orderId: Int): InstallationOrder
    suspend fun cancelInstallationOrder(orderId: Int, cancellationReason: String?): InstallationOrder
} 