package com.dscorp.ispadmin.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.dscorp.ispadmin.data.datasource.remote.AllInstallationOrdersPagingSource
import com.dscorp.ispadmin.data.datasource.remote.InstallationOrderApi
import com.dscorp.ispadmin.data.datasource.remote.InstallationOrderApiService
import com.dscorp.ispadmin.data.datasource.remote.SellerInstallationOrdersPagingSource
import com.dscorp.ispadmin.data.datasource.remote.TechnicianInstallationOrdersPagingSource
import com.dscorp.ispadmin.data.repository.util.handleResponse
import com.dscorp.ispadmin.domain.model.InstallationOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class InstallationOrderRepositoryImpl(
    private val apiService: InstallationOrderApiService,
    private val installationOrderApi: InstallationOrderApi
) : InstallationOrderRepository, KoinComponent {

    private val formatter = DateTimeFormatter.ISO_DATE_TIME

    override suspend fun getInstallationOrderById(id: Int): InstallationOrder? =
        withContext(Dispatchers.IO) {
            val response = apiService.getInstallationOrderById(id)
            handleResponse(response, "Obtener orden de instalación $id")
        }

    override suspend fun createInstallationOrder(installationOrder: InstallationOrder): InstallationOrder =
        withContext(Dispatchers.IO) {
            val response = apiService.createInstallationOrder(installationOrder)
            handleResponse(response, "Crear orden de instalación")
        }

    override suspend fun assignTechnician(
        orderId: Int,
        technicianId: Int,
        assignedById: Int,
        scheduledDate: LocalDateTime
    ): InstallationOrder = withContext(Dispatchers.IO) {

        val response = apiService.assignTechnician(orderId,
            technicianId = technicianId,
            assignedById = assignedById,
            scheduledDateTime = scheduledDate.format(formatter), )
        handleResponse(response, "Asignar técnico a orden $orderId")
    }

    override suspend fun closeInstallationOrder(orderId: Int): InstallationOrder =
        withContext(Dispatchers.IO) {
            val response = apiService.closeInstallationOrder(orderId)
            handleResponse(response, "Cerrar orden de instalación $orderId")
        }

    override suspend fun cancelInstallationOrder(
        orderId: Int,
        cancellationReason: String?
    ): InstallationOrder = withContext(Dispatchers.IO) {
        val response = apiService.cancelInstallationOrder(orderId, cancellationReason)
        handleResponse(response, "Cancelar orden de instalación $orderId")
    }

    override suspend fun transferInstallationOrder(
        orderId: Int,
        newTechnicianId: Int,
        transferredById: Int,
        scheduledDate: LocalDateTime
    ): InstallationOrder = withContext(Dispatchers.IO) {
        val response = apiService.transferInstallationOrder(
            orderId = orderId,
            newTechnicianId = newTechnicianId,
            transferredById = transferredById,
            scheduledDateTime = scheduledDate.format(formatter)
        )
        handleResponse(response, "Transferir orden de instalación $orderId")
    }

    /**
     * Obtiene todas las órdenes de instalación sin filtros de forma paginada
     */
    override fun getAllInstallationOrdersPaginated(): Flow<PagingData<InstallationOrder>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                maxSize = MAX_SIZE,
                prefetchDistance = PREFETCH_DISTANCE
            ),
            pagingSourceFactory = {
                AllInstallationOrdersPagingSource(api = installationOrderApi)
            }
        ).flow
    }

    /**
     * Obtiene las órdenes de instalación de un vendedor específico de forma paginada
     */
    override fun getInstallationOrdersBySellerPaginated(sellerId: Int): Flow<PagingData<InstallationOrder>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                maxSize = MAX_SIZE,
                prefetchDistance = PREFETCH_DISTANCE
            ),
            pagingSourceFactory = {
                SellerInstallationOrdersPagingSource(api = installationOrderApi, sellerId = sellerId)
            }
        ).flow
    }

    /**
     * Obtiene las órdenes de instalación de un técnico específico de forma paginada
     */
    override fun getInstallationOrdersByTechnicianPaginated(technicianId: Int): Flow<PagingData<InstallationOrder>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                maxSize = MAX_SIZE,
                prefetchDistance = PREFETCH_DISTANCE
            ),
            pagingSourceFactory = {
                TechnicianInstallationOrdersPagingSource(api = installationOrderApi, technicianId = technicianId)
            }
        ).flow
    }
    
    companion object {
         const val PAGE_SIZE = 10
        private const val MAX_SIZE = 100
        private const val PREFETCH_DISTANCE = 5
    }
}