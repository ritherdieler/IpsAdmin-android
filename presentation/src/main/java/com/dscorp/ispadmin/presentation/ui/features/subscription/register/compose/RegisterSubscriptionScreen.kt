package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RegisterSubscriptionFormScreen(
    viewModel: RegisterSubscriptionComposeViewModel,
    context: Context = LocalContext.current,
    onSubscriptionRegisterSuccess: () -> Unit = {},
    installationOrderId: Int?,
) {
    val locationPermissionState =
        rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val isEnabled = isGpsEnabled(context)
        viewModel.onGpsStateChanged(isEnabled)
    }

    LaunchedEffect(Unit) {
        viewModel.loadInitialFormData()
        installationOrderId?.let {
            viewModel.loadInstallationOrderData(it)
        }
        
        val isGpsCurrentlyEnabled = isGpsEnabled(context)
        viewModel.onGpsStateChanged(isGpsCurrentlyEnabled)
        viewModel.onLocationPermissionChanged(locationPermissionState.status.isGranted)
    }
    
    LaunchedEffect(locationPermissionState.status.isGranted) {
        viewModel.onLocationPermissionChanged(locationPermissionState.status.isGranted)
    }
    
    LaunchedEffect(uiState.isGpsEnabled, uiState.hasLocationPermission) {
        if (uiState.isGpsEnabled && uiState.hasLocationPermission) {
            try {
                val currentLocationRequest = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .build()

                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedLocationClient.getCurrentLocation(currentLocationRequest, null)
                        .addOnSuccessListener { location ->
                            location?.let {
                                viewModel.processCurrentLocation(it.latitude, it.longitude)
                            }
                        }
                }
            } catch (e: Exception) {
            }
        }
    }

    uiState.registeredSubscription?.let { subscription ->
        SuccessDialog(
            subscription = subscription,
            onDismiss = { viewModel.clearRegisteredSubscription() },
            onContinue = onSubscriptionRegisterSuccess
        )
    }

    uiState.error?.let { error ->
        ErrorDialog(
            error = error,
            onDismiss = { viewModel.clearError() }
        )
    }

    if (uiState.shouldShowGpsDialog) {
        GpsDialog(
            onActivateGps = {
                viewModel.dismissGpsDialog()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                locationSettingsLauncher.launch(intent)
            },
            onDismiss = { viewModel.dismissGpsDialog() }
        )
    }

    when {
        uiState.isGpsEnabled && uiState.hasLocationPermission -> {
            RegisterSubscriptionForm(
                formState = uiState,
                onFirstNameChanged = { viewModel.onFirstNameChanged(it) },
                onLastNameChanged = { viewModel.onLastNameChanged(it) },
                onDniChanged = { viewModel.onDniChanged(it) },
                onAddressChanged = { viewModel.onAddressChanged(it) },
                onPhoneChanged = { viewModel.onPhoneChanged(it) },
                onPlanSelected = { viewModel.onPlanSelected(it) },
                onOnuSelected = { viewModel.onOnuSelected(it) },
                onPlaceSelected = {
                    viewModel.onPlaceSelected(it)
                },
                onNapBoxSelected = { viewModel.onNapBoxSelected(it) },
                onPLaceSelectionCleared = { viewModel.onPlaceSelectionCleared() },
                onNapBoxSelectionCleared = { viewModel.onNapBoxSelectionCleared() },
                onInstallationTypeSelected = {
                    viewModel.onInstallationTypeSelected(it)
                },
                onRefreshOnuList = { viewModel.refreshOnuList() },
                onRegisterClick = {
                    viewModel.saveSubscription()
                },
                onNoteChanged = { viewModel.onNoteChanged(it) },
                onEquipmentConditionChanged = { viewModel.onEquipmentConditionChanged(it) },
            )
        }

        !uiState.isGpsEnabled -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Es necesario activar el GPS para continuar")
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        locationSettingsLauncher.launch(intent)
                    }
                ) {
                    Text("Activar GPS")
                }
            }
        }

        else -> {
            if (locationPermissionState.status.shouldShowRationale) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Se necesita permiso de ubicación para obtener su ubicación actual")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                        Text("Solicitar permiso")
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Por favor conceda el permiso o vaya a la configuración y habilite manualmente el permiso de ubicación para usar esta función.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val intent =
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            context.startActivity(intent)
                        }
                    ) {
                        Text("Ir a Configuración")
                    }
                }
            }
        }
    }
}

private fun isGpsEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SuccessDialog(
    subscription: com.dscorp.ispadmin.domain.model.Subscription,
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Éxito",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "¡Registro Exitoso!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    "La suscripción se ha registrado correctamente",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "${subscription.firstName} ${subscription.lastName}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoRow("DNI", subscription.dni ?: "")
                        InfoRow("Teléfono", subscription.phone ?: "")
                        InfoRow("Dirección", subscription.address ?: "")
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Detalles Técnicos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoRow("IP", subscription.ip ?: "No asignada")
                        
                        if (subscription.installationType == com.dscorp.ispadmin.domain.model.InstallationType.FIBER || 
                            subscription.installationType == com.dscorp.ispadmin.domain.model.InstallationType.ONLY_TV_FIBER) {
                            InfoRow("Borne", subscription.borneNumber ?: "No asignado")
                        }
                        
                        InfoRow("Tipo", subscription.installationType?.toString() ?: "No especificado")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onDismiss()
                    onContinue()
                }
            ) {
                Text("Continuar")
            }
        }
    )
}

@Composable
private fun ErrorDialog(
    error: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error") },
        text = { Text(error) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Aceptar")
            }
        }
    )
}

@Composable
private fun GpsDialog(
    onActivateGps: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GPS desactivado") },
        text = { 
            Text("Para utilizar esta funcionalidad, es necesario activar el GPS de su dispositivo.") 
        },
        confirmButton = {
            Button(onClick = onActivateGps) {
                Text("Activar GPS")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}


