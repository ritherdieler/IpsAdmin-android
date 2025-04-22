package com.dscorp.ispadmin.domain.usecase

import com.dscorp.ispadmin.domain.model.InstallationOrder
import com.dscorp.ispadmin.domain.model.InstallationOrderStatus
import java.time.LocalDateTime

interface InstallationOrderUseCase {
    suspend fun getAllInstallationOrders(): List<InstallationOrder>
    suspend fun getInstallationOrderById(id: Int): InstallationOrder
    suspend fun createInstallationOrder(installationOrder: InstallationOrder): InstallationOrder
    suspend fun assignTechnician(
        orderId: Int, 
        technicianId: Int, 
        assignedById: Int, 
        scheduledDate: LocalDateTime
    ): InstallationOrder
    suspend fun closeInstallationOrder(orderId: Int): InstallationOrder
    suspend fun cancelInstallationOrder(orderId: Int, cancellationReason: String?): InstallationOrder
    suspend fun getInstallationOrdersByStatus(status: InstallationOrderStatus): List<InstallationOrder>
} 