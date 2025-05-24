package com.dscorp.ispadmin.data.datasource.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.dscorp.ispadmin.data.model.InstallationOrderDto
import com.dscorp.ispadmin.data.model.InstallationOrderStatus
import com.dscorp.ispadmin.domain.model.InstallationOrder

/**
 * PagingSource para las órdenes de instalación.
 * Se encarga de cargar páginas de órdenes de instalación desde la API.
 */
class InstallationOrderPagingSource(
    private val api: InstallationOrderApi,
    private val userId: Int,
    private val status: InstallationOrderStatus?
) : PagingSource<Int, InstallationOrder>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, InstallationOrder> {
        try {
            // Si es null, usamos 0 como página inicial
            val page = params.key ?: 0
            
            // Obtenemos la respuesta paginada desde la API
            val response = api.getPaginatedInstallationOrders(
                userId = userId,
                status = status,
                page = page,
                size = 10
            )
            
            // Convertimos los DTOs a modelos de dominio
            val orders = response.content.map { dto ->
                dto.toDomainModel()
            }
            
            return LoadResult.Page(
                data = orders,
                prevKey = if (page > 0) page - 1 else null,
                nextKey = if (page < response.totalPages - 1) page + 1 else null
            )
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, InstallationOrder>): Int? {
        // Implementamos la lógica para obtener la clave de actualización
        // Esto ayuda a Paging a decidir qué página cargar después de una actualización
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
    
    /**
     * Extensión para convertir un DTO a un modelo de dominio
     */
    private fun InstallationOrderDto.toDomainModel(): InstallationOrder {
        return InstallationOrder(
            id = id,
            customerFirstName = customerFirstName,
            customerLastName = customerLastName,
            customerAddress = customerAddress,
            customerPhone = customerPhone,
            status = status,
            scheduledDate = scheduledDate,
            // Otros campos pueden ser nulos o tener valores por defecto
            // dependiendo de la estructura del modelo de dominio
        )
    }
} 