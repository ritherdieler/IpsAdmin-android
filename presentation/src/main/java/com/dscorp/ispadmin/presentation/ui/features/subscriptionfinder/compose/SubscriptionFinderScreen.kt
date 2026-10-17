package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.observability.ObservabilityComposeText
import androidx.navigation.NavController
import com.dscorp.components.components.formfields.MyOutlinedTextField
import com.dscorp.ispadmin.domain.model.GeoLocation
import com.dscorp.ispadmin.domain.model.SubscriptionResume
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Payment
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Subscription
import com.dscorp.ispadmin.presentation.ui.components.Loader
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionMenu.CANCEL_SUBSCRIPTION
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionMenu.CHANGE_NAP_BOX
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionMenu.EDIT_PLAN_SUBSCRIPTION
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionMenu.MIGRATE_TO_FIBER
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionMenu.REACTIVATE_SERVICE
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionMenu.REBOOT_FIBER_ONU
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionMenu.RETRY_TR069
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionMenu.NEW_SERVICE_FOR_CUSTOMER
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionMenu.SEE_DETAILS
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionMenu.SHOW_PAYMENT_HISTORY
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionMenu.UPDATE_LOCATION
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

const val SUBSCRIPTION_ID = "subscriptionId"

/**
 * Main screen for finding and managing subscriptions.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SubscriptionFinderScreen(
    navController: NavController,
    viewModel: SubscriptionFinderViewModel = koinViewModel(),
    onShowMapSelector: (GeoLocation?) -> Unit,
    onGetCurrentLocation: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutinesScope = rememberCoroutineScope()
    var showCancelSubscriptionConfirmDialog by remember { mutableStateOf(false) }
    var showReactivateServiceDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showChangeNapBoxDialog by remember { mutableStateOf(false) }
    var showRebootOnuDialog by remember { mutableStateOf(false) }
    var showRetryTr069Dialog by remember { mutableStateOf(false) }
    var deferCutTarget by remember { mutableStateOf<SubscriptionResume?>(null) }
    var showDateSearch by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }
    // Escucha el resultado del guardado y muestra un mensaje visible al usuario.
    LaunchedEffect(Unit) {
        viewModel.customerSaveMessages.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
        }
    }
    // Muestra un mensaje visible cuando la búsqueda falla (antes el error era silencioso).
    LaunchedEffect(uiState.searchError) {
        uiState.searchError?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
            viewModel.clearSearchError()
        }
    }
    // Track which subscription card is expanded
    var expandedSubscriptionId by remember { mutableStateOf<Int?>(null) }

    // Handle reactivation errors and success
    LaunchedEffect(uiState.reactivateServiceState) {
        when (val state = uiState.reactivateServiceState) {
            is ReactivateServiceState.Error -> {
                showReactivateServiceDialog = false
                snackbarHostState.showSnackbar(
                    message = state.error?:"Error al reactivar el servicio. Intente nuevamente.",
                    actionLabel = "Cerrar",
                    duration = androidx.compose.material3.SnackbarDuration.Indefinite
                )
                viewModel.clearReactivateServiceState()
            }
            is ReactivateServiceState.Success -> {
                showReactivateServiceDialog = false
                snackbarHostState.showSnackbar(
                    message = "Servicio reactivado exitosamente",
                    actionLabel = "Cerrar",
                    duration = androidx.compose.material3.SnackbarDuration.Long
                )
                viewModel.clearReactivateServiceState()
            }
            else -> {}
        }
    }

    LaunchedEffect(uiState.rebootOnuState) {
        when (val state = uiState.rebootOnuState) {
            is RebootOnuState.Error -> {
                showRebootOnuDialog = false
                snackbarHostState.showSnackbar(
                    message = state.message ?: "No se pudo reiniciar la ONU",
                    actionLabel = "Cerrar",
                    duration = androidx.compose.material3.SnackbarDuration.Indefinite
                )
                viewModel.clearRebootOnuState()
            }
            is RebootOnuState.Success -> {
                showRebootOnuDialog = false
                snackbarHostState.showSnackbar(
                    message = "Reinicio de ONU enviado correctamente",
                    actionLabel = "Cerrar",
                    duration = androidx.compose.material3.SnackbarDuration.Long
                )
                viewModel.clearRebootOnuState()
            }
            else -> {}
        }
    }

    LaunchedEffect(uiState.retryTr069State) {
        when (val state = uiState.retryTr069State) {
            is RetryTr069State.Error -> {
                showRetryTr069Dialog = false
                snackbarHostState.showSnackbar(
                    message = state.message ?: "No se pudo reintentar el aprovisionamiento TR-069",
                    actionLabel = "Cerrar",
                    duration = androidx.compose.material3.SnackbarDuration.Indefinite
                )
                viewModel.clearRetryTr069State()
            }
            is RetryTr069State.Success -> {
                showRetryTr069Dialog = false
                snackbarHostState.showSnackbar(
                    message = state.message,
                    actionLabel = "Cerrar",
                    duration = androidx.compose.material3.SnackbarDuration.Long
                )
                viewModel.clearRetryTr069State()
            }
            else -> {}
        }
    }

    // For scrolling behavior with topAppBar
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Function to show the map selection dialog (using callback now)
    val showMapSelection: () -> Unit = {
        val initialGeoLocation = uiState.selectedSubscription?.location
        onShowMapSelector(initialGeoLocation)
    }

    // Recargar datos cuando se vuelve a la pantalla
    LaunchedEffect(Unit) {
        viewModel.reloadLastSearch()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SubscriptionSearchContent(
                state = uiState,
                onQueryChange = viewModel::onSmartQueryChanged,
                onFilterSelected = viewModel::onResultFilterSelected,
                onQuickAccess = viewModel::openQuickAccess,
                onOpenSubscription = { subscription ->
                    viewModel.onSubscriptionOpened(subscription)
                    navController.navigate(Subscription.Details(subscription.id))
                },
                onOpenRecent = { id -> navController.navigate(Subscription.Details(id)) },
                onClearRecents = viewModel::clearRecents,
                onOpenFilters = { showDateSearch = true },
                onLoadMore = viewModel::loadNextPage,
                rowMenu = { subscription, expanded, onDismiss ->
                    SubscriptionRowMenu(
                        subscription = subscription,
                        expanded = expanded,
                        onDismiss = onDismiss,
                        onMenuItemSelected = { menuItem ->
                            handleMenuAction(
                                menuItem = menuItem,
                                subscription = subscription,
                                navController = navController,
                                context = context,
                                viewModel = viewModel,
                                onShowCancelDialog = { showCancelSubscriptionConfirmDialog = true },
                                onShowChangeNapBoxDialog = { showChangeNapBoxDialog = true },
                                onShowReactivateDialog = { showReactivateServiceDialog = true },
                                onShowRebootOnuDialog = { showRebootOnuDialog = true },
                                onShowRetryTr069Dialog = { showRetryTr069Dialog = true },
                                onShowDeferCutDialog = { deferCutTarget = subscription },
                            )
                        },
                        onEditHolder = {
                            val expand = expandedSubscriptionId != subscription.id
                            expandedSubscriptionId = if (expand) subscription.id else null
                            if (expand) {
                                viewModel.setSelectedSubscription(subscription)
                                viewModel.initCustomerFormData(subscription)
                            }
                        },
                    )
                },
                rowExpansion = { subscription ->
                    val formData = uiState.customerFormData
                    if (expandedSubscriptionId == subscription.id && formData?.subscriptionId == subscription.id) {
                        CustomerDataForm(
                            formData = formData,
                            placesState = uiState.placesState,
                            saveState = uiState.saveSubscriptionState,
                            onFieldChange = viewModel::updateCustomerFormField,
                            onPlaceSelected = viewModel::onPlaceSelected,
                            onUpdatePlace = viewModel::updateCustomerPlaceId,
                            onSaveClick = viewModel::saveCustomerData,
                        )
                    }
                },
            )
        }
    }

    if (showDateSearch) {
        DateSearchDialog(
            onDismiss = { showDateSearch = false },
            onSearch = { startDate, endDate ->
                showDateSearch = false
                coroutinesScope.launch {
                    viewModel.documentNumberFlow.emit(SubscriptionFilter.BY_DATE(startDate, endDate))
                }
            },
        )
    }

    // Handle cancel subscription state
    HandleCancelSubscriptionState(
        cancelState = uiState.cancelSubscriptionState,
        context = context,
        selectedSubscription = uiState.selectedSubscription,
        onRemoveSubscription = viewModel::removeSubscriptionFromList
    )

    // Cancel subscription confirmation dialog
    if (showCancelSubscriptionConfirmDialog) {
        CancelSubscriptionDialog(
            onDismiss = { showCancelSubscriptionConfirmDialog = false },
            onConfirm = {
                uiState.selectedSubscription?.id?.let {
                    showCancelSubscriptionConfirmDialog = false
                    viewModel.cancelSubscription(it)
                }
            }
        )
    }

    // Reactivate service confirmation dialog
        if (showReactivateServiceDialog) {
            ReactivateServiceDialog(
                onDismiss = {
                    if (uiState.reactivateServiceState != ReactivateServiceState.Loading) {
                        showReactivateServiceDialog = false
                    }
                },
                onConfirm = {
                    uiState.selectedSubscription?.id?.let {
                        viewModel.reactivateService(it)
                    }
                },
                isLoading = uiState.reactivateServiceState == ReactivateServiceState.Loading,
                notes = uiState.reactivationNotes,
                onNotesChange = { viewModel.updateReactivationNotes(it) }
            )
        }

    if (showRebootOnuDialog) {
        RebootOnuConfirmDialog(
            onDismiss = {
                if (uiState.rebootOnuState != RebootOnuState.Loading) {
                    showRebootOnuDialog = false
                }
            },
            onConfirm = {
                uiState.selectedSubscription?.id?.let { viewModel.rebootFiberOnu(it) }
            },
            isLoading = uiState.rebootOnuState == RebootOnuState.Loading
        )
    }

    LaunchedEffect(uiState.deferCutMessage) {
        uiState.deferCutMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearDeferCutMessage()
        }
    }

    deferCutTarget?.let { target ->
        DeferCutDialog(
            onDismiss = { deferCutTarget = null },
            onConfirm = { untilDate, reason ->
                viewModel.deferCut(target.id, untilDate, reason)
                deferCutTarget = null
            },
        )
    }

    if (showRetryTr069Dialog) {
        RetryTr069ConfirmDialog(
            onDismiss = {
                if (uiState.retryTr069State != RetryTr069State.Loading) {
                    showRetryTr069Dialog = false
                }
            },
            onConfirm = {
                uiState.selectedSubscription?.id?.let { viewModel.retryTr069(it) }
            },
            isLoading = uiState.retryTr069State == RetryTr069State.Loading
        )
    }

    // Change NAP box dialog
    if (showChangeNapBoxDialog) {
        ChangeNapBoxDialog(
            viewModel = viewModel,
            selectedSubscription = uiState.selectedSubscription,
            napBoxesState = uiState.napBoxesState,
            context = context,
            onDismiss = { showChangeNapBoxDialog = false }
        )
    }

    // Location update dialog
    if (uiState.showLocationUpdateDialog) {
        LocationUpdateDialog(
            selectedSubscription = uiState.selectedSubscription,
            saveState = uiState.saveSubscriptionState,
            latitude = uiState.editableLatitude,
            longitude = uiState.editableLongitude,
            onShowMap = showMapSelection,
            onGetCurrentLocationClick = {
                onGetCurrentLocation()
            },
            isFetchingCurrentLocation = uiState.isFetchingCurrentLocation,
            onUpdateLocation = { viewModel.updateSubscriptionLocation() },
            onDismiss = { viewModel.toggleLocationUpdateDialog(false) }
        )
    }
}

private fun handleMenuAction(
    menuItem: SubscriptionMenu,
    subscription: SubscriptionResume,
    navController: NavController,
    context: Context,
    viewModel: SubscriptionFinderViewModel,
    onShowCancelDialog: () -> Unit,
    onShowChangeNapBoxDialog: () -> Unit,
    onShowReactivateDialog: () -> Unit,
    onShowRebootOnuDialog: () -> Unit,
    onShowRetryTr069Dialog: () -> Unit,
    onShowDeferCutDialog: () -> Unit = {},
) {
    when (menuItem) {
        SHOW_PAYMENT_HISTORY -> {
            navController.navigate(
                Payment.History(
                    subscription.id,
                    subscription.serviceStatus.toString()
                )
            )
        }

        EDIT_PLAN_SUBSCRIPTION -> {
            navController.navigate(
                Subscription.ChangePlan(subscription.id)
            )
        }

        SEE_DETAILS -> {
            navController.navigate(
                Subscription.Details(subscription.id)
            )
        }

        NEW_SERVICE_FOR_CUSTOMER -> {
            subscription.customerId?.let { customerId ->
                navController.navigate(Subscription.Register(customerId = customerId))
            }
        }

        MIGRATE_TO_FIBER -> {
            navController.navigate(
                Subscription.Migrate(subscription.id)
            )
        }

        CANCEL_SUBSCRIPTION -> {
            viewModel.setSelectedSubscription(subscription)
            onShowCancelDialog()
        }

        REACTIVATE_SERVICE -> {
            viewModel.setSelectedSubscription(subscription)
            onShowReactivateDialog()
        }

        CHANGE_NAP_BOX -> {
            viewModel.setSelectedSubscription(subscription)
            onShowChangeNapBoxDialog()
        }

        UPDATE_LOCATION -> {
            viewModel.setSelectedSubscription(subscription)
            viewModel.toggleLocationUpdateDialog(true)
        }

        REBOOT_FIBER_ONU -> {
            viewModel.setSelectedSubscription(subscription)
            onShowRebootOnuDialog()
        }

        RETRY_TR069 -> {
            viewModel.setSelectedSubscription(subscription)
            onShowRetryTr069Dialog()
        }

        SubscriptionMenu.DEFER_CUT -> onShowDeferCutDialog()
    }
}

/**
 * Component to handle cancel subscription state changes
 */
