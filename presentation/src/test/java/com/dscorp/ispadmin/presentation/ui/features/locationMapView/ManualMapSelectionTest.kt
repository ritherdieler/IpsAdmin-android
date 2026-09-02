package com.dscorp.ispadmin.presentation.ui.features.locationMapView

import com.dscorp.ispadmin.presentation.ui.features.subscription.register.E2ePlaceLocationFixture
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ManualMapSelectionTest {

    @Test
    fun `with maps prefers searched fixture over default camera outside lab polygon`() {
        val searched = LatLng(
            E2ePlaceLocationFixture.LATITUDE.toDouble(),
            E2ePlaceLocationFixture.LONGITUDE.toDouble(),
        )
        val selected = resolveManualMapSelectionTarget(
            mapsConfigured = true,
            searchedOrPinned = searched,
            cameraTarget = DEFAULT_MANUAL_MAP_CAMERA_TARGET,
        )
        assertEquals(searched.latitude, selected!!.latitude, 0.0001)
        assertEquals(searched.longitude, selected.longitude, 0.0001)
    }

    @Test
    fun `without search and without maps key does not select default camera`() {
        val selected = resolveManualMapSelectionTarget(
            mapsConfigured = false,
            searchedOrPinned = null,
            cameraTarget = DEFAULT_MANUAL_MAP_CAMERA_TARGET,
        )
        assertNull(selected)
    }

    @Test
    fun `with maps and no search falls back to camera center`() {
        val selected = resolveManualMapSelectionTarget(
            mapsConfigured = true,
            searchedOrPinned = null,
            cameraTarget = DEFAULT_MANUAL_MAP_CAMERA_TARGET,
        )
        assertEquals(DEFAULT_MANUAL_MAP_CAMERA_TARGET, selected)
    }
}
