package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import android.content.res.Configuration
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionUiEvent
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

private const val LOCATION_LOG_TAG = "RegisterSubscription"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RegisterSubscriptionFormScreen(
    modifier: Modifier = Modifier,
    viewModel: RegisterSubscriptionComposeViewModel,
    context: Context = LocalContext.current,
    onSubscriptionRegisterSuccess: () -> Unit = {},
    installationOrderId: Int?,
) {
    val locationPermissionState =
        rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var dialogError by remember { mutableStateOf<String?>(null) }
    var successSubscription by remember { mutableStateOf<Subscription?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is RegisterSubscriptionUiEvent.Error -> dialogError = event.message
                    is RegisterSubscriptionUiEvent.Success -> successSubscription = event.subscription
                }
            }
        }
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val isEnabled = isGpsEnabled(context)
        viewModel.onGpsStateChanged(isEnabled)
    }

    LaunchedEffect(Unit) {
        viewModel.loadScreenData(installationOrderId)

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
                Log.w(LOCATION_LOG_TAG, "getCurrentLocation failed", e)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isGpsEnabled && uiState.hasLocationPermission -> {
                RegisterSubscriptionForm(
                    formState = uiState,
                    onIntent = viewModel::onIntent,
                )
            }

            !uiState.isGpsEnabled -> {
                GpsDisabledContent(
                    modifier = Modifier.fillMaxSize(),
                    onOpenLocationSettings = {
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        locationSettingsLauncher.launch(intent)
                    }
                )
            }

            locationPermissionState.status.shouldShowRationale -> {
                LocationPermissionRationaleContent(
                    modifier = Modifier.fillMaxSize(),
                    onRequestPermission = { locationPermissionState.launchPermissionRequest() }
                )
            }

            else -> {
                LocationPermissionSettingsContent(
                    modifier = Modifier.fillMaxSize(),
                    packageName = context.packageName,
                    onOpenAppSettings = { pkg ->
                        val intent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", pkg, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        context.startActivity(intent)
                    }
                )
            }
        }

        successSubscription?.let { subscription ->
            SuccessDialog(
                subscription = subscription,
                onDismiss = { successSubscription = null },
                onContinue = onSubscriptionRegisterSuccess
            )
        }

        dialogError?.let { error ->
            ErrorDialog(
                error = error,
                onDismiss = { dialogError = null }
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
    }
}

@Composable
private fun GpsDisabledContent(
    modifier: Modifier = Modifier,
    onOpenLocationSettings: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Es necesario activar el GPS para continuar",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onOpenLocationSettings) {
            Text("Activar GPS")
        }
    }
}

@Composable
private fun LocationPermissionRationaleContent(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Se necesita permiso de ubicación para obtener su ubicación actual",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRequestPermission) {
            Text("Solicitar permiso")
        }
    }
}

@Composable
private fun LocationPermissionSettingsContent(
    modifier: Modifier = Modifier,
    packageName: String,
    onOpenAppSettings: (String) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Por favor conceda el permiso o vaya a la configuración y habilite manualmente el permiso de ubicación para usar esta función.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onOpenAppSettings(packageName) }) {
            Text("Ir a Configuración")
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
    subscription: Subscription,
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "¡Registro Exitoso!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "La suscripción se ha registrado correctamente",
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
                            text = "${subscription.firstName} ${subscription.lastName}",
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
                            text = "Detalles Técnicos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoRow("IP", subscription.ip ?: "No asignada")

                        if (subscription.installationType == InstallationType.FIBER ||
                            subscription.installationType == InstallationType.ONLY_TV_FIBER
                        ) {
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
        title = {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium
            )
        },
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
        title = {
            Text(
                text = "GPS desactivado",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "Para utilizar esta funcionalidad, es necesario activar el GPS de su dispositivo.",
                style = MaterialTheme.typography.bodyMedium
            )
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

@Preview(showBackground = true)
@Composable
private fun GpsDisabledContentPreview() {
    MyTheme {
        GpsDisabledContent(onOpenLocationSettings = {})
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LocationPermissionRationaleContentPreview() {
    MyTheme {
        LocationPermissionRationaleContent(onRequestPermission = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationPermissionSettingsContentPreview() {
    MyTheme {
        LocationPermissionSettingsContent(packageName = "com.example", onOpenAppSettings = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun SuccessDialogPreview() {
    MyTheme {
        SuccessDialog(
            subscription = Subscription(
                firstName = "Ana",
                lastName = "García",
                dni = "12345678",
                phone = "987654321",
                address = "Av. Principal 100",
                ip = "10.0.0.1",
                installationType = InstallationType.FIBER,
                borneNumber = "B12"
            ),
            onDismiss = {},
            onContinue = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorDialogPreview() {
    MyTheme {
        ErrorDialog(error = "No se pudo completar la operación", onDismiss = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun GpsDialogPreview() {
    MyTheme {
        GpsDialog(onActivateGps = {}, onDismiss = {})
    }
}
