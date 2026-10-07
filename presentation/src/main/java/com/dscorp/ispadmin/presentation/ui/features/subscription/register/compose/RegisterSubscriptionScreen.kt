package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.dscorp.ispadmin.domain.model.AccessMode
import com.dscorp.ispadmin.domain.model.GeoLocation
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.PlanResponse
import com.dscorp.ispadmin.domain.model.Subscription
import com.dscorp.ispadmin.observability.ObsReplayPrivacy
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.components.rememberPhotoTaker
import com.dscorp.ispadmin.presentation.ui.features.locationMapView.LocationSelectorComposeDialog
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionDebugActions
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.RegisterSubscriptionTestTags
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionIntent
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionUiEvent
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.RegisterSubscriptionWizardStep
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.SubmissionState
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.resultNotice
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.models.resultSubscription
import java.io.File
import java.util.Locale

@Composable
fun RegisterSubscriptionFormScreen(
    modifier: Modifier = Modifier,
    viewModel: RegisterSubscriptionComposeViewModel,
    context: Context = LocalContext.current,
    onSubscriptionRegisterSuccess: () -> Unit = {},
    onNavigateToPendingSubscriptions: () -> Unit = {},
    onCancelRegistration: () -> Unit = {},
    onViewProvisioning: (Int) -> Unit = {},
    installationOrderId: Int?,
    customerId: Int? = null,
) {
    val locationSetup = rememberLocationSetupState()
    val locationSetupLatest = rememberUpdatedState(locationSetup)
    val viewModelLatest = rememberUpdatedState(viewModel)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var dialogError by remember { mutableStateOf<String?>(null) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showQueuedOfflineDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.isRegistering && !uiState.cancellationInProgress) {
        showLeaveDialog = true
    }
    var showFacadePhotoOptionsDialog by remember { mutableStateOf(false) }
    var showCurrentLocationGate by remember { mutableStateOf(false) }

    val selectFacadePhoto: (Uri) -> Unit = { uri ->
        val file = runCatching { prepareFacadePhotoFile(context = context, uri = uri) }.getOrNull()
        viewModel.onFacadePhotoSelected(uri, file)
    }

    DisposableEffect(Unit) {
        val replayHold = ObsReplayPrivacy.hold()
        onDispose { replayHold.release() }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is RegisterSubscriptionUiEvent.Error -> dialogError = event.message
                    is RegisterSubscriptionUiEvent.Success -> Unit
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
            selectFacadePhoto(uri)
        }
    )

    val facadePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let(selectFacadePhoto)
    }

    LaunchedEffect(Unit) {
        viewModel.loadScreenData(installationOrderId, customerId)
    }

    LaunchedEffect(uiState.registrationCancelled) {
        if (uiState.registrationCancelled) onCancelRegistration()
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
                            viewModel.onFacadePhotoSelected(Uri.fromFile(file), file)
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
                    val facadePhotoFile = if (uiState.preauthorizationOperation == null) {
                        uiState.registerSubscriptionForm.facadePhotoUri?.let { uri ->
                            prepareFacadePhotoFile(context = context, uri = uri)
                        }
                    } else null
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

        uiState.submission.resultSubscription()?.let { subscription ->
            RegisterSuccessFullScreen(
                subscription = subscription,
                plan = uiState.registerSubscriptionForm.selectedPlan,
                tr069RetryLoading = uiState.tr069RetryLoading,
                notice = uiState.submission.resultNotice(),
                onRetryTr069 = subscription.resolvedSubscriptionId()?.let {
                    {
                        dialogError = null
                        viewModel.onIntent(RegisterSubscriptionIntent.RetryTr069(subscription))
                    }
                },
                onViewProgress = subscription.resolvedSubscriptionId()
                    ?.takeIf { uiState.submission is SubmissionState.ProvisioningUnknown }
                    ?.let { id -> { onViewProvisioning(id) } },
                onDismiss = { viewModel.onIntent(RegisterSubscriptionIntent.DismissRegistrationResult) },
                onContinue = onSubscriptionRegisterSuccess
            )
        }

        if (showLeaveDialog) {
            AlertDialog(
                onDismissRequest = { showLeaveDialog = false },
                title = { Text("El registro sigue en curso") },
                text = {
                    Text("Si sales, el servidor continuará el proceso. Al volver a registrar se retomará su estado.")
                },
                confirmButton = {
                    TextButton(
                        modifier = Modifier.testTag(RegisterSubscriptionTestTags.REGISTRATION_LEAVE_CONFIRM),
                        onClick = {
                            showLeaveDialog = false
                            viewModel.onIntent(RegisterSubscriptionIntent.LeaveDuringRegistration)
                            onCancelRegistration()
                        },
                    ) { Text("Salir") }
                },
                dismissButton = {
                    Button(onClick = { showLeaveDialog = false }) { Text("Quedarme") }
                },
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

        if (uiState.showCancelConfirmation) {
            AlertDialog(
                onDismissRequest = {
                    viewModel.onIntent(RegisterSubscriptionIntent.DismissCancelOnuRegistration)
                },
                title = { Text("Cancelar registro") },
                text = {
                    val linkedSubscriptionId = uiState.preauthorizationOperation?.subscriptionId
                    Text(
                        if (linkedSubscriptionId != null) {
                            "La suscripción #$linkedSubscriptionId ya quedó registrada. Si cancelas se dará de baja y se limpiará su configuración en OLT, ACS y MikroTik. ¿Deseas darla de baja?"
                        } else {
                            "Se eliminará la suscripción y se limpiará la configuración asociada en OLT, ACS y MikroTik. ¿Deseas continuar?"
                        }
                    )
                },
                confirmButton = {
                    Button(
                        modifier = Modifier.testTag(RegisterSubscriptionTestTags.REGISTRATION_CANCEL_CONFIRM),
                        onClick = {
                            viewModel.onIntent(RegisterSubscriptionIntent.ConfirmCancelOnuRegistration)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) {
                        Text("Sí, cancelar y limpiar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.onIntent(RegisterSubscriptionIntent.DismissCancelOnuRegistration)
                    }) {
                        Text("Seguir registrando")
                    }
                },
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

        when (registerScreenBusyMode(uiState.isLoading, uiState.isRegistering)) {
            RegisterScreenBusyMode.REGISTERING -> RegistrationProgressOverlay(
                progressMessage = uiState.registrationProgressMessage,
                progressCheckpoints = uiState.registrationProgressCheckpoints,
                accessMode = uiState.registerSubscriptionForm.accessMode,
                onCancel = if (uiState.preauthorizationEnabled) {
                    { viewModel.onIntent(RegisterSubscriptionIntent.CancelOnuRegistration) }
                } else null,
                cancelEnabled = !uiState.cancellationInProgress,
                onViewProgress = (uiState.submission as? SubmissionState.Provisioning)
                    ?.takeIf { !uiState.cancellationInProgress }
                    ?.let { provisioning -> { onViewProvisioning(provisioning.subscriptionId) } },
            )
            RegisterScreenBusyMode.CATALOG -> CatalogLoadingOverlay(
                onCancel = if (
                    uiState.preauthorizationEnabled &&
                    uiState.isPreauthorizationRequestInProgress
                ) {
                    { viewModel.onIntent(RegisterSubscriptionIntent.CancelOnuRegistration) }
                } else null,
                message = when {
                    uiState.isPreauthorizationRequestInProgress && uiState.wizardStep in setOf(
                        RegisterSubscriptionWizardStep.ONU_SELECTION,
                        RegisterSubscriptionWizardStep.ONU_CONFIRMATION,
                    ) ->
                        "Autorizando el equipo en la OLT…"
                    uiState.isPreauthorizationRequestInProgress && uiState.wizardStep == RegisterSubscriptionWizardStep.WAITING_FOR_ACS ->
                        "Esperando conexión con ACS…"
                    else -> "Preparando el registro…"
                },
                description = when {
                    uiState.isPreauthorizationRequestInProgress && uiState.wizardStep in setOf(
                        RegisterSubscriptionWizardStep.ONU_SELECTION,
                        RegisterSubscriptionWizardStep.ONU_CONFIRMATION,
                    ) ->
                        "Estamos comprobando que la ONU pueda conectarse."
                    uiState.isPreauthorizationRequestInProgress && uiState.wizardStep == RegisterSubscriptionWizardStep.WAITING_FOR_ACS ->
                        "El formulario aparecerá cuando la ONU envíe su primer mensaje a ACS."
                    else -> "Cargando equipos, planes y lugares disponibles."
                },
            )
            RegisterScreenBusyMode.NONE -> Unit
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "$label:",
            modifier = Modifier.weight(0.45f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.55f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
internal fun CatalogLoadingOverlay(
    onCancel: (() -> Unit)? = null,
    message: String = "Preparando el registro…",
    description: String = "Cargando equipos, planes y lugares disponibles.",
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
            .testTag(RegisterSubscriptionTestTags.CATALOG_LOADING_OVERLAY),
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
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(RegisterSubscriptionTestTags.CATALOG_LOADING_MESSAGE)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (onCancel != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag(RegisterSubscriptionTestTags.REGISTRATION_CANCEL_ACTION),
                    ) {
                        Text("Cancelar registro")
                    }
                }
            }
        }
    }
}

@Composable
internal fun RegistrationProgressOverlay(
    progressMessage: String? = null,
    progressCheckpoints: List<com.dscorp.ispadmin.domain.model.RegistrationProgressCheckpoint> = emptyList(),
    accessMode: AccessMode = AccessMode.PPPOE_DYNAMIC,
    onCancel: (() -> Unit)? = null,
    cancelEnabled: Boolean = true,
    onViewProgress: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
            .testTag(RegisterSubscriptionTestTags.PROGRESS_OVERLAY),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .heightIn(max = 600.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = registrationProgressStepMessage(progressMessage, progressCheckpoints, accessMode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(RegisterSubscriptionTestTags.PROGRESS_STEP)
                )
                progressCheckpoints.forEach { checkpoint ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                        color = if (checkpoint.failure != null) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = registrationProgressStageLabel(checkpoint.stage, accessMode),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (checkpoint.failure != null) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                                Text(
                                    text = registrationProgressStateLabel(checkpoint.state),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (checkpoint.failure != null) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                            if (checkpoint.attempts > 1) {
                                Text(
                                    text = "Intento ${checkpoint.attempts}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            checkpoint.failure?.let { failure ->
                                Text(
                                    text = "${failure.message} (${failure.code})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Esto puede tomar hasta 2 minutos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(RegisterSubscriptionTestTags.PROGRESS_HINT)
                )
                if (onViewProgress != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onViewProgress,
                        modifier = Modifier.testTag(RegisterSubscriptionTestTags.REGISTRATION_VIEW_PROGRESS),
                    ) {
                        Text("Ver progreso")
                    }
                }
                if (onCancel != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = onCancel,
                        enabled = cancelEnabled,
                        modifier = Modifier.testTag(RegisterSubscriptionTestTags.REGISTRATION_CANCEL_ACTION),
                    ) {
                        Text(if (cancelEnabled) "Cancelar registro" else "Cancelando…")
                    }
                }
            }
        }
    }
}

@Composable
internal fun RegisterSuccessFullScreen(
    subscription: Subscription,
    plan: PlanResponse? = null,
    tr069RetryLoading: Boolean = false,
    notice: String? = null,
    onRetryTr069: (() -> Unit)? = null,
    onViewProgress: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    val tr069Status = subscription.tr069ProvisionStatus
    val requiresManualTr069 = tr069Status == "MANUAL_REQUIRED" || subscription.tr069RequiresManualConfig
    val isPendingTr069 = tr069Status == "PENDING"
    val isFailedTr069 = tr069Status == "FAILED"
    val showRetryTr069 = (requiresManualTr069 || isPendingTr069 || isFailedTr069) && onRetryTr069 != null
    val usesPppoe = AccessMode.parse(subscription.accessMode)?.usesPppoe() == true
    val monthlyPrice = subscription.price ?: plan?.price

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

                    if (notice != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_result_notice"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = notice,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                if (onViewProgress != null) {
                                    TextButton(onClick = onViewProgress) { Text("Ver progreso") }
                                }
                            }
                        }
                    }

                    if (tr069RetryLoading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wifi_retry_feedback"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Actualizando redes Wi-Fi…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.testTag("wifi_retry_feedback_message")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    RegisterSuccessSectionCard(
                        title = "Suscriptor",
                        testTag = "register_success_section_subscriber"
                    ) {
                        Text(
                            text = listOfNotNull(
                                subscription.firstName?.takeIf(String::isNotBlank),
                                subscription.lastName?.takeIf(String::isNotBlank),
                            ).joinToString(" ").ifBlank { "Suscriptor" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        subscription.dni?.takeIf(String::isNotBlank)?.let { InfoRow("DNI", it) }
                        subscription.phone?.takeIf(String::isNotBlank)?.let { InfoRow("Teléfono", it) }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    RegisterSuccessSectionCard(
                        title = "Plan contratado",
                        testTag = "register_success_section_plan"
                    ) {
                        Text(
                            text = plan?.name?.takeIf(String::isNotBlank) ?: "Plan de internet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        monthlyPrice?.let { price ->
                            InfoRow("Mensualidad", "S/ ${"%.2f".format(Locale.US, price)} / mes")
                        }
                        plan?.downloadSpeed?.takeIf(String::isNotBlank)?.let { speed ->
                            InfoRow("Bajada", "$speed Mbps")
                        }
                        plan?.uploadSpeed?.takeIf(String::isNotBlank)?.let { speed ->
                            InfoRow("Subida", "$speed Mbps")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    RegisterSuccessSectionCard(
                        title = "Acceso a internet",
                        testTag = "register_success_section_access"
                    ) {
                        if (usesPppoe) {
                            InfoRow("Usuario PPPoE", subscription.pppoeUsername?.takeIf(String::isNotBlank) ?: "No disponible")
                            InfoRow("Contraseña PPPoE", subscription.pppoePassword?.takeIf(String::isNotBlank) ?: "No disponible")
                        } else {
                            InfoRow("IP", subscription.ip?.takeIf(String::isNotBlank) ?: "No disponible")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    RegisterSuccessSectionCard(
                        title = "Redes Wi-Fi",
                        testTag = RegisterSubscriptionTestTags.SUCCESS_SECTION_WIFI
                    ) {
                        InfoRow("SSID 2.4 GHz", subscription.wifiSsid24?.takeIf(String::isNotBlank) ?: "No disponible")
                        InfoRow("Contraseña 2.4 GHz", subscription.wifiPassword24?.takeIf(String::isNotBlank) ?: "No disponible")
                        Spacer(modifier = Modifier.height(4.dp))
                        InfoRow("SSID 5 GHz", subscription.wifiSsid5?.takeIf(String::isNotBlank) ?: "No disponible")
                        InfoRow("Contraseña 5 GHz", subscription.wifiPassword5?.takeIf(String::isNotBlank) ?: "No disponible")
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
                                .testTag("btn_retry_wifi"),
                            enabled = !tr069RetryLoading,
                            onClick = onRetryTr069,
                        ) {
                            if (tr069RetryLoading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text("Reintentando Wi-Fi…")
                                }
                            } else {
                                Text("Reintentar Wi-Fi")
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
                        Text("Finalizar instalación")
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
        shape = RoundedCornerShape(18.dp),
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
    plan: PlanResponse? = null,
    tr069RetryLoading: Boolean = false,
    onRetryTr069: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    RegisterSuccessFullScreen(
        subscription = subscription,
        plan = plan,
        tr069RetryLoading = tr069RetryLoading,
        onRetryTr069 = onRetryTr069,
        onDismiss = onDismiss,
        onContinue = onContinue
    )
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
