package com.dscorp.ispadmin.presentation.ui.features.locationMapView

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.BuildConfig
import com.dscorp.ispadmin.domain.model.GeoLocation
import com.dscorp.ispadmin.presentation.ui.features.dialog.MyCustomDialog
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

const val MAP_SELECTION_REQUEST_KEY = "map_selection_request"
const val MAP_SELECTION_RESULT_KEY = "map_selection_result"

@Composable
fun LocationSelectorComposeDialog(
    initialLocation: GeoLocation?,
    onLocationSelected: (LatLng) -> Unit,
    onDismiss: () -> Unit,
    enableMyLocation: Boolean = true,
    mapsConfigured: Boolean = BuildConfig.HAS_MAPS_API_KEY,
) {
    val cameraLatLng = remember(initialLocation) { initialMapCameraLatLng(initialLocation) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(cameraLatLng, DEFAULT_MANUAL_MAP_CAMERA_ZOOM)
    }
    var coordinateQuery by remember { mutableStateOf("") }
    var coordinateError by remember { mutableStateOf<String?>(null) }
    var selectedLatLng by remember(initialLocation) {
        mutableStateOf(initialLocation?.let { LatLng(it.latitude, it.longitude) })
    }

    val uiSettings = remember(enableMyLocation) {
        MapUiSettings(
            zoomControlsEnabled = true,
            myLocationButtonEnabled = enableMyLocation
        )
    }
    val properties = remember(enableMyLocation) {
        MapProperties(
            mapType = MapType.SATELLITE,
            isMyLocationEnabled = enableMyLocation
        )
    }

    MyCustomDialog(
        usePlatformDefaultWidth = false,
        paddingValues = PaddingValues(0.dp),
        content = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text(
                            text = "Seleccionar ubicación",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        OutlinedTextField(
                            value = coordinateQuery,
                            onValueChange = {
                                coordinateQuery = it
                                coordinateError = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("map_coordinate_search"),
                            label = { Text("Latitud, longitud") },
                            placeholder = { Text("-11.23416, -77.37872") },
                            isError = coordinateError != null,
                            supportingText = coordinateError?.let { { Text(it) } },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val parsed = parseLatLngQuery(coordinateQuery)
                                if (parsed == null) {
                                    coordinateError = "Pega una coordenada válida: latitud, longitud"
                                } else {
                                    coordinateError = null
                                    val target = LatLng(parsed.latitude, parsed.longitude)
                                    selectedLatLng = target
                                    if (mapsConfigured) {
                                        cameraPositionState.move(
                                            CameraUpdateFactory.newLatLngZoom(
                                                target,
                                                DEFAULT_MANUAL_MAP_CAMERA_ZOOM
                                            )
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.testTag("map_coordinate_search_button")
                        ) {
                            Text("Buscar")
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = true)
                    ) {
                        if (mapsConfigured) {
                            GoogleMap(
                                modifier = Modifier.fillMaxSize(),
                                cameraPositionState = cameraPositionState,
                                uiSettings = uiSettings,
                                properties = properties
                            )
                            Image(
                                painter = painterResource(id = android.R.drawable.ic_menu_mylocation),
                                contentDescription = "Pin de ubicación",
                                modifier = Modifier
                                    .size(48.dp)
                                    .align(Alignment.Center)
                            )
                        } else {
                            Text(
                                text = "Google Maps no puede cargar: falta MAPS_API_KEY en local.properties. " +
                                    "Pega latitud, longitud y pulsa Seleccionar ubicación, o reinstala tras configurar la clave.",
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp)
                                    .testTag("map_missing_api_key"),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Mueve el mapa y confirma la ubicación central",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 16.dp)
                        )
                        Button(
                            onClick = {
                                val center = resolveManualMapSelectionTarget(
                                    mapsConfigured = mapsConfigured,
                                    searchedOrPinned = selectedLatLng,
                                    cameraTarget = cameraPositionState.position.target,
                                ) ?: return@Button
                                onLocationSelected(center)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .testTag("map_select_location_button")
                        ) {
                            Text("Seleccionar ubicación")
                        }
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancelar")
                        }
                    }
                }
            }
        }
    )
}
