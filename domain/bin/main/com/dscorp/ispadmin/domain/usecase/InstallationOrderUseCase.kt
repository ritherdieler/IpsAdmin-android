package com.dscorp.ispadmin.domain.usecase

import com.dscorp.ispadmin.domain.model.InstallationOrder
import java.time.LocalDate

interface InstallationOrderUseCase {
    suspend fun getAllInstallationOrders(): List<InstallationOrder>
    suspend fun getInstallationOrderById(id: Int): InstallationOrder
    suspend fun createInstallationOrder(installationOrder: InstallationOrder): InstallationOrder
    suspend fun assignTechnician(
        orderId: Int, 
        technicianId: Int, 
        assignedById: Int, 
        scheduledDate: LocalDate
    ): InstallationOrder
    suspend fun closeInstallationOrder(orderId: Int): InstallationOrder
    suspend fun cancelInstallationOrder(orderId: Int, cancellationReason: String?): InstallationOrder
} 