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
import retrofit2.Response
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class InstallationOrderRepositoryImpl : InstallationOrderRepository, KoinComponent {
    
    private val apiService: InstallationOrderApiService by inject()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // Método genérico para manejar códigos de respuesta HTTP
    private fun <T> handleResponse(response: Response<T>, resourceName: String): T {
        val code = response.code()
        return when {
            // 2xx - Éxito
            code in 200..299 -> response.body() ?: throw Exception("Respuesta vacía de $resourceName")
            
            // 3xx - Redirección (normalmente no deberían ocurrir)
            code in 300..399 -> throw Exception("$resourceName - Redirección no manejada: $code")
            
            // 4xx - Errores de cliente
            code == 400 -> throw Exception("$resourceName - Petición incorrecta")
            code == 401 -> throw Exception("$resourceName - No autorizado")
            code == 403 -> throw Exception("$resourceName - Prohibido")
            code == 404 -> throw Exception("$resourceName - No encontrado")
            code == 409 -> throw Exception("$resourceName - Conflicto con el estado actual")
            code in 400..499 -> throw Exception("$resourceName - Error de cliente: $code")
            
            // 5xx - Errores de servidor
            code in 500..599 -> throw Exception("$resourceName - Error de servidor: $code")
            
            // Otros códigos no estándar
            else -> throw Exception("$resourceName - Código de respuesta inesperado: $code")
        }
    }

    override suspend fun getAllInstallationOrders(): Flow<List<InstallationOrder>> = flow {
        try {
            val response = withContext(Dispatchers.IO) {
                apiService.getAllInstallationOrders()
            }
            
            val code = response.code()
            when {
                code in 200..299 -> emit(response.body() ?: emptyList())
                code == 404 -> emit(emptyList())
                else -> {
                    println("Error al obtener órdenes de instalación: $code")
                    emit(emptyList())
                }
            }
        } catch (e: Exception) {
            println("Excepción al obtener órdenes de instalación: ${e.message}")
            emit(emptyList())
        }
    }
    
    override suspend fun getInstallationOrderById(id: Int): InstallationOrder? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getInstallationOrderById(id)
            
            val code = response.code()
            when {
                code in 200..299 -> response.body()
                code == 404 -> null
                else -> {
                    println("Error al obtener orden de instalación $id: $code")
                    null
                }
            }
        } catch (e: Exception) {
            println("Excepción al obtener orden de instalación $id: ${e.message}")
            null
        }
    }
    
    override suspend fun createInstallationOrder(installationOrder: InstallationOrder): InstallationOrder = withContext(Dispatchers.IO) {
        val response = apiService.createInstallationOrder(installationOrder)
        handleResponse(response, "Crear orden de instalación")
    }
    
    override suspend fun updateInstallationOrder(installationOrder: InstallationOrder): InstallationOrder = withContext(Dispatchers.IO) {
        val response = apiService.createInstallationOrder(installationOrder)
        handleResponse(response, "Actualizar orden de instalación")
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
        handleResponse(response, "Asignar técnico a orden $orderId")
    }
    
    override suspend fun closeInstallationOrder(orderId: Int): InstallationOrder = withContext(Dispatchers.IO) {
        val response = apiService.closeInstallationOrder(orderId)
        handleResponse(response, "Cerrar orden de instalación $orderId")
    }
    
    override suspend fun cancelInstallationOrder(orderId: Int, cancellationReason: String?): InstallationOrder = withContext(Dispatchers.IO) {
        val response = apiService.cancelInstallationOrder(orderId, cancellationReason)
        handleResponse(response, "Cancelar orden de instalación $orderId")
    }
} 