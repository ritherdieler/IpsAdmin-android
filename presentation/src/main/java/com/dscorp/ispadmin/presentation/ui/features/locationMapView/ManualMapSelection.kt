package com.dscorp.ispadmin.presentation.ui.features.locationMapView

import com.google.android.gms.maps.model.LatLng

internal fun resolveManualMapSelectionTarget(
    mapsConfigured: Boolean,
    searchedOrPinned: LatLng?,
    cameraTarget: LatLng,
): LatLng? {
    return if (mapsConfigured) {
        searchedOrPinned ?: cameraTarget
    } else {
        searchedOrPinned
    }
}
