package com.example.data2.data.repository

import com.dscorp.ispadmin.domain.model.InstallationOrder
import com.dscorp.ispadmin.domain.model.InstallationOrderStatus
import com.example.data2.data.datasource.InstallationOrderApiService
import com.example.data2.data.repository.util.handleResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class InstallationOrderRepositoryImpl : InstallationOrderRepository, KoinComponent {

    private val apiService: InstallationOrderApiService by inject()
    private val formatter = DateTimeFormatter.ISO_DATE_TIME



    override suspend fun getAllInstallationOrders(): List<InstallationOrder> =
        withContext(Dispatchers.IO) {
            val response = apiService.getAllInstallationOrders()
            handleResponse(response, "Obtener todas las órdenes de instalación")
        }


    override suspend fun getInstallationOrderById(id: Int): InstallationOrder? =
        withContext(Dispatchers.IO) {
            val response = apiService.getInstallationOrderById(id)
            handleResponse(response, "Obtener orden de instalación $id")
        }

    override suspend fun getInstallationOrdersByStatus(status: InstallationOrderStatus): List<InstallationOrder> =
        withContext(Dispatchers.IO) {
            val response = apiService.getInstallationOrdersByStatus(status)
            handleResponse(response, "Obtener órdenes de instalación por estado: $status")
        }

    override suspend fun createInstallationOrder(installationOrder: InstallationOrder): InstallationOrder =
        withContext(Dispatchers.IO) {
            val response = apiService.createInstallationOrder(installationOrder)
            handleResponse(response, "Crear orden de instalación")
        }

    override suspend fun updateInstallationOrder(installationOrder: InstallationOrder): InstallationOrder =
        withContext(Dispatchers.IO) {
            val response = apiService.createInstallationOrder(installationOrder)
            handleResponse(response, "Actualizar orden de instalación")
        }

    override suspend fun deleteInstallationOrder(id: Int): Boolean = withContext(Dispatchers.IO) {
        // Este método requeriría un endpoint específico de eliminación
        // Como no está en la interfaz, lanzamos una excepción o devolvemos false
        throw NotImplementedError("La eliminación de órdenes de instalación no está implementada en la API")
    }

    override suspend fun searchInstallationOrders(query: String): Flow<List<InstallationOrder>> =
        flow {
            // Este método requeriría un endpoint específico de búsqueda
            // Como no está en la interfaz, podríamos obtener todos y filtrar, pero esto no es eficiente
            // Mejor lanzar una excepción o devolver vacío
            emit(emptyList())
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

    override suspend fun getInstallationOrdersByTechnicianAndStatus(
        userId: Int, 
        status: InstallationOrderStatus
    ): List<InstallationOrder> = withContext(Dispatchers.IO) {
        val response = apiService.getInstallationOrdersByTechnicianAndStatus(userId, status)
        handleResponse(response, "Obtener órdenes de instalación por usuario $userId y estado $status")
    }
} 