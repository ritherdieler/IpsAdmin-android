package com.dscorp.ispadmin.data.usecase

import com.dscorp.ispadmin.domain.model.Place
import com.dscorp.ispadmin.domain.usecase.PlaceUseCase
import com.dscorp.ispadmin.data.repository.IRepository

class PlaceUseCaseImpl(private val repository: IRepository) : PlaceUseCase {
    
    override suspend fun getPlaces(): List<Place> {
        return repository.getPlaces()
    }

}