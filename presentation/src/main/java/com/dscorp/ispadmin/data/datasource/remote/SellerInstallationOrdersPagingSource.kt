package com.dscorp.ispadmin.data.datasource.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.dscorp.ispadmin.data.extensions.toDomain
import com.dscorp.ispadmin.data.repository.InstallationOrderRepositoryImpl.Companion.PAGE_SIZE
import com.dscorp.ispadmin.domain.model.InstallationOrder
import retrofit2.HttpException
import java.io.IOException

/**
 * PagingSource para cargar órdenes de instalación por sellerId.
 */
class SellerInstallationOrdersPagingSource(
    private val api: InstallationOrderApi,
    private val sellerId: Int
) : PagingSource<Int, InstallationOrder>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, InstallationOrder> {
        val page = params.key ?: 0 // Página inicial

        return try {
            val response = api.getInstallationOrdersBySellerPaginated(
                sellerId = sellerId,
                page = page,
                size = PAGE_SIZE
            )

            val orders = response.content.map { it.toDomain() }

            LoadResult.Page(
                data = orders,
                prevKey = if (page > 0) page - 1 else null,
                nextKey = if (orders.isEmpty()) null else page + 1
            )
        } catch (e: IOException) {
            return LoadResult.Error(e)
        } catch (e: HttpException) {
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, InstallationOrder>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

} 