package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import android.content.Context
import android.net.Uri
import android.content.res.Configuration
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.components.rememberPhotoTaker
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionIntent
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionUiEvent
import java.io.File

@Composable
fun RegisterSubscriptionFormScreen(
    modifier: Modifier = Modifier,
    viewModel: RegisterSubscriptionComposeViewModel,
    context: Context = LocalContext.current,
    onSubscriptionRegisterSuccess: () -> Unit = {},
    installationOrderId: Int?,
) {
    val locationSetup = rememberLocationSetupState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var dialogError by remember { mutableStateOf<String?>(null) }
    var successSubscription by remember { mutableStateOf<Subscription?>(null) }
    var showFacadePhotoOptionsDialog by remember { mutableStateOf(false) }
    var locationFetched by remember { mutableStateOf(false) }

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

    val (takeFacadePhoto, _) = rememberPhotoTaker(
        context = context,
        onPhotoTaken = { uri ->
            viewModel.onFacadePhotoSelected(uri)
        }
    )

    val facadePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onFacadePhotoSelected(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.loadScreenData(installationOrderId)
    }

    LaunchedEffect(locationSetup.isReady) {
        if (locationSetup.isReady && !locationFetched) {
            locationFetched = true
            locationSetup.fetchCurrentLocation { latitude, longitude ->
                viewModel.processCurrentLocation(latitude, longitude)
            }
        }
        if (!locationSetup.isReady) {
            locationFetched = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (locationSetup.isReady) {
            RegisterSubscriptionForm(
                formState = uiState,
                onIntent = { intent ->
                    if (intent is RegisterSubscriptionIntent.RegisterClick) {
                        val facadePhotoFile =
                            uiState.registerSubscriptionForm.facadePhotoUri?.let { uri ->
                                uriToFile(context = context, uri = uri)
                            }
                        viewModel.onIntent(
                            RegisterSubscriptionIntent.RegisterClick(
                                facadePhotoFile = facadePhotoFile
                            )
                        )
                    } else {
                        viewModel.onIntent(intent)
                    }
                },
                onFacadePhotoClick = { showFacadePhotoOptionsDialog = true },
            )
        } else {
            LocationSetupGate(
                status = locationSetup.status,
                onContinue = locationSetup.onContinue,
                onOpenAppSettings = locationSetup.openAppSettings,
                onOpenLocationSettings = locationSetup.openLocationSettings,
                modifier = Modifier.fillMaxSize(),
            )
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

        if (showFacadePhotoOptionsDialog) {
            AlertDialog(
                onDismissRequest = { showFacadePhotoOptionsDialog = false },
                title = { Text("Foto de fachada") },
                text = { Text("Elige como quieres adjuntar la foto de la fachada.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showFacadePhotoOptionsDialog = false
                            takeFacadePhoto()
                        }
                    ) {
                        Text("Tomar foto")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showFacadePhotoOptionsDialog = false
                            facadePhotoPickerLauncher.launch("image/*")
                        }
                    ) {
                        Text("Galería")
                    }
                }
            )
        }
    }
}

private fun uriToFile(context: Context, uri: Uri): File {
    val file = File.createTempFile(
        "facade_photo_",
        ".jpg",
        context.cacheDir
    )

    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        file.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    } ?: throw IllegalArgumentException("No se pudo leer la foto de fachada")

    return file
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

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ErrorDialogPreview() {
    MyTheme {
        ErrorDialog(error = "No se pudo completar la operación", onDismiss = {})
    }
}
