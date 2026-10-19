package com.dscorp.ispadmin.presentation.ui.features.networkmap

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

private const val BOUNDS_PADDING_PX = 120

/** «Mapa de red» on Google Maps: one pin per box with its free ports, the mufas, and the selected box card. */
@Composable
fun NetworkMapRoute(onOpenBox: (Int) -> Unit) {
    val viewModel: NetworkMapViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val camera = rememberCameraPositionState()
    var mapLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.onIntent(NetworkMapIntent.Load) }
    // Frame the place whenever its boxes change.
    LaunchedEffect(state.map?.placeId, mapLoaded) {
        val pins = state.map?.pins.orEmpty()
        if (!mapLoaded || pins.isEmpty()) return@LaunchedEffect
        val bounds = LatLngBounds.builder().apply { pins.forEach { include(LatLng(it.latitude, it.longitude)) } }.build()
        runCatching { camera.move(CameraUpdateFactory.newLatLngBounds(bounds, BOUNDS_PADDING_PX)) }
    }
    NetworkMapContent(
        state = state,
        onIntent = viewModel::onIntent,
        onOpenBox = onOpenBox,
        onDirections = { detail -> context.directionsTo(detail) },
        map = {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = camera,
                uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false),
                onMapLoaded = { mapLoaded = true },
                onMapClick = { viewModel.onIntent(NetworkMapIntent.CardClosed) },
            ) {
                val networkMap = state.map ?: return@GoogleMap
                if (MapLayer.NAPS in state.layers) {
                    networkMap.pins.forEach { pin ->
                        val selected = pin.id == state.selectedId
                        MarkerComposable(
                            pin.id, pin.free, selected,
                            state = rememberUpdatedMarkerState(LatLng(pin.latitude, pin.longitude)),
                            title = pin.code,
                            onClick = { viewModel.onIntent(NetworkMapIntent.PinSelected(pin.id)); true },
                        ) {
                            Box(
                                Modifier.size(if (selected) 36.dp else 30.dp).background(pin.level.color(), CircleShape)
                                    .border(if (selected) 3.dp else 2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(if (pin.level == NapLevel.FULL) "!" else "${pin.free}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (MapLayer.MUFAS in state.layers) {
                    networkMap.mufas.forEach { mufa ->
                        Marker(
                            state = rememberUpdatedMarkerState(LatLng(mufa.latitude, mufa.longitude)),
                            title = "Mufa ${mufa.label}",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET),
                        )
                    }
                }
            }
        },
    )
}

/** «Caja NAP» screen. */
@Composable
fun NapDetailRoute(napId: Int, onOpenSubscription: (Int) -> Unit) {
    val viewModel: NapDetailViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(napId) { viewModel.load(napId) }
    NapDetailContent(
        state = state,
        onOpenSubscription = onOpenSubscription,
        onDirections = { context.directionsTo(it) },
        onOpenMap = { context.showOnMap(it) },
        onRetry = { viewModel.load(napId) },
    )
}

/** Turn-by-turn in Google Maps when installed, any map app otherwise. */
private fun Context.directionsTo(detail: NapDetail) {
    val lat = detail.latitude ?: return
    val lng = detail.longitude ?: return
    val navigation = Intent(Intent.ACTION_VIEW, Uri.parse(String.format(Locale.US, "google.navigation:q=%f,%f", lat, lng)))
        .setPackage("com.google.android.apps.maps").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(navigation)
    } catch (_: ActivityNotFoundException) {
        showOnMap(detail)
    }
}

private fun Context.showOnMap(detail: NapDetail) {
    val lat = detail.latitude ?: return
    val lng = detail.longitude ?: return
    val label = Uri.encode(detail.code)
    val geo = Intent(Intent.ACTION_VIEW, Uri.parse(String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)", lat, lng, lat, lng, label)))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(geo) }
}
