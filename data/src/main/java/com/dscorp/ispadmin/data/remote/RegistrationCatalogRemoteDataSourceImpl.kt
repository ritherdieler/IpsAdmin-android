package com.dscorp.ispadmin.data.remote

import com.dscorp.ispadmin.domain.model.CatalogCoreDevice
import com.dscorp.ispadmin.domain.model.CatalogNapBox
import com.dscorp.ispadmin.domain.model.CatalogOnu
import com.dscorp.ispadmin.domain.model.CatalogPlace
import com.dscorp.ispadmin.domain.model.CatalogPlan

class RegistrationCatalogRemoteDataSourceImpl(
    private val api: RegistrationCatalogApi
) : RegistrationCatalogRemoteDataSource {

    override suspend fun fetchPlans(): List<CatalogPlan> {
        return api.getPlans().requireBody().mapNotNull { dto ->
            val id = dto.id ?: return@mapNotNull null
            CatalogPlan(
                id = id,
                name = dto.name,
                price = dto.price,
                downloadSpeed = dto.downloadSpeed,
                uploadSpeed = dto.uploadSpeed,
                type = dto.type
            )
        }
    }

    override suspend fun fetchPlaces(): List<CatalogPlace> {
        return api.getPlaces().requireBody().mapNotNull { dto ->
            val id = dto.id ?: return@mapNotNull null
            CatalogPlace(
                id = id,
                name = dto.name,
                latitude = dto.latitude,
                longitude = dto.longitude
            )
        }
    }

    override suspend fun fetchNapBoxes(): List<CatalogNapBox> {
        return api.getNapBoxes().requireBody().mapNotNull { dto ->
            val id = dto.id ?: return@mapNotNull null
            CatalogNapBox(
                id = id,
                code = dto.code,
                address = dto.address,
                mufaId = dto.mufaId,
                latitude = dto.latitude,
                longitude = dto.longitude,
                portsNumber = dto.ports_number,
                placeName = dto.placeName,
                placeId = dto.placeId
            )
        }
    }

    override suspend fun fetchOnus(): List<CatalogOnu> {
        return api.getUnconfiguredOnus().requireBody().mapNotNull { dto ->
            if (dto.sn.isBlank()) return@mapNotNull null
            CatalogOnu(
                sn = dto.sn,
                board = dto.board,
                oltId = dto.olt_id,
                onu = dto.onu,
                onuTypeId = dto.onu_type_id,
                onuTypeName = dto.onu_type_name,
                ponType = dto.pon_type,
                port = dto.port
            )
        }
    }

    override suspend fun fetchCoreDevices(): List<CatalogCoreDevice> {
        return api.getCoreDevices().requireBody().mapNotNull { dto ->
            val id = dto.id ?: return@mapNotNull null
            CatalogCoreDevice(
                id = id,
                name = dto.name,
                password = "",
                username = "",
                ipAddress = dto.ipAddress,
                networkDeviceType = dto.networkDeviceType,
                disabled = dto.disabled
            )
        }
    }

    private fun <T> retrofit2.Response<List<T>>.requireBody(): List<T> {
        if (!isSuccessful) {
            error("Error al obtener catálogo remoto: ${code()}")
        }
        return body().orEmpty()
    }
}
