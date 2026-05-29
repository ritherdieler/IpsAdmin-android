package com.dscorp.ispadmin.data.repository.adapters

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.NetworkDevice
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.repository.SubscriptionRegistrationQueryRepository

class SubscriptionRegistrationQueryRepositoryAdapter(
    private val repository: IRepository
) : SubscriptionRegistrationQueryRepository {

    override suspend fun getPlaces(): List<Place> = repository.getPlaces()

    override suspend fun getPlaceFromLocation(latitude: Double, longitude: Double): Place =
        repository.getPlaceFromLocation(latitude, longitude)

    override suspend fun getNapBoxes(): List<NapBoxResponse> = repository.getNapBoxes()

    override suspend fun getNapBoxesOrderedByLocation(
        latitude: Double,
        longitude: Double
    ): List<NapBoxResponse> = repository.getNapBoxesOrderedByLocation(latitude, longitude)

    override suspend fun getCoreDevices(): List<NetworkDevice> = repository.getCoreDevices()

    override suspend fun getUnconfirmedOnus(): List<Onu> = repository.getUnconfirmedOnus()
}
