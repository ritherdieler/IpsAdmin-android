package com.dscorp.ispadmin.data.repository

import com.dscorp.ispadmin.data.local.dao.CatalogDao
import com.dscorp.ispadmin.data.local.entity.CatalogMetadataEntity
import com.dscorp.ispadmin.data.local.mapper.toDomain
import com.dscorp.ispadmin.data.local.mapper.toEntity
import com.dscorp.ispadmin.data.remote.RegistrationCatalogRemoteDataSource
import com.dscorp.ispadmin.domain.exception.CatalogNotAvailableOffline
import com.dscorp.ispadmin.domain.model.RegistrationCatalog
import com.dscorp.ispadmin.domain.repository.RegistrationCatalogRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

class RegistrationCatalogRepositoryImpl(
    private val catalogDao: CatalogDao,
    private val remote: RegistrationCatalogRemoteDataSource
) : RegistrationCatalogRepository {

    override suspend fun getCachedCatalog(): Result<RegistrationCatalog> = runCatching {
        catalogDao.getMetadata(REGISTRATION_CATALOG_KEY) ?: throw CatalogNotAvailableOffline()
        RegistrationCatalog(
            plans = catalogDao.getPlans().map { it.toDomain() },
            places = catalogDao.getPlaces().map { it.toDomain() },
            napBoxes = catalogDao.getNapBoxes().map { it.toDomain() },
            onus = catalogDao.getOnus().map { it.toDomain() },
            coreDevices = catalogDao.getCoreDevices().map { it.toDomain() }
        )
    }

    override suspend fun refreshFromRemote(): Result<Unit> {
        return try {
            val plans = remote.fetchPlans()
            val places = remote.fetchPlaces()
            val napBoxes = remote.fetchNapBoxes()
            val onusResult = fetchOptionalCatalog { remote.fetchOnus() }
            val coreDevicesResult = fetchOptionalCatalog { remote.fetchCoreDevices() }
            val onus = onusResult.getOrElse { emptyList() }
            val coreDevices = coreDevicesResult.getOrElse { emptyList() }

            catalogDao.clearPlans()
            catalogDao.upsertPlans(plans.map { it.toEntity() })
            catalogDao.clearPlaces()
            catalogDao.upsertPlaces(places.map { it.toEntity() })
            catalogDao.clearNapBoxes()
            catalogDao.upsertNapBoxes(napBoxes.map { it.toEntity() })
            catalogDao.clearOnus()
            catalogDao.upsertOnus(onus.map { it.toEntity() })
            catalogDao.clearCoreDevices()
            catalogDao.upsertCoreDevices(coreDevices.map { it.toEntity() })
            catalogDao.upsertMetadata(
                CatalogMetadataEntity(
                    catalogKey = REGISTRATION_CATALOG_KEY,
                    lastSyncedAt = System.currentTimeMillis()
                )
            )
            val partialFailure = onusResult.exceptionOrNull() ?: coreDevicesResult.exceptionOrNull()
            if (partialFailure == null) Result.success(Unit) else Result.failure(partialFailure)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private suspend fun <T> fetchOptionalCatalog(fetch: suspend () -> List<T>): Result<List<T>> {
        return try {
            Result.success(fetch())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override fun observeLastSync(): Flow<Long?> {
        return catalogDao.observeLastSyncedAt(REGISTRATION_CATALOG_KEY)
    }

    companion object {
        const val REGISTRATION_CATALOG_KEY = "registration_catalog"
    }
}
