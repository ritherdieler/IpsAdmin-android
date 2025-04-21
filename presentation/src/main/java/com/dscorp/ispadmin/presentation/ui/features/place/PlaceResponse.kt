package com.dscorp.ispadmin.presentation.ui.features.place

import com.dscorp.ispadmin.domain.model.Place

/**
 * Created by Sergio Carrillo Diestra on 20/12/2022.
 * scarrillo.peruapps@gmail.com
 * Peru Apps
 * Huacho, Peru.
 *
 **/
sealed class PlaceResponse {
    class OnPlaceRegister(val place: Place) : PlaceResponse()
    class OnError(val error: Exception) : PlaceResponse()
}
