package com.dscorp.ispadmin.presentation.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun MiniMap(
    modifier: Modifier = Modifier,
    location: LatLng?,
    title: String = "Location"
) {
    val defaultLocation = LatLng(0.0, 0.0) // Fallback location
    val targetLocation = location ?: defaultLocation
    val zoomLevel = 16f // Increased zoom level
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(targetLocation, zoomLevel)
    }
    
    // Update camera when location changes
    LaunchedEffect(location) {
        location?.let {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, zoomLevel))
        }
    }
    
    // Disable all gestures and controls for a static map view
    val uiSettings by remember { mutableStateOf(MapUiSettings(
        zoomControlsEnabled = false,
        zoomGesturesEnabled = false,
        scrollGesturesEnabled = false,
        tiltGesturesEnabled = false,
        mapToolbarEnabled = false
    ))}
    val properties by remember { mutableStateOf(MapProperties(mapType = MapType.HYBRID)) }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp) // Fixed height for the mini map
            .clip(RoundedCornerShape(8.dp)) 
    ) {
        GoogleMap(
            modifier = Modifier.matchParentSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = uiSettings,
            properties = properties
        ) {
            // Add a marker only if the location is valid (not the default)
            if (location != null && location != defaultLocation) {
                Marker(
                    state = MarkerState(position = targetLocation),
                    title = title
                )
            }
        }
    }
} 