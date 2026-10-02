package com.dscorp.ispadmin.presentation.ui.features.locationMapView

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.android.gms.maps.model.LatLng
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class LocationSelectorSnapshotConcurrencyTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun manualCoordinateSelection_keepsComposeStateConsistent() {
        val selectedLocation = AtomicReference<LatLng?>(null)
        composeRule.activity.setTheme(com.dscorp.ispadmin.R.style.Theme_IspAdminAndroid)
        composeRule.setContent {
            MaterialTheme {
                LocationSelectorComposeDialog(
                    initialLocation = null,
                    onLocationSelected = selectedLocation::set,
                    onDismiss = {},
                    enableMyLocation = false,
                    mapsConfigured = true,
                )
            }
        }

        composeRule.onNodeWithTag("map_coordinate_search")
            .performTextInput("-11.233, -77.3766")
        composeRule.onNodeWithTag("map_coordinate_search_button").performClick()
        composeRule.onNodeWithTag("map_select_location_button").performClick()

        val location = requireNotNull(selectedLocation.get())
        assertEquals(-11.233, location.latitude, 0.0001)
        assertEquals(-77.3766, location.longitude, 0.0001)
    }
}