@Composable
private fun HandleCancelSubscriptionState(
    cancelState: CancelSubscriptionState,
    context: Context,
    selectedSubscription: SubscriptionResume?,
    onRemoveSubscription: (Int) -> Unit
) {
    when (cancelState) {
        CancelSubscriptionState.Empty -> {}
        CancelSubscriptionState.Error -> {
            Toast.makeText(
                context,
                "Ocurrió un error al cancelar la suscripción",
                Toast.LENGTH_LONG
            ).show()
        }

        CancelSubscriptionState.Loading -> Loader(Modifier.background(color = Color.White))
        CancelSubscriptionState.Success -> {
            Toast.makeText(context, "Suscripción cancelada correctamente", Toast.LENGTH_LONG)
                .show()
            selectedSubscription?.id?.let { onRemoveSubscription(it) }
        }
    }
}

/**
 * Dialog to confirm subscription cancellation
 */
@Composable
private fun CancelSubscriptionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cancelar Suscripción",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        },
        text = {
            Column(modifier = Modifier.padding(top = 0.dp)) {
                Text(
                    text = "¿Está seguro que desea cancelar esta suscripción?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Esta acción no se puede deshacer y el servicio se suspenderá inmediatamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.padding(end = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    modifier = Modifier.testTag(SubscriptionFinderTestTags.CANCEL_DIALOG_DISMISS),
                    onClick = onDismiss
                ) {
                    Text(
                        text = "Volver",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    modifier = Modifier.testTag(SubscriptionFinderTestTags.CANCEL_DIALOG_CONFIRM),
                    onClick = onConfirm,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "Cancelar Suscripción",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun RebootOnuConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Reiniciar ONU",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        },
        text = {
            Column(modifier = Modifier.padding(top = 0.dp)) {
                Text(
                    text = "¿Enviar reinicio remoto a la ONU del cliente?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "El equipo puede tardar unos minutos en volver a estar en línea.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.padding(end = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    modifier = Modifier.testTag(SubscriptionFinderTestTags.REBOOT_DIALOG_DISMISS),
                    onClick = onDismiss,
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Volver",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isLoading)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    modifier = Modifier.testTag(SubscriptionFinderTestTags.REBOOT_DIALOG_CONFIRM),
                    onClick = onConfirm,
                    enabled = !isLoading,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Enviando...",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    } else {
                        Text(
                            text = "Reiniciar ONU",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun RetryTr069ConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Reintentar TR-069",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        },
        text = {
            Column(modifier = Modifier.padding(top = 0.dp)) {
                Text(
                    text = "¿Reintentar el aprovisionamiento TR-069 de esta suscripción?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "El equipo vuelve a aplicar WiFi e IP. Puede tardar unos minutos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.padding(end = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    modifier = Modifier.testTag(SubscriptionFinderTestTags.RETRY_TR069_DIALOG_DISMISS),
                    onClick = onDismiss,
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Volver",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isLoading)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    modifier = Modifier.testTag(SubscriptionFinderTestTags.RETRY_TR069_DIALOG_CONFIRM),
                    onClick = onConfirm,
                    enabled = !isLoading,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reintentando...",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    } else {
                        Text(
                            text = "Reintentar",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Dialog to confirm service reactivation
 */
@Composable
private fun ReactivateServiceDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean = false,
    notes: String = "",
    onNotesChange: (String) -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Reactivar Servicio",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        },
        text = {
            Column(modifier = Modifier.padding(top = 0.dp)) {
                Text(
                    text = "¿Está seguro que desea reactivar este servicio?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "El servicio se reactivará y el cliente podrá acceder nuevamente a internet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Nota: Para realizar la reactivación, el cliente debe tener menos de 2 facturas pendientes de pago.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                MyOutlinedTextField(
                    value = notes,
                    onValueChange = {
                        onNotesChange(it)
                        ObservabilityComposeText.report(
                            tag = SubscriptionFinderTestTags.REACTIVATE_NOTES,
                            label = "Notas de reactivación",
                            value = it
                        )
                    },
                    label = "Notas de reactivación (opcional)",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(SubscriptionFinderTestTags.REACTIVATE_NOTES),
                    enabled = !isLoading,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.padding(end = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    modifier = Modifier.testTag(SubscriptionFinderTestTags.REACTIVATE_DIALOG_DISMISS),
                    onClick = onDismiss,
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Volver",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isLoading) 
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    modifier = Modifier.testTag(SubscriptionFinderTestTags.REACTIVATE_DIALOG_CONFIRM),
                    onClick = onConfirm,
                    enabled = !isLoading,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reactivando...",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    } else {
                        Text(
                            text = "Reactivar Servicio",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Asks for the date (within the current month) and the reason to defer the cut for debt. */
@Composable
private fun DeferCutDialog(onDismiss: () -> Unit, onConfirm: (untilDate: String, reason: String) -> Unit) {
    var untilDate by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val validDate = Regex("\\d{4}-\\d{2}-\\d{2}").matches(untilDate.trim())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Postergar corte") },
        text = {
            Column {
                Text("No se corta hasta la fecha indicada, dentro del mes en curso. El cierre de mes cancela igual con 2 facturas impagas.")
                OutlinedTextField(
                    value = untilDate,
                    onValueChange = { untilDate = it },
                    label = { Text("No cortar hasta (AAAA-MM-DD)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo") },
                )
            }
        },
        confirmButton = {
            TextButton(enabled = validDate && reason.isNotBlank(), onClick = { onConfirm(untilDate.trim(), reason.trim()) }) {
                Text("Postergar corte")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
