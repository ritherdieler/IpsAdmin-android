package com.dscorp.ispadmin.presentation.ui.features.locationMapView

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.domain.model.GeoLocation
import com.dscorp.ispadmin.presentation.ui.features.dialog.MyCustomDialog
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

// Constantes para el resultado del diálogo
const val MAP_SELECTION_REQUEST_KEY = "map_selection_request"
const val MAP_SELECTION_RESULT_KEY = "map_selection_result"

@Composable
fun LocationSelectorComposeDialog(
    initialLocation: GeoLocation?,
    onLocationSelected: (LatLng) -> Unit,
    onDismiss: () -> Unit
) {
    val initialLatLng = initialLocation?.let { LatLng(it.latitude, it.longitude) }
        ?: LatLng(-12.046374, -77.042793) // Lima, Perú como ubicación por defecto
    
    // Estado de la cámara del mapa
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLatLng, 15f)
    }
    
    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = true,
            myLocationButtonEnabled = true
        )
    }
    
    val properties = remember {
        MapProperties(
            mapType = MapType.SATELLITE,
            isMyLocationEnabled = true
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
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Barra superior con botón de volver
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
                    
                    // Contenedor principal que ocupa todo el espacio disponible
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = true)
                    ) {
                        // Mapa que ocupa todo el contenedor
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            uiSettings = uiSettings,
                            properties = properties
                        )
                        
                        // Pin fijo en el centro
                        Image(
                            painter = painterResource(id = android.R.drawable.ic_menu_mylocation),
                            contentDescription = "Pin de ubicación",
                            modifier = Modifier
                                .size(48.dp)
                                .align(Alignment.Center)
                        )
                    }
                    
                    // Botones de acción en la parte inferior
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
                                val center = cameraPositionState.position.target
                                onLocationSelected(center)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Text("Confirmar ubicación")
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