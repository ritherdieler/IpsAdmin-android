package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
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
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionMenu.SEE_DETAILS
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionMenu.SHOW_PAYMENT_HISTORY
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionMenu.UPDATE_LOCATION
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
            // Search card with nice material design
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Buscar suscripción",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val filters = filters
                    var selectedFilter by remember { mutableStateOf(filters[0]) }
                    var searchQuery by remember { mutableStateOf("") }
                    var lastNameQuery by remember { mutableStateOf("") }

                    // Show filter tabs
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filters.forEach { filter ->
                            val isSelected = selectedFilter == filter
                            val filterTag = when (filter) {
                                is SubscriptionFilter.BY_NAME -> SubscriptionFinderTestTags.FILTER_NAME
                                is SubscriptionFilter.BY_DOCUMENT -> SubscriptionFinderTestTags.FILTER_DOCUMENT
                                is SubscriptionFilter.BY_DATE -> SubscriptionFinderTestTags.FILTER_DATE
                                is SubscriptionFilter.BY_IP -> SubscriptionFinderTestTags.FILTER_IP
                                is SubscriptionFilter.BY_CODE -> SubscriptionFinderTestTags.FILTER_CODE
                            }
                            FilterChip(
                                filter = filter,
                                isSelected = isSelected,
                                testTag = filterTag,
                                onClick = {
                                    // Restablecer los campos de búsqueda al cambiar de filtro
                                    if (selectedFilter != filter) {
                                        searchQuery = ""
                                        lastNameQuery = ""
                                    }
                                    selectedFilter = filter
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Contenido específico según el tipo de filtro
                    when (selectedFilter) {
                        is SubscriptionFilter.BY_NAME -> {
                            // Campo único de búsqueda por nombre o apellido
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { newValue ->
                                    val upperValue = newValue.uppercase()
                                    searchQuery = upperValue
                                    ObservabilityComposeText.report(
                                        tag = SubscriptionFinderTestTags.QUERY_NAME,
                                        label = "Nombre o apellido",
                                        value = upperValue
                                    )
                                    coroutinesScope.launch {
                                        viewModel.documentNumberFlow.emit(
                                            SubscriptionFilter.BY_NAME(
                                                name = upperValue,
                                                lastName = ""
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(SubscriptionFinderTestTags.QUERY_NAME),
                                label = { Text("Nombre o apellido") },
                                placeholder = { Text("Ej: Juan Pérez") },
                                trailingIcon = {
                                    if (uiState.isSearching) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.Search,
                                            contentDescription = SubscriptionFinderContentDescriptions.SEARCH_LOADING,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        is SubscriptionFilter.BY_DOCUMENT -> {
                            // Campo único para documento
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { newValue ->
                                    searchQuery = newValue
                                    ObservabilityComposeText.report(
                                        tag = SubscriptionFinderTestTags.QUERY_DOCUMENT,
                                        label = "DNI",
                                        value = newValue
                                    )
                                    coroutinesScope.launch {
                                        viewModel.documentNumberFlow.emit(
                                            SubscriptionFilter.BY_DOCUMENT(
                                                newValue
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(SubscriptionFinderTestTags.QUERY_DOCUMENT),
                                label = { Text("DNI") },
                                placeholder = { Text("Ingrese número de documento") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = "Buscar",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        is SubscriptionFilter.BY_DATE -> {
                            // Reemplazamos la implementación por completo
                            DateRangeSelector(
                                onSearch = { startDate, endDate ->
                                    coroutinesScope.launch {
                                        viewModel.documentNumberFlow.emit(
                                            SubscriptionFilter.BY_DATE(startDate, endDate)
                                        )
                                    }
                                }
                            )
                        }

                        is SubscriptionFilter.BY_IP -> {
                            // Campo único para IP
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { newValue ->
                                    val filteredValue = newValue.filter { it.isDigit() || it == '.' }
                                    searchQuery = filteredValue
                                    ObservabilityComposeText.report(
                                        tag = SubscriptionFinderTestTags.QUERY_IP,
                                        label = "IP",
                                        value = filteredValue
                                    )
                                    coroutinesScope.launch {
                                        viewModel.documentNumberFlow.emit(
                                            SubscriptionFilter.BY_IP(filteredValue)
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(SubscriptionFinderTestTags.QUERY_IP),
                                label = { Text("IP") },
                                placeholder = { Text("Ej: 192.168.1.1") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = "Buscar",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                )
                            )
                        }
                        is SubscriptionFilter.BY_CODE -> {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { newValue ->
                                    val filteredValue = newValue.filter { it.isDigit() }
                                    searchQuery = filteredValue
                                    ObservabilityComposeText.report(
                                        tag = SubscriptionFinderTestTags.QUERY_CODE,
                                        label = "Código",
                                        value = filteredValue
                                    )
                                    coroutinesScope.launch {
                                        viewModel.documentNumberFlow.emit(
                                            SubscriptionFilter.BY_CODE(filteredValue)
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(SubscriptionFinderTestTags.QUERY_CODE),
                                label = { Text("Código") },
                                placeholder = { Text("Ingrese ID de suscripción") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = "Buscar",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                )
                            )
                        }
                    }
                }
            }

            // Results section
            if (uiState.cancelSubscriptionState == CancelSubscriptionState.Loading ||
                (uiState.isSearching && uiState.subscriptions.isEmpty())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (uiState.subscriptions.isEmpty()) {
                // Estado vacío diferenciado: inicial vs sin resultados
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.searchPerformed) Icons.Outlined.Info else Icons.Filled.Search,
                            contentDescription = SubscriptionFinderContentDescriptions.EMPTY_STATE_ICON,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (uiState.searchPerformed) "No se encontraron resultados" else "Busca una suscripción",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (uiState.searchPerformed) "Intente con otros criterios de búsqueda" else "Ingresa un nombre, apellido o documento para comenzar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                // Main subscription finder content
                SubscriptionFinder(
                    subscriptions = uiState.subscriptions,
                    isLoadingMore = uiState.isSearching,
                    onLoadMore = viewModel::loadNextPage,
                    onMenuItemSelected = { menuItem, subscription ->
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
                            onShowRetryTr069Dialog = { showRetryTr069Dialog = true }
                        )
                    },
                    onSubscriptionExpanded = { subscription, expanded ->
                        // Handle the expanded state change
                        expandedSubscriptionId = if (expanded) subscription.id else null

                        // Initialize customer form data when expanding
                        if (expanded) {
                            // Set the selected subscription first
                            viewModel.setSelectedSubscription(subscription)

                            // Then initialize the form data for this subscription
                            viewModel.initCustomerFormData(subscription)
                        }
                    },
                    expandedSubscriptionId = expandedSubscriptionId,
                    customerFormData = uiState.customerFormData,
                    placesState = uiState.placesState,
                    saveState = uiState.saveSubscriptionState,
                    onFieldChange = viewModel::updateCustomerFormField,
                    onPlaceSelected = viewModel::onPlaceSelected,
                    onUpdatePlaceId = viewModel::updateCustomerPlaceId,
                    onSaveCustomer = viewModel::saveCustomerData
                )
            }
        }
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

@Composable
fun FilterChip(
    filter: SubscriptionFilter,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = filter.valueName,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/**
 * Handles menu actions for subscription items
 */
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

/**
 * Componente para seleccionar un rango de fechas para búsqueda
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeSelector(
    onSearch: (String, String) -> Unit
) {
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Función para formatear la fecha
    fun formatDate(millis: Long): String {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = millis
        }
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Rango de fechas",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Campo de fecha inicial
            OutlinedTextField(
                value = startDate,
                onValueChange = { },
                modifier = Modifier
                    .weight(1f)
                    .testTag(SubscriptionFinderTestTags.DATE_START)
                    .clickable { showStartDatePicker = true },
                label = { Text("Fecha inicial") },
                placeholder = { Text("Seleccione") },
                readOnly = true,
                enabled = false,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = "Seleccionar fecha inicial",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                shape = RoundedCornerShape(8.dp)
            )

            // Campo de fecha final
            OutlinedTextField(
                value = endDate,
                onValueChange = { },
                modifier = Modifier
                    .weight(1f)
                    .testTag(SubscriptionFinderTestTags.DATE_END)
                    .clickable { showEndDatePicker = true },
                label = { Text("Fecha final") },
                placeholder = { Text("Seleccione") },
                readOnly = true,
                enabled = false,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = "Seleccionar fecha final",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                shape = RoundedCornerShape(8.dp)
            )
        }

        // Botón de búsqueda
        Button(
            onClick = {
                if (startDate.isNotEmpty() || endDate.isNotEmpty()) {
                    onSearch(startDate, endDate)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .testTag(SubscriptionFinderTestTags.DATE_SUBMIT),
            enabled = startDate.isNotEmpty() || endDate.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = SubscriptionFinderContentDescriptions.DATE_SEARCH_ICON,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Buscar",
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }

    // Diálogo de selección de fecha inicial
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState()
        val confirmEnabled = remember {
            derivedStateOf { datePickerState.selectedDateMillis != null }
        }

        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag(SubscriptionFinderTestTags.DATE_PICKER_CONFIRM),
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            startDate = formatDate(it)
                            ObservabilityComposeText.report(
                                tag = SubscriptionFinderTestTags.DATE_START,
                                label = "Fecha inicial",
                                value = startDate
                            )
                        }
                        showStartDatePicker = false
                    },
                    enabled = confirmEnabled.value
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    modifier = Modifier.testTag(SubscriptionFinderTestTags.DATE_PICKER_DISMISS),
                    onClick = { showStartDatePicker = false }
                ) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Diálogo de selección de fecha final
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState()
        val confirmEnabled = remember {
            derivedStateOf { datePickerState.selectedDateMillis != null }
        }

        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag(SubscriptionFinderTestTags.DATE_PICKER_CONFIRM),
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            endDate = formatDate(it)
                            ObservabilityComposeText.report(
                                tag = SubscriptionFinderTestTags.DATE_END,
                                label = "Fecha final",
                                value = endDate
                            )
                        }
                        showEndDatePicker = false
                    },
                    enabled = confirmEnabled.value
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    modifier = Modifier.testTag(SubscriptionFinderTestTags.DATE_PICKER_DISMISS),
                    onClick = { showEndDatePicker = false }
                ) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
