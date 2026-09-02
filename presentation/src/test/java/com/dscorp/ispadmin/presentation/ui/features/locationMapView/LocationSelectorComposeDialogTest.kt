package com.dscorp.ispadmin.presentation.ui.features.locationMapView

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.E2ePlaceLocationFixture
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LocationSelectorComposeDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `missing maps key still returns pasted coordinates inside place polygon`() {
        var selected: LatLng? = null

        composeRule.setContent {
            MaterialTheme {
                LocationSelectorComposeDialog(
                    initialLocation = null,
                    onLocationSelected = { selected = it },
                    onDismiss = {},
                    enableMyLocation = false,
                    mapsConfigured = false,
                )
            }
        }

        composeRule.onNodeWithTag("map_missing_api_key").assertIsDisplayed()
        composeRule.onNodeWithTag("map_coordinate_search").performTextInput(
            E2ePlaceLocationFixture.pastedCoordinates()
        )
        composeRule.onNodeWithTag("map_coordinate_search_button").performClick()
        composeRule.onNodeWithTag("map_select_location_button").performClick()

        assertEquals(E2ePlaceLocationFixture.LATITUDE.toDouble(), selected!!.latitude, 0.0001)
        assertEquals(E2ePlaceLocationFixture.LONGITUDE.toDouble(), selected!!.longitude, 0.0001)
    }

    @Test
    fun `does not treat default camera as a selected location`() {
        var selected: LatLng? = null

        composeRule.setContent {
            MaterialTheme {
                LocationSelectorComposeDialog(
                    initialLocation = null,
                    onLocationSelected = { selected = it },
                    onDismiss = {},
                    enableMyLocation = false,
                    mapsConfigured = false,
                )
            }
        }

        assertEquals(null, selected)
        composeRule.onNodeWithTag("map_select_location_button").performClick()
        assertEquals(null, selected)
    }
}
