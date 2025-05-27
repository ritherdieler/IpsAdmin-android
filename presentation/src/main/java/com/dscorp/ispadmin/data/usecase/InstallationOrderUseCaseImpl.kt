package com.dscorp.ispadmin.data.usecase

import androidx.paging.PagingData
import com.dscorp.ispadmin.data.repository.InstallationOrderRepository
import com.dscorp.ispadmin.domain.model.InstallationOrder
import com.dscorp.ispadmin.domain.usecase.InstallationOrderUseCase
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

class InstallationOrderUseCaseImpl(
    private val repository: InstallationOrderRepository
) : InstallationOrderUseCase {


    override suspend fun getInstallationOrderById(id: Int): InstallationOrder {
        return repository.getInstallationOrderById(id)
            ?: throw IllegalArgumentException("Installation order with id $id not found")
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

    override suspend fun transferInstallationOrder(
        orderId: Int,
        newTechnicianId: Int,
        transferredById: Int,
        scheduledDate: LocalDateTime
    ): InstallationOrder {
        return repository.transferInstallationOrder(
            orderId = orderId,
            newTechnicianId = newTechnicianId,
            transferredById = transferredById,
            scheduledDate = scheduledDate
        )
    }
    
    /**
     * Obtiene todas las órdenes de instalación sin filtros de forma paginada
     */
    override fun getAllInstallationOrdersPaginated(): Flow<PagingData<InstallationOrder>> {
        return repository.getAllInstallationOrdersPaginated()
    }
    
    /**
     * Obtiene las órdenes de instalación de un vendedor específico de forma paginada
     */
    override fun getInstallationOrdersBySellerPaginated(sellerId: Int): Flow<PagingData<InstallationOrder>> {
        return repository.getInstallationOrdersBySellerPaginated(sellerId)
    }
    
    /**
     * Obtiene las órdenes de instalación de un técnico específico de forma paginada
     */
    override fun getInstallationOrdersByTechnicianPaginated(technicianId: Int): Flow<PagingData<InstallationOrder>> {
        return repository.getInstallationOrdersByTechnicianPaginated(technicianId)
    }
}
