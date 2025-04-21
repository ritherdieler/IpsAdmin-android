package com.dscorp.ispadmin.presentation.ui.features.place.placelist

import com.dscorp.ispadmin.domain.model.PlaceResponse

/**
 * Created by Sergio Carrillo Diestra on 19/12/2022.
 * scarrillo.peruapps@gmail.com
 * Peru Apps
 * Huacho, Peru.
 *
 **/
sealed class PlaceListResponse {
    class OnPlaceListFound(val placeList: List<PlaceResponse>) : PlaceListResponse()
    class OnError(val error: Exception) : PlaceListResponse()
}
