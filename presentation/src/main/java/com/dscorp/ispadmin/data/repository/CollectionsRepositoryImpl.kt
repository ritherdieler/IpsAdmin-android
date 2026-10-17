package com.dscorp.ispadmin.data.repository

import com.dscorp.ispadmin.data.datasource.remote.dashboard.ClientsDashboardApi
import com.dscorp.ispadmin.domain.model.dashboard.CollectionSummary
import com.dscorp.ispadmin.domain.repository.CollectionsRepository
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

class CollectionsRepositoryImpl(private val api: ClientsDashboardApi) : CollectionsRepository {
    override suspend fun summary(): Result<CollectionSummary> = try {
        val response = api.collections()
        if (!response.isSuccessful) throw HttpException(response)
        Result.success(response.body()?.toDomain() ?: error("Empty collection summary"))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
