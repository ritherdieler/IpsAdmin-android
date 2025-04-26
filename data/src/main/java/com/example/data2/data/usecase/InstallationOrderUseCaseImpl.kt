package com.example.data2.data.usecase

import com.dscorp.ispadmin.domain.model.InstallationOrder
import com.dscorp.ispadmin.domain.model.InstallationOrderStatus
import com.dscorp.ispadmin.domain.usecase.InstallationOrderUseCase
import com.example.data2.data.repository.InstallationOrderRepository
import java.time.LocalDateTime

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
        scheduledDate: LocalDateTime
    ): InstallationOrder {
        return repository.assignTechnician(orderId, technicianId, assignedById, scheduledDate)
    }

    override suspend fun closeInstallationOrder(orderId: Int): InstallationOrder {
        return repository.closeInstallationOrder(orderId)
    }

    override suspend fun cancelInstallationOrder(
        orderId: Int,
        cancellationReason: String?
    ): InstallationOrder {
        return repository.cancelInstallationOrder(orderId, cancellationReason)
    }

    override suspend fun getInstallationOrdersByTechnicianId(technicianId: String?): List<InstallationOrder> {
        // Si el técnico no tiene ID, retornar lista vacía
        if (technicianId == null) return emptyList()

        // Obtener todas las órdenes y filtrar por técnico
        val allOrders = repository.getAllInstallationOrders()
        return allOrders.filter { it.technician?.id == technicianId.toInt() }
    }



    override suspend fun closeInstallationOrderAsResult(orderId: Int): Result<InstallationOrder> =runCatching {
            repository.closeInstallationOrder(orderId)
        }

    override suspend fun getInstallationOrdersByTechnicianAndStatus(userId: Int, status: InstallationOrderStatus): List<InstallationOrder> {
        return repository.getInstallationOrdersByTechnicianAndStatus(userId, status)
    }

    override suspend fun getInstallationOrdersBySellerAndStatus(userId: Int, status: InstallationOrderStatus): List<InstallationOrder> {
        return repository.getInstallationOrdersBySellerAndStatus(userId, status)
    }

} 
