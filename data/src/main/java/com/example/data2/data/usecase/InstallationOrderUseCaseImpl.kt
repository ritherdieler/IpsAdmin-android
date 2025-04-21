package com.example.data2.data.usecase

import com.dscorp.ispadmin.domain.model.InstallationOrder
import com.dscorp.ispadmin.domain.model.InstallationOrderStatus
import com.dscorp.ispadmin.domain.usecase.InstallationOrderUseCase
import com.example.data2.data.repository.InstallationOrderRepository
import java.time.LocalDate

class InstallationOrderUseCaseImpl(
    private val repository: InstallationOrderRepository
) : InstallationOrderUseCase {
    
    override suspend fun getAllInstallationOrders(): List<InstallationOrder> {
        return repository.getAllInstallationOrders()
    }
    
    override suspend fun getInstallationOrderById(id: Int): InstallationOrder {
        return repository.getInstallationOrderById(id) 
            ?: throw IllegalArgumentException("Installation order with id $id not found")
    }
    
    override suspend fun getInstallationOrdersByStatus(status: InstallationOrderStatus): List<InstallationOrder> {
        return repository.getInstallationOrdersByStatus(status)
    }
    
    override suspend fun createInstallationOrder(installationOrder: InstallationOrder): InstallationOrder {
        return repository.createInstallationOrder(installationOrder)
    }
    
    override suspend fun assignTechnician(
        orderId: Int,
        technicianId: Int,
        assignedById: Int,
        scheduledDate: LocalDate
    ): InstallationOrder {
        return repository.assignTechnician(orderId, technicianId, assignedById, scheduledDate)
    }
    
    override suspend fun closeInstallationOrder(orderId: Int): InstallationOrder {
        return repository.closeInstallationOrder(orderId)
    }
    
    override suspend fun cancelInstallationOrder(orderId: Int, cancellationReason: String?): InstallationOrder {
        return repository.cancelInstallationOrder(orderId, cancellationReason)
    }
} 