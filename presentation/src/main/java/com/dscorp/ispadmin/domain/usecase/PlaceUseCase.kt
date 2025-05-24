package com.dscorp.ispadmin.domain.usecase

import com.dscorp.ispadmin.domain.model.Place

interface PlaceUseCase {
    suspend fun getPlaces(): List<Place>

}