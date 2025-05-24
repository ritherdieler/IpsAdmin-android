package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import com.dscorp.ispadmin.data.repository.IRepository
import com.dscorp.ispadmin.domain.model.NapBoxResponse

class GetNearNapBoxesUseCase(val repository: IRepository) {

    suspend operator fun invoke(latitude: Double, longitude: Double): Result<List<NapBoxResponse>> =
        runCatching {
            repository.getNapBoxesOrderedByLocation(latitude, longitude)
        }

}
