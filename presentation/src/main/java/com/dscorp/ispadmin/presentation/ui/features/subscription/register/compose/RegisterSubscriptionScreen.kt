package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.dscorp.ispadmin.BuildConfig
import com.dscorp.ispadmin.data.media.prepareFacadePhotoFile
import com.dscorp.ispadmin.domain.model.GeoLocation
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.components.rememberPhotoTaker
import com.dscorp.ispadmin.presentation.ui.features.locationMapView.LocationSelectorComposeDialog
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionDebugActions
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionIntent
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionUiEvent
import java.io.File

@Composable
fun RegisterSubscriptionFormScreen(
    modifier: Modifier = Modifier,
    viewModel: RegisterSubscriptionComposeViewModel,
    context: Context = LocalContext.current,
    onSubscriptionRegisterSuccess: () -> Unit = {},
    onNavigateToPendingSubscriptions: () -> Unit = {},
    installationOrderId: Int?,
) {
    val locationSetup = rememberLocationSetupState()
    val locationSetupLatest = rememberUpdatedState(locationSetup)
    val viewModelLatest = rememberUpdatedState(viewModel)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var dialogError by remember { mutableStateOf<String?>(null) }
    var successSubscription by remember { mutableStateOf<Subscription?>(null) }
    var showQueuedOfflineDialog by remember { mutableStateOf(false) }
    var showFacadePhotoOptionsDialog by remember { mutableStateOf(false) }
    var showCurrentLocationGate by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is RegisterSubscriptionUiEvent.Error -> dialogError = event.message
                    is RegisterSubscriptionUiEvent.Success -> successSubscription = event.subscription
                    RegisterSubscriptionUiEvent.QueuedOffline -> showQueuedOfflineDialog = true
                    RegisterSubscriptionUiEvent.RequestCurrentLocation -> {
                        val setup = locationSetupLatest.value
                        showCurrentLocationGate = !setup.hasPermission || !setup.isReady
                        setup.requestCurrentLocation { latitude, longitude ->
                            showCurrentLocationGate = false
                            viewModelLatest.value.onIntent(
                                RegisterSubscriptionIntent.LocationCoordinatesSelected(
                                    latitude = latitude,
                                    longitude = longitude
                                )
                            )
                        }
                    }
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

    if (BuildConfig.DEBUG) {
        DisposableEffect(viewModel) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    when (intent?.action) {
                        RegisterSubscriptionDebugActions.SET_FACADE_PHOTO -> {
                            val path = intent.getStringExtra(RegisterSubscriptionDebugActions.EXTRA_PATH)
                                ?: return
                            val file = File(path)
                            if (!file.exists()) return
                            viewModel.onFacadePhotoSelected(Uri.fromFile(file))
                        }
                    }
                }
            }
            val filter = IntentFilter().apply {
                addAction(RegisterSubscriptionDebugActions.SET_FACADE_PHOTO)
            }
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            onDispose {
                runCatching { context.unregisterReceiver(receiver) }
            }
        }
    }

    LaunchedEffect(locationSetup.isReady) {
        if (locationSetup.isReady) {
            showCurrentLocationGate = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        RegisterSubscriptionForm(
            formState = uiState,
            onIntent = { intent ->
                if (intent is RegisterSubscriptionIntent.RegisterClick) {
                    val facadePhotoFile =
                        uiState.registerSubscriptionForm.facadePhotoUri?.let { uri ->
                            prepareFacadePhotoFile(context = context, uri = uri)
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

        if (showCurrentLocationGate && !locationSetup.isReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                LocationSetupGate(
                    status = locationSetup.status,
                    onContinue = locationSetup.onContinue,
                    onOpenAppSettings = locationSetup.openAppSettings,
                    onOpenLocationSettings = locationSetup.openLocationSettings,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (uiState.showManualLocationMap) {
            LocationSelectorComposeDialog(
                initialLocation = uiState.registerSubscriptionForm.location?.let {
                    GeoLocation(it.latitude, it.longitude)
                },
                enableMyLocation = locationSetup.hasPermission,
                onLocationSelected = { latLng ->
                    viewModel.onIntent(
                        RegisterSubscriptionIntent.LocationCoordinatesSelected(
                            latitude = latLng.latitude,
                            longitude = latLng.longitude
                        )
                    )
                },
                onDismiss = {
                    viewModel.onIntent(RegisterSubscriptionIntent.DismissManualLocationMap)
                }
            )
        }

        successSubscription?.let { subscription ->
            RegisterSuccessFullScreen(
                subscription = subscription,
                tr069RetryLoading = uiState.tr069RetryLoading,
                onRetryTr069 = subscription.resolvedSubscriptionId()?.let { subscriptionId ->
                    { viewModel.onIntent(RegisterSubscriptionIntent.RetryTr069(subscriptionId)) }
                },
                onDismiss = { successSubscription = null },
                onContinue = onSubscriptionRegisterSuccess
            )
        }

        if (showQueuedOfflineDialog) {
            QueuedOfflineDialog(
                onDismiss = {
                    showQueuedOfflineDialog = false
                    onSubscriptionRegisterSuccess()
                },
                onViewPending = {
                    showQueuedOfflineDialog = false
                    onNavigateToPendingSubscriptions()
                }
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

        if (uiState.isLoading) {
            RegistrationProgressOverlay(
                progressMessage = uiState.registrationProgressMessage
            )
        }
    }
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
internal fun RegistrationProgressOverlay(
    progressMessage: String? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
            .testTag("registration_progress_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = registrationProgressStepMessage(progressMessage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("registration_progress_step")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Esto puede tomar hasta 2 minutos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("registration_progress_hint")
                )
            }
        }
    }
}

@Composable
internal fun RegisterSuccessFullScreen(
    subscription: Subscription,
    tr069RetryLoading: Boolean = false,
    onRetryTr069: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    val tr069Status = subscription.tr069ProvisionStatus
    val requiresManualTr069 = tr069Status == "MANUAL_REQUIRED" || subscription.tr069RequiresManualConfig
    val isPendingTr069 = tr069Status == "PENDING"
    val showTr069Section = tr069Status == "COMPLETE" || requiresManualTr069 || isPendingTr069
    val showRetryTr069 = (requiresManualTr069 || isPendingTr069) && onRetryTr069 != null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("register_success_fullscreen"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (subscription.provisioningPending) {
                            "Registrado; provisión de red pendiente de reconciliar"
                        } else {
                            "La suscripción se ha registrado correctamente"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag("register_success_message")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    RegisterSuccessSectionCard(
                        title = "Suscriptor",
                        testTag = "register_success_section_subscriber"
                    ) {
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

                    Spacer(modifier = Modifier.height(12.dp))

                    RegisterSuccessSectionCard(
                        title = "Red",
                        testTag = "register_success_section_network"
                    ) {
                        InfoRow("IP", subscription.ip ?: "No asignada")

                        if (subscription.installationType == InstallationType.FIBER ||
                            subscription.installationType == InstallationType.ONLY_TV_FIBER
                        ) {
                            InfoRow("Borne", subscription.borneNumber ?: "No asignado")
                        }

                        InfoRow(
                            "Tipo",
                            subscription.installationType?.toString() ?: "No especificado"
                        )
                    }

                    if (showTr069Section) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ONU / TR-069",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("register_success_section_tr069")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Tr069StatusCard(subscription = subscription)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    if (showRetryTr069) {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_retry_tr069"),
                            enabled = !tr069RetryLoading,
                            onClick = onRetryTr069,
                        ) {
                            if (tr069RetryLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Reintentar TR-069")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_success_continue"),
                        enabled = !tr069RetryLoading,
                        onClick = {
                            onDismiss()
                            onContinue()
                        }
                    ) {
                        Text("Continuar")
                    }
                }
            }
        }
    }
}

@Composable
private fun RegisterSuccessSectionCard(
    title: String,
    testTag: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

/** Alias for tests; delegates to [RegisterSuccessFullScreen]. */
@Composable
internal fun SuccessDialog(
    subscription: Subscription,
    tr069RetryLoading: Boolean = false,
    onRetryTr069: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    RegisterSuccessFullScreen(
        subscription = subscription,
        tr069RetryLoading = tr069RetryLoading,
        onRetryTr069 = onRetryTr069,
        onDismiss = onDismiss,
        onContinue = onContinue
    )
}

@Composable
internal fun Tr069StatusCard(subscription: Subscription) {
    val isComplete = subscription.tr069ProvisionStatus == "COMPLETE"
    val isPending = subscription.tr069ProvisionStatus == "PENDING"
    val containerColor = when {
        isComplete -> MaterialTheme.colorScheme.tertiaryContainer
        isPending -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when {
        isComplete -> MaterialTheme.colorScheme.onTertiaryContainer
        isPending -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onErrorContainer
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tr069_status_card"),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = when {
                    isComplete ->
                        "ONU configurada automáticamente por TR-069. No requiere configuración manual."
                    isPending ->
                        "Aplicando configuración WiFi en la ONU. Espere o reintente."
                    else ->
                        "No se pudo configurar la ONU por TR-069. Configure la ONU manualmente."
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                modifier = Modifier.testTag("tr069_status_message")
            )

            if (!isComplete) {
                subscription.tr069Message?.takeIf { it.isNotBlank() }?.let { reason ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor,
                        modifier = Modifier.testTag("tr069_status_reason")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            InfoRow("SSID 2.4 GHz", subscription.wifiSsid24 ?: "—")
            if (!isComplete) {
                InfoRow("Clave 2.4 GHz", subscription.wifiPassword24 ?: "—")
            }
            InfoRow("SSID 5 GHz", subscription.wifiSsid5 ?: "—")
            if (!isComplete) {
                InfoRow("Clave 5 GHz", subscription.wifiPassword5 ?: "—")
            }
        }
    }
}

@Composable
private fun QueuedOfflineDialog(
    onDismiss: () -> Unit,
    onViewPending: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Guardado en modo offline",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Suscripción guardada localmente en modo Offline. Sincronízala cuando tengas conexión.",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("queued_offline_dialog_view_pending"),
                    onClick = onViewPending
                ) {
                    Text("Ver suscripciones pendientes")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("queued_offline_dialog_continue"),
                    onClick = onDismiss
                ) {
                    Text("Entendido")
                }
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
