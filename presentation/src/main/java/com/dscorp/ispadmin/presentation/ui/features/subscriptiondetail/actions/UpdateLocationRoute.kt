package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalContext
import com.dscorp.ispadmin.presentation.utils.PermissionUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private const val ZOOM = 18f

/** Wires the location screen to Google Maps and the device GPS. */
@SuppressLint("MissingPermission")
@Composable
fun UpdateLocationRoute(subscriptionId: Int, onDone: (String) -> Unit, onBack: () -> Unit) {
    val viewModel: UpdateLocationViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val camera = rememberCameraPositionState()
    val gps = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(subscriptionId) { viewModel.onIntent(UpdateLocationIntent.Load(subscriptionId)) }
    LaunchedEffect(state.loaded) {
        if (state.loaded) camera.position = CameraPosition.fromLatLngZoom(LatLng(state.latitude, state.longitude), ZOOM)
    }
    LaunchedEffect(camera.isMoving) {
        if (!camera.isMoving && state.loaded) {
            val target = camera.position.target
            viewModel.onIntent(UpdateLocationIntent.CenterMoved(target.latitude, target.longitude))
        }
    }
    LaunchedEffect(state.done) { state.done?.let(onDone) }

    UpdateLocationContent(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        onMyLocation = {
            PermissionUtils.requestLocationPermissionFromContext(
                context = context,
                onPermissionGranted = {
                    gps.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { fix ->
                        if (fix != null) {
                            viewModel.onIntent(UpdateLocationIntent.MyLocation(fix.latitude, fix.longitude, fix.accuracy))
                            scope.launch { camera.animate(CameraUpdateFactory.newLatLngZoom(LatLng(fix.latitude, fix.longitude), ZOOM)) }
                        }
                    }
                },
                onPermissionDenied = {},
            )
        },
        map = {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = camera,
                uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false, myLocationButtonEnabled = false),
            ) {
                val nap = state.nap
                val lat = nap?.latitude
                val lng = nap?.longitude
                if (nap != null && lat != null && lng != null) {
                    Marker(state = MarkerState(LatLng(lat, lng)), title = "${nap.code} · actual")
                }
            }
        },
    )
}
