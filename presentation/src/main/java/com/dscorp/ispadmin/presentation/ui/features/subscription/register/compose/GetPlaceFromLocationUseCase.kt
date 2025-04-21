package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import com.dscorp.ispadmin.domain.model.Place
import com.example.data2.data.repository.IRepository

class GetPlaceFromLocationUseCase(val repository: IRepository) {

    suspend operator fun invoke(latitude: Double, longitude: Double): Result<Place> =
        runCatching {
            repository.getPlaceFromLocation(latitude, longitude)
        }

}