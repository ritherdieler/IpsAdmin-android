package com.dscorp.ispadmin.domain.usecase

import com.dscorp.ispadmin.data.model.InstallationOrderStatus
import com.dscorp.ispadmin.domain.model.InstallationOrder
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
    suspend fun getInstallationOrdersByTechnicianId(technicianId: String?): List<InstallationOrder>
    
    // Nuevos métodos que retornan Result
    suspend fun closeInstallationOrderAsResult(orderId: Int): Result<InstallationOrder>
    suspend fun getInstallationOrdersByTechnicianAndStatus(userId: Int, status: InstallationOrderStatus): List<InstallationOrder>
    suspend fun getInstallationOrdersBySellerAndStatus(userId: Int, status: InstallationOrderStatus): List<InstallationOrder>
} 