package com.dscorp.ispadmin.domain.repository

import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.NetworkDevice
import com.dscorp.ispadmin.domain.model.Onu
import com.dscorp.ispadmin.domain.model.Place

interface SubscriptionRegistrationQueryRepository {
    suspend fun getPlaces(): List<Place>

    suspend fun getPlaceFromLocation(latitude: Double, longitude: Double): Place

    suspend fun getNapBoxes(): List<NapBoxResponse>

    suspend fun getNapBoxesOrderedByLocation(latitude: Double, longitude: Double): List<NapBoxResponse>

    suspend fun getCoreDevices(): List<NetworkDevice>

    suspend fun getUnconfirmedOnus(): List<Onu>
}
