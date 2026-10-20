package com.dscorp.ispadmin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dscorp.ispadmin.data.local.entity.CatalogCoreDeviceEntity
import com.dscorp.ispadmin.data.local.entity.CatalogMetadataEntity
import com.dscorp.ispadmin.data.local.entity.CatalogNapBoxEntity
import com.dscorp.ispadmin.data.local.entity.CatalogOnuEntity
import com.dscorp.ispadmin.data.local.entity.CatalogPlaceEntity
import com.dscorp.ispadmin.data.local.entity.CatalogPlanEntity
import kotlinx.coroutines.flow.Flow

// Unused: the registration catalog now loads straight from the API. Removed in delivery 2.
@Dao
interface CatalogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlans(plans: List<CatalogPlanEntity>)

    @Query("SELECT * FROM catalog_plans")
    suspend fun getPlans(): List<CatalogPlanEntity>

    @Query("DELETE FROM catalog_plans")
    suspend fun clearPlans()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaces(places: List<CatalogPlaceEntity>)

    @Query("SELECT * FROM catalog_places")
    suspend fun getPlaces(): List<CatalogPlaceEntity>

    @Query("DELETE FROM catalog_places")
    suspend fun clearPlaces()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNapBoxes(napBoxes: List<CatalogNapBoxEntity>)

    @Query("SELECT * FROM catalog_nap_boxes")
    suspend fun getNapBoxes(): List<CatalogNapBoxEntity>

    @Query("DELETE FROM catalog_nap_boxes")
    suspend fun clearNapBoxes()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOnus(onus: List<CatalogOnuEntity>)

    @Query("SELECT * FROM catalog_onus")
    suspend fun getOnus(): List<CatalogOnuEntity>

    @Query("DELETE FROM catalog_onus")
    suspend fun clearOnus()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCoreDevices(devices: List<CatalogCoreDeviceEntity>)

    @Query("SELECT * FROM catalog_core_devices")
    suspend fun getCoreDevices(): List<CatalogCoreDeviceEntity>

    @Query("DELETE FROM catalog_core_devices")
    suspend fun clearCoreDevices()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetadata(metadata: CatalogMetadataEntity)

    @Query("SELECT * FROM catalog_metadata WHERE catalogKey = :key")
    suspend fun getMetadata(key: String): CatalogMetadataEntity?

    @Query("SELECT lastSyncedAt FROM catalog_metadata WHERE catalogKey = :key")
    fun observeLastSyncedAt(key: String): Flow<Long?>
}
