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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.google.android.gms.maps.model.LatLng

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

    // Estado para controlar el diálogo de GPS
    var showGpsDialog by remember { mutableStateOf(false) }

    // Estado para verificar si el GPS está habilitado
    var isGpsEnabled by remember { mutableStateOf(isGpsEnabled(context)) }

    // Launcher para abrir la configuración de ubicación
    val locationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Verificar nuevamente si el GPS está habilitado después de regresar de la configuración
        isGpsEnabled = isGpsEnabled(context)
    }

    LaunchedEffect(Unit) {
        viewModel.loadInitialFormData()
        installationOrderId?.let {
            viewModel.loadInstallationOrderData(it)
        }
    }


    when {
        uiState.registeredSubscription != null -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearRegisteredSubscription() },
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
                                    "${uiState.registeredSubscription?.firstName} ${uiState.registeredSubscription?.lastName}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                InfoRow("DNI", uiState.registeredSubscription?.dni ?: "")
                                InfoRow("Teléfono", uiState.registeredSubscription?.phone ?: "")
                                InfoRow("Dirección", uiState.registeredSubscription?.address ?: "")
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
                                InfoRow("IP", uiState.registeredSubscription?.ip ?: "No asignada")
                                
                                // Solo mostrar Borne si el tipo es Fibra o TV Cable
                                val installationType = uiState.registeredSubscription?.installationType
                                if (installationType == com.dscorp.ispadmin.domain.model.InstallationType.FIBER || 
                                    installationType == com.dscorp.ispadmin.domain.model.InstallationType.ONLY_TV_FIBER) {
                                    InfoRow("Borne", uiState.registeredSubscription?.borneNumber ?: "No asignado")
                                }
                                
                                InfoRow("Tipo", uiState.registeredSubscription?.installationType?.toString() ?: "No especificado")
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.clearRegisteredSubscription()
                            onSubscriptionRegisterSuccess()
                        }
                    ) {
                        Text("Continuar")
                    }
                }
            )
        }

        uiState.error != null -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("Error") },
                text = {
                    Text(
                        uiState.error ?: "Ha ocurrido un error al registrar la suscripción"
                    )
                },
                confirmButton = {
                    Button(onClick = { viewModel.clearError() }) {
                        Text("Aceptar")
                    }
                }
            )
        }
    }

    // Verificar GPS al inicio
    LaunchedEffect(Unit) {
        isGpsEnabled = isGpsEnabled(context)
        if (!isGpsEnabled) {
            showGpsDialog = true
        }
    }

    // Diálogo para solicitar activar el GPS
    if (showGpsDialog) {
        AlertDialog(
            onDismissRequest = { showGpsDialog = false },
            title = { Text("GPS desactivado") },
            text = { Text("Para utilizar esta funcionalidad, es necesario activar el GPS de su dispositivo.") },
            confirmButton = {
                Button(
                    onClick = {
                        showGpsDialog = false
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        locationSettingsLauncher.launch(intent)
                    }
                ) {
                    Text("Activar GPS")
                }
            },
            dismissButton = {
                Button(onClick = { showGpsDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LaunchedEffect(isGpsEnabled, locationPermissionState.status.isGranted) {
        if (isGpsEnabled && locationPermissionState.status.isGranted) {
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
                                viewModel.onLocationChanged(
                                    LatLng(
                                        it.latitude,
                                        it.longitude
                                    )
                                )
                                viewModel.getPlaceFromCurrentLocation(
                                    it.latitude,
                                    it.longitude
                                )
                                
                                // Obtener cajas Nap cercanas a la ubicación actual
                                viewModel.getNearbyNapBoxes(
                                    it.latitude,
                                    it.longitude
                                )
                            }
                        }
                }
            } catch (e: Exception) {
            }
        }
    }

    LaunchedEffect(uiState.registerSubscriptionForm.selectedPlace) {
        println(uiState.registerSubscriptionForm.selectedPlace)
    }

    when {
        // Solo mostrar el formulario si GPS está habilitado y los permisos concedidos
        isGpsEnabled && locationPermissionState.status.isGranted -> {
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

        // Si el GPS está deshabilitado, mostrar mensaje para habilitarlo
        !isGpsEnabled -> {
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

        // Si el GPS está habilitado pero falta el permiso
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
                            // Abrir la configuración de la aplicación para que el usuario active manualmente el permiso
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

// Función para verificar si el GPS está habilitado
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

