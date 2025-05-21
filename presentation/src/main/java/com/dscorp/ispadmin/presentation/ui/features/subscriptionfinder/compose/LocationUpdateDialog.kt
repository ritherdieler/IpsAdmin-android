package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MiniMap
import com.dscorp.ispadmin.presentation.ui.features.dialog.MyCustomDialog
import com.dscorp.ispadmin.domain.model.SubscriptionResume
import com.google.android.gms.maps.model.LatLng

/**
 * Dialog to update subscription geographic location with Material 3 design
 */
@Composable
fun LocationUpdateDialog(
    selectedSubscription: SubscriptionResume?,
    saveState: SaveSubscriptionState,
    latitude: String,
    longitude: String,
    onShowMap: () -> Unit,
    onGetCurrentLocationClick: () -> Unit,
    isFetchingCurrentLocation: Boolean,
    onUpdateLocation: () -> Unit,
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
        paddingValues = PaddingValues(0.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Header con botón de volver
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = "Actualizar ubicación",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(16.dp)
                        .weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Content area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (saveState) {
                SaveSubscriptionState.Loading -> LoadingContent()
                SaveSubscriptionState.Success -> LocationContent(
                    currentLatLng = currentLatLng,
                    newLatLng = newLatLng,
                    isLocationChanged = isLocationChanged,
                    isFetchingCurrentLocation = isFetchingCurrentLocation,
                    onShowMap = onShowMap,
                    onGetCurrentLocationClick = onGetCurrentLocationClick,
                    onUpdateLocation = onUpdateLocation,
                    onDismiss = onDismiss
                )
                SaveSubscriptionState.Error -> ErrorContent(onDismiss)
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "Actualizando ubicación...",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun LocationContent(
    currentLatLng: LatLng?,
    newLatLng: LatLng?,
    isLocationChanged: Boolean,
    isFetchingCurrentLocation: Boolean,
    onShowMap: () -> Unit,
    onGetCurrentLocationClick: () -> Unit,
    onUpdateLocation: () -> Unit,
    onDismiss: () -> Unit
) {
    // Current location section
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Ubicación Actual",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (currentLatLng != null) {
                MiniMap(location = currentLatLng, title = "Actual")
            } else {
                Text(
                    text = "No hay ubicación registrada",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    
    Divider(modifier = Modifier.padding(vertical = 16.dp))
    
    // New location section
    if (isLocationChanged) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Nueva Ubicación Seleccionada",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    newLatLng?.let {
                        MiniMap(location = it, title = "Nueva")
                    }
                }
            }
        }
    } else {
        Text(
            text = "Seleccione una nueva ubicación usando las opciones de abajo",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Location selection actions
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onShowMap,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Map,
                contentDescription = stringResource(R.string.select_from_map),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.select_from_map),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }

        OutlinedButton(
            onClick = onGetCurrentLocationClick,
            enabled = !isFetchingCurrentLocation,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isFetchingCurrentLocation) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.MyLocation,
                    contentDescription = stringResource(R.string.current_location),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.current_location),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    
    // Action buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Cancelar",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }

        Button(
            onClick = onUpdateLocation,
            modifier = Modifier.weight(1f),
            enabled = isLocationChanged,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = "Actualizar",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ErrorContent(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )

        Text(
            text = "Error al actualizar ubicación",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(0.7f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(
                text = "Aceptar",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
} 