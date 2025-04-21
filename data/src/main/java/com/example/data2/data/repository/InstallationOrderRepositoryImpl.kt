package com.example.data2.data.repository

import com.dscorp.ispadmin.domain.model.InstallationOrder
import com.dscorp.ispadmin.domain.model.InstallationOrderStatus
import com.dscorp.ispadmin.domain.model.User
import java.time.LocalDate

class InstallationOrderRepositoryImpl : InstallationOrderRepository {
    
    // Almacenamiento temporal en memoria para simular persistencia
    private val ordersList = mutableListOf<InstallationOrder>()
    private var nextId = 1
    
    override suspend fun getAllInstallationOrders(): List<InstallationOrder> {
        return ordersList.toList()
    }
    
    override suspend fun getInstallationOrderById(id: Int): InstallationOrder {
        return ordersList.find { it.id == id }
            ?: throw IllegalArgumentException("Orden de instalación con id $id no encontrada")
    }
    
    override suspend fun createInstallationOrder(installationOrder: InstallationOrder): InstallationOrder {
        val newOrder = installationOrder.copy(
            id = nextId++,
            status = InstallationOrderStatus.SOLICITADO
        )
        ordersList.add(newOrder)
        return newOrder
    }
    
    override suspend fun assignTechnician(
        orderId: Int,
        technicianId: Int,
        assignedById: Int,
        scheduledDate: LocalDate
    ): InstallationOrder {
        val order = getInstallationOrderById(orderId)
        val updatedOrder = order.copy(
            technician = User(id = technicianId),
            assignedBy = User(id = assignedById),
            scheduledDate = scheduledDate,
            status = InstallationOrderStatus.EN_CURSO
        )
        
        val index = ordersList.indexOfFirst { it.id == orderId }
        if (index != -1) {
            ordersList[index] = updatedOrder
        }
        
        return updatedOrder
    }
    
    override suspend fun closeInstallationOrder(orderId: Int): InstallationOrder {
        val order = getInstallationOrderById(orderId)
        val updatedOrder = order.copy(status = InstallationOrderStatus.CERRADO)
        
        val index = ordersList.indexOfFirst { it.id == orderId }
        if (index != -1) {
            ordersList[index] = updatedOrder
        }
        
        return updatedOrder
    }
    
    override suspend fun cancelInstallationOrder(orderId: Int, cancellationReason: String?): InstallationOrder {
        val order = getInstallationOrderById(orderId)
        val updatedOrder = order.copy(
            status = InstallationOrderStatus.CANCELADO,
            cancellationReason = cancellationReason
        )
        
        val index = ordersList.indexOfFirst { it.id == orderId }
        if (index != -1) {
            ordersList[index] = updatedOrder
        }
        
        return updatedOrder
    }
} 