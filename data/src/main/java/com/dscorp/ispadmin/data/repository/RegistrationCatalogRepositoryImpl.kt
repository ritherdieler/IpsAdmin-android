package com.dscorp.ispadmin.data.repository

import com.dscorp.ispadmin.data.remote.RegistrationCatalogRemoteDataSource
import com.dscorp.ispadmin.domain.model.RegistrationCatalog
import com.dscorp.ispadmin.domain.repository.RegistrationCatalogRepository
import kotlinx.coroutines.CancellationException

class RegistrationCatalogRepositoryImpl(
    private val remote: RegistrationCatalogRemoteDataSource
) : RegistrationCatalogRepository {

    override suspend fun fetchCatalog(): Result<RegistrationCatalog> {
        return try {
            val plans = remote.fetchPlans()
            val places = remote.fetchPlaces()
            val napBoxes = remote.fetchNapBoxes()
            val onus = fetchOptional { remote.fetchOnus() }
            val coreDevices = fetchOptional { remote.fetchCoreDevices() }
            Result.success(
                RegistrationCatalog(
                    plans = plans,
                    places = places,
                    napBoxes = napBoxes,
                    onus = onus,
                    coreDevices = coreDevices
                )
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private suspend fun <T> fetchOptional(fetch: suspend () -> List<T>): List<T> {
        return try {
            fetch()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            emptyList()
        }
    }
}
