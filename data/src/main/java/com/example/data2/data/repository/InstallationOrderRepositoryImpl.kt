package com.example.data2.data.repository

import com.dscorp.ispadmin.domain.model.InstallationOrder
import com.dscorp.ispadmin.domain.model.InstallationOrderStatus
import com.example.data2.data.apirequestmodel.AssignTechnicianRequest
import com.example.data2.data.datasource.InstallationOrderApiService
import com.example.data2.data.utils.HttpCodes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class InstallationOrderRepositoryImpl : InstallationOrderRepository, KoinComponent {
    
    private val apiService: InstallationOrderApiService by inject()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override suspend fun getAllInstallationOrders(): Flow<List<InstallationOrder>> = flow {
        val response = withContext(Dispatchers.IO) {
            apiService.getAllInstallationOrders()
        }
        
        when (response.code()) {
            HttpCodes.OK -> emit(response.body() ?: emptyList())
            else -> emit(emptyList())
        }
    }
    
    override suspend fun getInstallationOrderById(id: Int): InstallationOrder? = withContext(Dispatchers.IO) {
        val response = apiService.getInstallationOrderById(id)
        
        when (response.code()) {
            HttpCodes.OK -> response.body()
            else -> null
        }
    }
    
    override suspend fun createInstallationOrder(installationOrder: InstallationOrder): InstallationOrder = withContext(Dispatchers.IO) {
        val response = apiService.createInstallationOrder(installationOrder)
        
        when (response.code()) {
            HttpCodes.OK -> response.body() 
                ?: throw Exception("Error al crear la orden de instalación")
            else -> throw Exception("Error al crear la orden de instalación: ${response.code()}")
        }
    }
    
    override suspend fun updateInstallationOrder(installationOrder: InstallationOrder): InstallationOrder = withContext(Dispatchers.IO) {
        // Aquí asumimos que el endpoint de actualización acepta el objeto completo, similar a createInstallationOrder
        val response = apiService.createInstallationOrder(installationOrder)
        
        when (response.code()) {
            HttpCodes.OK -> response.body()
                ?: throw Exception("Error al actualizar la orden de instalación")
            HttpCodes.NOT_FOUND -> throw IllegalArgumentException("Installation order with id ${installationOrder.id} not found")
            else -> throw Exception("Error al actualizar la orden de instalación: ${response.code()}")
        }
    }
    
    override suspend fun deleteInstallationOrder(id: Int): Boolean = withContext(Dispatchers.IO) {
        // Este método requeriría un endpoint específico de eliminación
        // Como no está en la interfaz, lanzamos una excepción o devolvemos false
        throw NotImplementedError("La eliminación de órdenes de instalación no está implementada en la API")
    }
    
    override suspend fun searchInstallationOrders(query: String): Flow<List<InstallationOrder>> = flow {
        // Este método requeriría un endpoint específico de búsqueda
        // Como no está en la interfaz, podríamos obtener todos y filtrar, pero esto no es eficiente
        // Mejor lanzar una excepción o devolver vacío
        emit(emptyList())
    }
    
    override suspend fun getInstallationOrdersByStatus(status: String): Flow<List<InstallationOrder>> = flow {
        // Este método requeriría un endpoint específico de filtrado por estado
        // Como no está en la interfaz, podríamos obtener todos y filtrar, pero no es eficiente
        // Mejor lanzar una excepción o devolver vacío
        emit(emptyList())
    }
    
    override suspend fun assignTechnician(
        orderId: Int,
        technicianId: Int,
        assignedById: Int,
        scheduledDate: LocalDate
    ): InstallationOrder = withContext(Dispatchers.IO) {
        val request = AssignTechnicianRequest(
            technicianId = technicianId,
            assignedById = assignedById,
            scheduledDate = scheduledDate.format(formatter)
        )
        
        val response = apiService.assignTechnician(orderId, request)
        
        when (response.code()) {
            HttpCodes.OK -> response.body()
                ?: throw Exception("Error al asignar técnico a la orden")
            HttpCodes.NOT_FOUND -> throw Exception("Orden de instalación con id $orderId no encontrada")
            else -> throw Exception("Error al asignar técnico a la orden: ${response.code()}")
        }
    }
    
    override suspend fun closeInstallationOrder(orderId: Int): InstallationOrder = withContext(Dispatchers.IO) {
        val response = apiService.closeInstallationOrder(orderId)
        
        when (response.code()) {
            HttpCodes.OK -> response.body()
                ?: throw Exception("Error al cerrar la orden de instalación")
            HttpCodes.NOT_FOUND -> throw Exception("Orden de instalación con id $orderId no encontrada")
            else -> throw Exception("Error al cerrar la orden de instalación: ${response.code()}")
        }
    }
    
    override suspend fun cancelInstallationOrder(orderId: Int, cancellationReason: String?): InstallationOrder = withContext(Dispatchers.IO) {
        val response = apiService.cancelInstallationOrder(orderId, cancellationReason)
        
        when (response.code()) {
            HttpCodes.OK -> response.body()
                ?: throw Exception("Error al cancelar la orden de instalación")
            HttpCodes.NOT_FOUND -> throw Exception("Orden de instalación con id $orderId no encontrada")
            else -> throw Exception("Error al cancelar la orden de instalación: ${response.code()}")
        }
    }
} 