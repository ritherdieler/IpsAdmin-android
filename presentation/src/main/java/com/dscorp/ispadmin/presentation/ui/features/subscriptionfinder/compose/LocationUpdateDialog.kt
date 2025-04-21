package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MiniMap
import com.dscorp.ispadmin.presentation.ui.features.dialog.MyCustomDialog
import com.dscorp.ispadmin.domain.model.SubscriptionResume
import com.google.android.gms.maps.model.LatLng

/**
 * Dialog to update subscription geographic location (Map Selection Only)
 */
@Composable
fun LocationUpdateDialog(
    viewModel: SubscriptionFinderViewModel,
    selectedSubscription: SubscriptionResume?,
    saveState: SaveSubscriptionState,
    latitude: String,
    longitude: String,
    onShowMap: () -> Unit,
    onGetCurrentLocationClick: () -> Unit,
    isFetchingCurrentLocation: Boolean,
    onDismiss: () -> Unit
) {
    if (selectedSubscription == null) {
        LaunchedEffect(Unit) {
            onDismiss()
        }
        return
    }

    val currentLatLng by remember(selectedSubscription.location) {
        derivedStateOf {
            selectedSubscription.location.latitude?.let { lat ->
                selectedSubscription.location.longitude?.let { lon ->
                    LatLng(lat, lon)
                }
            }
        }
    }
    val newLatLng by remember(latitude, longitude) {
        derivedStateOf {
            latitude.toDoubleOrNull()?.let { lat ->
                longitude.toDoubleOrNull()?.let { lon ->
                    LatLng(lat, lon)
                }
            }
        }
    }
    
    val isLocationChanged by remember(currentLatLng, newLatLng) {
        derivedStateOf {
            newLatLng != null && newLatLng != currentLatLng
        }
    }

    MyCustomDialog(
        onDismissRequest = onDismiss,
        usePlatformDefaultWidth = false,
        paddingValues =  PaddingValues(0.dp),
        modifier = Modifier.fillMaxSize()
    ) {


        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Actualizar ubicación",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            when (saveState) {
                SaveSubscriptionState.Loading -> {
                    CircularProgressIndicator()
                    Text(text = "Actualizando ubicación...")
                }

                SaveSubscriptionState.Success -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Ubicación Actual",
                            style = MaterialTheme.typography.titleMedium
                        )
                        MiniMap(location = currentLatLng, title = "Actual")
                        
                        if (isLocationChanged) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nueva Ubicación Seleccionada",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            MiniMap(location = newLatLng, title = "Nueva")
                        } else {
                           Text(
                                text = "Seleccione una nueva ubicación en el mapa",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                           )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onShowMap,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Map,
                                    contentDescription = stringResource(R.string.select_from_map),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.select_from_map))
                            }

                            OutlinedButton(
                                onClick = onGetCurrentLocationClick,
                                enabled = !isFetchingCurrentLocation,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isFetchingCurrentLocation) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.MyLocation,
                                        contentDescription = stringResource(R.string.current_location),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.current_location))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancelar")
                            }

                            Button(
                                onClick = { viewModel.updateSubscriptionLocation() },
                                modifier = Modifier.weight(1f),
                                enabled = isLocationChanged
                            ) {
                                Text("Actualizar")
                            }
                        }
                    }
                }

                SaveSubscriptionState.Error -> {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )

                    Text(
                        text = "Error al actualizar ubicación",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Aceptar")
                    }
                }
            }
        }
    }
} 