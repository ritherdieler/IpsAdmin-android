package com.dscorp.ispadmin.navigation

import androidx.lifecycle.compose.currentStateAsState
import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.dscorp.ispadmin.BuildConfig
import com.dscorp.ispadmin.data.response.AssistanceTicketStatus
import com.dscorp.ispadmin.domain.model.GeoLocation
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.domain.usecase.subscription.OnuRegistrationOperationUseCase
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.AsyncImageViewer
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Dashboard
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Installation
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Outlay
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Payment
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Profile
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Subscription
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.SupportTicket
import com.dscorp.ispadmin.presentation.ui.components.HorizontalImageCarousel
import com.dscorp.ispadmin.presentation.ui.features.dashboard.DashBoardComposeScreen
import com.dscorp.ispadmin.presentation.ui.features.installationorders.CreateInstallationOrderScreen
import com.dscorp.ispadmin.presentation.ui.features.installationorders.CreateInstallationOrderViewModel
import com.dscorp.ispadmin.presentation.ui.features.installationorders.InstallationOrderEvent
import com.dscorp.ispadmin.presentation.ui.features.installationorders.InstallationOrderListEvent
import com.dscorp.ispadmin.presentation.ui.features.installationorders.InstallationOrderListScreen
import com.dscorp.ispadmin.presentation.ui.features.installationorders.InstallationOrderListViewModel
import com.dscorp.ispadmin.presentation.ui.features.locationMapView.LocationSelectorComposeDialog
import com.dscorp.ispadmin.presentation.ui.features.main.MainEvent
import com.dscorp.ispadmin.presentation.ui.features.main.MainShell
import com.dscorp.ispadmin.presentation.ui.features.main.MainViewModel
import com.dscorp.ispadmin.presentation.ui.features.more.MoreContent
import com.dscorp.ispadmin.presentation.ui.features.migration.MigrationComposeScreen
import com.dscorp.ispadmin.presentation.ui.features.outlay.RegisterOutlayScreen
import com.dscorp.ispadmin.presentation.ui.features.payment.detail.PaymentDetailScreen
import com.dscorp.ispadmin.presentation.ui.features.payment.detail.PaymentDetailViewModel
import com.dscorp.ispadmin.presentation.ui.features.payment.history.PaymentHistoryComposeScreen
import com.dscorp.ispadmin.presentation.ui.features.payment.register.RegisterPaymentScreen
import com.dscorp.ispadmin.presentation.ui.features.plan.PlanListScreen
import com.dscorp.ispadmin.presentation.ui.features.plan.PlanListViewModel
import com.dscorp.ispadmin.presentation.ui.features.profile.ProfileScreen
import com.dscorp.ispadmin.presentation.ui.features.subscription.edit.EditSubscriptionViewModel
import com.dscorp.ispadmin.presentation.ui.features.subscription.edit.compose.EditPlanSubscriptionScreen
import com.dscorp.ispadmin.presentation.ui.features.subscription.pending.PendingSubscriptionsIntent
import com.dscorp.ispadmin.presentation.ui.features.subscription.pending.PendingSubscriptionsScreen
import com.dscorp.ispadmin.presentation.ui.features.subscription.pending.PendingSubscriptionsViewModel
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.RegisterSubscriptionFormScreen
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.SubscriptionDetailScreen
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionFinderScreen
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionFinderViewModel
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.create.CreateSupportTicketScreen
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.create.CreateSupportTicketViewModel
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose.SupportTicketListScreen
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose.SupportTicketListViewModel
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose.TicketImageDialog
import com.dscorp.ispadmin.presentation.utils.PermissionUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CancellationException
import com.dscorp.ispadmin.presentation.ui.features.home.HomeAction
import com.dscorp.ispadmin.presentation.ui.features.home.HomeIntent
import com.dscorp.ispadmin.presentation.ui.features.home.HomeViewModel
import com.dscorp.ispadmin.presentation.ui.features.home.SecretaryHomeContent
import androidx.navigation.NavBackStackEntry
import com.dscorp.ispadmin.presentation.ui.features.home.FieldAction
import com.dscorp.ispadmin.presentation.ui.features.home.FieldIntent
import com.dscorp.ispadmin.presentation.ui.features.home.SalesAgendaContent
import com.dscorp.ispadmin.presentation.ui.features.home.SalesAgendaViewModel
import com.dscorp.ispadmin.presentation.ui.features.home.TechnicianTodayContent
import com.dscorp.ispadmin.presentation.ui.features.home.TechnicianTodayViewModel
import com.dscorp.ispadmin.presentation.ui.features.home.TodayTarget
import com.dscorp.ispadmin.presentation.ui.features.payment.cash.CashAction
import com.dscorp.ispadmin.presentation.ui.features.payment.cash.CashDayContent
import com.dscorp.ispadmin.presentation.ui.features.payment.cash.CashDayViewModel
import com.dscorp.ispadmin.presentation.ui.features.payment.cash.CashIntent
import com.dscorp.ispadmin.presentation.ui.features.payment.cash.CashPeriod
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun FeatureNavGraph(
    navController: NavHostController = rememberNavController(),
    onLoggedOut: () -> Unit = {},
) {
    val viewModel: MainViewModel = koinViewModel()
    val onuRegistrationOperationUseCase: OnuRegistrationOperationUseCase = koinInject()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onEvent(MainEvent.LoadCurrentUser)
    }

    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = FeatureRoutes.FromString(currentEntry?.destination?.route)

    LaunchedEffect(uiState.currentUser?.id) {
        if (!uiState.currentUser.canRegisterSubscription() || currentRoute is Subscription.Register) return@LaunchedEffect
        val pending = try {
            onuRegistrationOperationUseCase.active()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        if (shouldResumeOnuRegistration(uiState.currentUser, pending)) {
            runNavigationOnMainThread {
                navController.navigate(Subscription.Register()) { launchSingleTop = true }
            }
        }
    }

    val userType = uiState.currentUser?.type
    if (userType == null) {
        // Wait for the session user: the start destination and the bar depend on the role.
        if (!uiState.isLoading) ProfileFallback(navController, onLoggedOut)
        return
    }

    val chrome = MainNavigation.shellChrome(userType, currentRoute)
    ExitOnRootBack(enabled = !chrome.showBack)

    MainShell(
        chrome = chrome,
        onDestinationSelected = { destination -> navController.navigateToRoot(destination.route) },
        onBack = { navController.popBackStack() },
    ) {
        NavGraphContent(
            navController,
            onLoggedOut = onLoggedOut,
            currentUser = uiState.currentUser,
            startDestination = MainNavigation.startRouteFor(userType),
        )
    }
}

/** Bottom-bar navigation: one copy of each root on the stack, keeping each tab's state. */
private fun NavHostController.navigateToRoot(route: FeatureRoutes) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun ExitOnRootBack(enabled: Boolean) {
    var showExitDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    BackHandler(enabled = enabled) { showExitDialog = true }
    if (!showExitDialog) return
    AlertDialog(
        onDismissRequest = { showExitDialog = false },
        title = { Text("¿Salir de la aplicación?") },
        text = { Text("Podrás volver a entrar sin iniciar sesión.") },
        confirmButton = {
            TextButton(onClick = {
                showExitDialog = false
                (context as? Activity)?.finish()
            }) { Text("Salir") }
        },
        dismissButton = {
            TextButton(onClick = { showExitDialog = false }) { Text("Volver") }
        },
    )
}

/** The user could not be loaded: keep the profile reachable so the session can be closed. */
@Composable
private fun ProfileFallback(navController: NavHostController, onLoggedOut: () -> Unit) {
    ProfileScreen(navController = navController, onLoggedOut = onLoggedOut)
}

@SuppressLint("MissingPermission")
@Composable
private fun NavGraphContent(
    navController: NavHostController,
    onLoggedOut: () -> Unit = {},
    currentUser: User?,
    startDestination: Any,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        // Plan
        composable<FeatureRoutes.Plan.List> {
            val viewModel: PlanListViewModel = koinViewModel()
            val state by viewModel.uiState.collectAsState()

            PlanListScreen(
                state = state,
                onPlanSelected = { /* Manejar selección si es necesario */ },
                onUpdateClick = { plan -> viewModel.updatePlan(plan) },
                onSuccessDialogDismiss = { viewModel.clearSuccess() },
                onErrorDismiss = { viewModel.clearError() },
                onShowEditDialog = { plan -> viewModel.showEditDialog(plan) },
                onHideEditDialog = { viewModel.hideEditDialog() },
                onUpdateEditedName = { name -> viewModel.updateEditedName(name) },
                onUpdateEditedPrice = { price -> viewModel.updateEditedPrice(price) },
                onUpdateEditedDownloadSpeed = { speed -> viewModel.updateEditedDownloadSpeed(speed) },
                onUpdateEditedUploadSpeed = { speed -> viewModel.updateEditedUploadSpeed(speed) },
                onUpdateSelectedType = { type -> viewModel.updateSelectedType(type) },
                onToggleFilterExpanded = { viewModel.toggleFilterExpanded() }
            )
        }

        // Dashboard
        composable<Dashboard> { backStackEntry ->
            if (currentUser?.type == User.UserType.SECRETARY) {
                SecretaryHomeRoute(backStackEntry, currentUser, navController)
            } else if (currentUser?.type == User.UserType.TECHNICIAN) {
                val viewModel: TechnicianTodayViewModel = koinViewModel()
                val state by viewModel.uiState.collectAsState()
                FieldRefresh(backStackEntry, viewModel::onIntent)
                TechnicianTodayContent(state, onAction = { navController.onFieldAction(it) }, onRetry = { viewModel.onIntent(FieldIntent.Refresh) })
            } else {
                DashBoardComposeScreen(navController = navController)
            }
        }
        composable<FeatureRoutes.More> {
            val type = currentUser?.type ?: return@composable
            MoreContent(
                items = MainNavigation.moreItemsFor(type),
                versionLabel = "IspAdmin ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                onItemSelected = { item -> navController.navigate(item.route) { launchSingleTop = true } },
            )
        }
        // Profile
        composable<Profile> {
            ProfileScreen(navController = navController, onLoggedOut = onLoggedOut)
        }
        // SUBSCRIPTION MODULE
        composable<Subscription.Register> {
            val route = it.toRoute<Subscription.Register>()
            val installationOrder = route.installationOrderId
            val customerId = route.customerId
            val canRegisterSubscription = currentUser.canRegisterSubscription()

            when {
                currentUser == null -> Unit
                canRegisterSubscription -> {
                    RegisterSubscriptionFormScreen(
                        viewModel = koinViewModel(),
                        installationOrderId = installationOrder,
                        customerId = customerId,
                        onSubscriptionRegisterSuccess = {
                            navController.popBackStack()
                        },
                        onCancelRegistration = {
                            navController.popBackStack()
                        },
                        onViewProvisioning = { subscriptionId ->
                            navController.navigate(Subscription.Provisioning(subscriptionId))
                        },
                        onNavigateToPendingSubscriptions = {
                            navController.navigate(Subscription.PendingSubscriptions)
                        },
                        onViewSubscription = { subscriptionId ->
                            navController.popBackStack()
                            navController.navigate(Subscription.Details(subscriptionId))
                        },
                    )
                }
                else -> {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }
            }
        }
        composable<Subscription.PendingSubscriptions> {
            val viewModel: PendingSubscriptionsViewModel = koinViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) {
                viewModel.onIntent(PendingSubscriptionsIntent.Load)
            }
            PendingSubscriptionsScreen(
                uiState = state,
                onIntent = viewModel::onIntent,
                uiEvents = viewModel.uiEvent
            )
        }
        composable<Subscription.Find> {
            val viewModel: SubscriptionFinderViewModel = koinViewModel()
            val context = LocalContext.current
            val locationClient = remember {
                LocationServices.getFusedLocationProviderClient(context)
            }
            val showLocationSelector = remember { mutableStateOf(false) }
            val initialLocation = remember { mutableStateOf<GeoLocation?>(null) }

            SubscriptionFinderScreen(
                navController = navController,
                viewModel = viewModel,
                onShowMapSelector = { geoLocation ->
                    initialLocation.value = geoLocation
                    showLocationSelector.value = true
                },
                onGetCurrentLocation = {
                    viewModel.setFetchingCurrentLocation(true)
                    PermissionUtils.requestLocationPermissionFromContext(
                        context = context,
                        onPermissionGranted = {
                            locationClient.lastLocation.addOnSuccessListener { location ->
                                if (location != null) {
                                    viewModel.updateCurrentLocation(
                                        LatLng(
                                            location.latitude,
                                            location.longitude
                                        )
                                    )
                                } else {
                                    viewModel.onLocationError()
                                }
                                viewModel.setFetchingCurrentLocation(false)
                            }.addOnFailureListener {
                                viewModel.onLocationError()
                                viewModel.setFetchingCurrentLocation(false)
                            }
                        },
                        onPermissionDenied = {
                            viewModel.onLocationError()
                        }
                    )
                }
            )
            if (showLocationSelector.value) {
                LocationSelectorComposeDialog(
                    initialLocation = initialLocation.value,
                    onLocationSelected = { latLng ->
                        viewModel.updateCoordinatesFromMap(latLng)
                        showLocationSelector.value = false
                    },
                    onDismiss = {
                        showLocationSelector.value = false
                    }
                )
            }
        }
        composable<Subscription.Details> { backStackEntry ->
            val route = backStackEntry.toRoute<Subscription.Details>()
            val returned by backStackEntry.savedStateHandle.getStateFlow<String?>(FICHA_RESULT, null).collectAsState()
            SubscriptionDetailScreen(
                subscriptionId = route.subscriptionId,
                openCharge = route.openCharge,
                navController = navController,
                returnedMessage = returned,
                onReturnedMessageShown = { backStackEntry.savedStateHandle[FICHA_RESULT] = null },
            )
        }
        composable<Subscription.Provisioning> { backStackEntry ->
            val subscriptionId = backStackEntry.toRoute<Subscription.Provisioning>().subscriptionId
            com.dscorp.ispadmin.presentation.ui.features.subscription.provisioning.ProvisioningScreen(subscriptionId = subscriptionId,
                onNavigate = { destination ->
                    when (destination) {
                        com.dscorp.ispadmin.presentation.ui.features.subscription.provisioning.ProvisioningDestination.BACK -> navController.popBackStack()
                        com.dscorp.ispadmin.presentation.ui.features.subscription.provisioning.ProvisioningDestination.NEW_REGISTRATION -> navController.navigate(Subscription.Register())
                    }
                })
        }
        composable<Subscription.ChangePlan> { backStackEntry ->
            val subscriptionId = backStackEntry.toRoute<Subscription.ChangePlan>().subscriptionId
            val viewModel: com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions.ChangePlanViewModel = koinViewModel()
            val state by viewModel.uiState.collectAsState()
            LaunchedEffect(subscriptionId) {
                viewModel.onIntent(com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions.ChangePlanIntent.Load(subscriptionId))
            }
            LaunchedEffect(state.done) { state.done?.let { navController.returnToFicha(it) } }
            com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions.ChangePlanContent(
                state = state,
                onIntent = viewModel::onIntent,
                onBack = { navController.popBackStack() },
                onRetry = { viewModel.onIntent(com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions.ChangePlanIntent.Load(subscriptionId)) },
            )
        }
        composable<Subscription.ChangeNap> { backStackEntry ->
            val subscriptionId = backStackEntry.toRoute<Subscription.ChangeNap>().subscriptionId
            val viewModel: com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions.ChangeNapViewModel = koinViewModel()
            val state by viewModel.uiState.collectAsState()
            LaunchedEffect(subscriptionId) {
                viewModel.onIntent(com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions.ChangeNapIntent.Load(subscriptionId))
            }
            LaunchedEffect(state.done) { state.done?.let { navController.returnToFicha(it) } }
            com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions.ChangeNapContent(
                state = state,
                onIntent = viewModel::onIntent,
                onBack = { navController.popBackStack() },
                onRetry = { viewModel.onIntent(com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions.ChangeNapIntent.Load(subscriptionId)) },
            )
        }
        composable<Subscription.UpdateLocation> { backStackEntry ->
            com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions.UpdateLocationRoute(
                subscriptionId = backStackEntry.toRoute<Subscription.UpdateLocation>().subscriptionId,
                onDone = { navController.returnToFicha(it) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Subscription.DeferCut> { backStackEntry ->
            com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions.DeferCutRoute(
                subscriptionId = backStackEntry.toRoute<Subscription.DeferCut>().subscriptionId,
                onDone = { navController.returnToFicha(it) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Subscription.Migrate> { backStackEntry ->
            val subscriptionId = backStackEntry.toRoute<Subscription.Migrate>().subscriptionId
            MigrationComposeScreen(
                navController = navController,
                subscriptionId = subscriptionId
            )
        }
        // PAYMENT MODULE
        composable<Payment.Register> { backStackEntry ->
            val paymentId = backStackEntry.toRoute<Payment.Register>().paymentId

            RegisterPaymentScreen(
                navController = navController,
                paymentId = paymentId
            )
        }
        composable<Payment.History> { backStackEntry ->
            val subscriptionId = backStackEntry.toRoute<Payment.History>().subscriptionId
            val serviceStatusStr = backStackEntry.toRoute<Payment.History>().serviceStatus
            PaymentHistoryComposeScreen(
                navController = navController,
                subscriptionId = subscriptionId,
                serviceStatus = serviceStatusStr
            )
        }
        composable<Payment.Detail> { backStackEntry ->
            val paymentId = backStackEntry.toRoute<Payment.Detail>().paymentId
            val viewModel: PaymentDetailViewModel = koinViewModel()
            LaunchedEffect(paymentId) {
                viewModel.fetchPaymentDetails(paymentId)
            }
            PaymentDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<Payment.FindPayer> { backStackEntry ->
            CashDayRoute(backStackEntry, navController)
        }
        composable<Payment.SearchPayers> {
            val viewModel: com.dscorp.ispadmin.presentation.ui.features.payment.cash.PaymentSearchViewModel = koinViewModel()
            val state by viewModel.uiState.collectAsState()
            LaunchedEffect(Unit) { viewModel.onIntent(com.dscorp.ispadmin.presentation.ui.features.payment.cash.PaymentSearchIntent.Load) }
            com.dscorp.ispadmin.presentation.ui.features.payment.cash.PaymentSearchContent(
                state = state,
                onIntent = viewModel::onIntent,
                onOpen = { navController.navigate(Payment.Receipt(it)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Payment.Receipt> { backStackEntry ->
            val receiptId = backStackEntry.toRoute<Payment.Receipt>().receiptId
            val viewModel: com.dscorp.ispadmin.presentation.ui.features.payment.cash.PaymentReceiptViewModel = koinViewModel()
            val state by viewModel.uiState.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current
            LaunchedEffect(receiptId) { viewModel.load(receiptId) }
            com.dscorp.ispadmin.presentation.ui.features.payment.cash.PaymentDetailContent(
                state = state,
                onBack = { navController.popBackStack() },
                onShare = {
                    state.detail?.let { detail ->
                        val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(android.content.Intent.EXTRA_TEXT, detail.shareText())
                        context.startActivity(android.content.Intent.createChooser(send, "Compartir comprobante"))
                    }
                },
                onOpenSubscription = { navController.navigate(Subscription.Details(it)) },
                onRetry = { viewModel.load(receiptId) },
            )
        }
        // OUTLAY MODULE
        composable<Outlay.Month> { backStackEntry ->
            val viewModel: com.dscorp.ispadmin.presentation.ui.features.outlay.OutlaysViewModel = koinViewModel()
            val state by viewModel.uiState.collectAsState()
            LaunchedEffect(Unit) { viewModel.onIntent(com.dscorp.ispadmin.presentation.ui.features.outlay.OutlaysIntent.Load) }
            // Coming back from a new outlay refreshes the month.
            val resumed = backStackEntry.lifecycle.currentStateAsState()
            LaunchedEffect(resumed.value) {
                if (resumed.value == androidx.lifecycle.Lifecycle.State.RESUMED && state.month != null) {
                    viewModel.onIntent(com.dscorp.ispadmin.presentation.ui.features.outlay.OutlaysIntent.Refresh)
                }
            }
            com.dscorp.ispadmin.presentation.ui.features.outlay.OutlaysContent(
                state,
                onFilter = { viewModel.onIntent(com.dscorp.ispadmin.presentation.ui.features.outlay.OutlaysIntent.Filter(it)) },
                onNew = { navController.navigate(Outlay.Register) },
                onRetry = { viewModel.onIntent(com.dscorp.ispadmin.presentation.ui.features.outlay.OutlaysIntent.Refresh) },
            )
        }
        composable<Outlay.Register> {

            RegisterOutlayScreen(
                onImageClick = { images, index ->
                    navController.navigate(Outlay.ImageGalleryViewer(images,index))
                }
            )
        }
        // SUPPORT TICKET MODULE
        composable<SupportTicket.List> { backStackEntry ->
            val viewModel: com.dscorp.ispadmin.presentation.ui.features.supportTicket.inbox.TicketInboxViewModel = koinViewModel()
            val state by viewModel.uiState.collectAsState()
            LaunchedEffect(Unit) { viewModel.onIntent(com.dscorp.ispadmin.presentation.ui.features.supportTicket.inbox.TicketInboxIntent.Load) }
            // Coming back from a ticket (taken, closed, rescheduled) refreshes the list and the counts.
            val resumed = backStackEntry.lifecycle.currentStateAsState()
            LaunchedEffect(resumed.value) {
                if (resumed.value == androidx.lifecycle.Lifecycle.State.RESUMED && state.tickets.isNotEmpty()) {
                    viewModel.onIntent(com.dscorp.ispadmin.presentation.ui.features.supportTicket.inbox.TicketInboxIntent.Refresh)
                }
            }
            com.dscorp.ispadmin.presentation.ui.features.supportTicket.inbox.TicketInboxContent(
                state = state,
                onIntent = viewModel::onIntent,
                onOpen = { navController.navigate(SupportTicket.Detail(it)) },
                onNew = { navController.navigate(SupportTicket.Create) },
            )
        }
        composable<SupportTicket.Detail> { backStackEntry ->
            val ticketId = backStackEntry.toRoute<SupportTicket.Detail>().ticketId
            val viewModel: com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail.TicketDetailViewModel = koinViewModel()
            val closed = backStackEntry.savedStateHandle.get<String>(com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail.CLOSED_MESSAGE_KEY)
            LaunchedEffect(closed) {
                if (closed != null) {
                    backStackEntry.savedStateHandle.remove<String>(com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail.CLOSED_MESSAGE_KEY)
                    viewModel.onIntent(com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail.TicketDetailIntent.Load(ticketId))
                }
            }
            com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail.TicketDetailScreen(ticketId, navController, viewModel)
        }
        composable<SupportTicket.Close> { backStackEntry ->
            val ticketId = backStackEntry.toRoute<SupportTicket.Close>().ticketId
            com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail.CloseTicketScreen(ticketId, navController)
        }
        composable<SupportTicket.Create> {
            val viewModel: CreateSupportTicketViewModel = koinViewModel()
            val uiState by viewModel.uiState.collectAsState()

            CreateSupportTicketScreen(
                uiState = uiState,
                onPhoneChange = viewModel::updatePhone,
                onCategoryChange = viewModel::updateCategory,
                onDescriptionChange = viewModel::updateDescription,
                onIsClientChange = viewModel::updateIsClient,
                onPlaceSelected = viewModel::updateSelectedPlace,
                onSubscriptionSelected = viewModel::updateSelectedSubscription,
                onSearchTextChange = viewModel::findSubscriptionByNames,
                onCustomerNameChange = viewModel::updateCustomerName,
                onCreateTicket = viewModel::createTicket,
                onDismissError = viewModel::resetError,
                onOpenAssign = viewModel::loadTechnicians,
                onAssigneeChange = viewModel::updateAssignee,
                categories = viewModel.categories,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable<AsyncImageViewer> { backStackEntry ->
            val imageUrl = backStackEntry.toRoute<AsyncImageViewer>().imageUrl
            TicketImageDialog(
                imageUrl = imageUrl,
                onDismiss = { navController.popBackStack() }
            )
        }

        // INSTALLATION MODULE
        composable<Installation.List> { backStackEntry ->
            if (currentUser?.type == User.UserType.SALES) {
                val agendaViewModel: SalesAgendaViewModel = koinViewModel()
                val agenda by agendaViewModel.uiState.collectAsState()
                FieldRefresh(backStackEntry, agendaViewModel::onIntent)
                SalesAgendaContent(
                    agenda,
                    onFilter = { agendaViewModel.onIntent(FieldIntent.Filter(it)) },
                    onAction = { navController.onFieldAction(it) },
                    onRetry = { agendaViewModel.onIntent(FieldIntent.Refresh) },
                )
                return@composable
            }
            val viewModel: InstallationOrderListViewModel = koinViewModel()

            InstallationOrderListScreen(
                viewModel = viewModel, onCreateOrderClicked = {
                    navController.navigate(Installation.Create) {
                        launchSingleTop = true
                    }
                },
                onNavigateToRegisterSubscription = {
                    viewModel.onEvent(InstallationOrderListEvent.ResetSelectedOrder)
                    navController.navigate(Subscription.Register(it.id)) {
                        launchSingleTop = true
                    }
                })
        }

        composable<Installation.Create> {
            val viewModel: CreateInstallationOrderViewModel = koinViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.onEvent(InstallationOrderEvent.OnLoadPlaces)
            }
            CreateInstallationOrderScreen(
                uiState = uiState,
                onCreateOrderClicked = {
                    viewModel.onEvent(InstallationOrderEvent.OnCreateOrder)
                },
                onFirstNameChange = {
                    viewModel.onEvent(InstallationOrderEvent.OnFirstNameChange(it))
                },
                onLastNameChange = {
                    viewModel.onEvent(InstallationOrderEvent.OnLastNameChange(it))
                },
                onAddressChange = {
                    viewModel.onEvent(InstallationOrderEvent.OnAddressChange(it))
                },
                onPhoneChange = {
                    viewModel.onEvent(InstallationOrderEvent.OnPhoneChange(it))
                },
                onDniChange = {
                    viewModel.onEvent(InstallationOrderEvent.OnDniChange(it))
                },
                onPlaceChange = {
                    viewModel.onEvent(InstallationOrderEvent.OnPlaceChange(it))
                },
                onErrorDismissed = {
                    viewModel.onEvent(InstallationOrderEvent.OnDismissError)
                },
                onSuccessDismissed = {
                    viewModel.onEvent(InstallationOrderEvent.OnDismissSuccess)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        dialog<Outlay.ImageGalleryViewer>(dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
            val data = it.toRoute<Outlay.ImageGalleryViewer>()
            HorizontalImageCarousel(
                imageUrls = data.imageUrls,
                onClose = {
                    navController.popBackStack()
                },
                selectedImageIndex = data.selectedIndex
            )
        }
//
//        composable<Installation.Pending> {
//            PendingInstallationOrdersScreen(
//                navController = navController,
//                onSuccess = {
//                    navController.popBackStack()
//                }
//            )
//        }
//
//        composable<Installation.Assigned> {
//            AssignedInstallationOrdersScreen(
//                navController = navController,
//                onSuccess = {
//                    navController.popBackStack()
//                }
//            )
//        }
//
//        composable<Installation.InProgress> {
//            SellerInProgressOrdersScreen(
//                navController = navController,
//                onSuccess = {
//                    navController.popBackStack()
//                }
//            )
//        }
//
//        composable<Installation.Closed> {
//            SellerClosedOrdersScreen(
//                navController = navController,
//                onSuccess = {
//                    navController.popBackStack()
//                }
//            )
//        }
    }
}

@Composable
private fun SecretaryHomeRoute(backStackEntry: NavBackStackEntry, user: User, navController: NavHostController) {
    val viewModel: HomeViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.onIntent(HomeIntent.Load) }
    // Coming back from a charge or a ticket refreshes today's numbers.
    val resumed = backStackEntry.lifecycle.currentStateAsState()
    LaunchedEffect(resumed.value) {
        if (resumed.value == androidx.lifecycle.Lifecycle.State.RESUMED && state.home != null) viewModel.onIntent(HomeIntent.Refresh)
    }
    SecretaryHomeContent(
        state = state,
        userInitials = listOfNotNull(user.name, user.lastName).mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }.joinToString(""),
        onRetry = { viewModel.onIntent(HomeIntent.Refresh) },
        onAction = { action ->
            when (action) {
                HomeAction.Search, HomeAction.CutCandidates, HomeAction.Owing, HomeAction.NewCharge ->
                    navController.navigateToRoot(Subscription.Find)
                HomeAction.Tickets -> navController.navigateToRoot(SupportTicket.List)
                HomeAction.Profile -> navController.navigate(Profile)
                is HomeAction.OpenSubscription -> navController.navigate(Subscription.Details(action.id))
                is HomeAction.Charge -> navController.navigate(Subscription.Details(action.id, openCharge = true))
                HomeAction.NewTicket -> navController.navigate(SupportTicket.Create)
                HomeAction.NewOutlay -> navController.navigate(FeatureRoutes.Outlay.Register)
                HomeAction.NewInstallationOrder -> navController.navigate(FeatureRoutes.Installation.Create)
            }
        },
    )
}

/** Loads once and refreshes when coming back (a closed ticket, a registration). */
@Composable
private fun FieldRefresh(backStackEntry: NavBackStackEntry, onIntent: (FieldIntent) -> Unit) {
    LaunchedEffect(Unit) { onIntent(FieldIntent.Load) }
    var seen by remember { mutableStateOf(false) }
    val resumed = backStackEntry.lifecycle.currentStateAsState()
    LaunchedEffect(resumed.value) {
        if (resumed.value == androidx.lifecycle.Lifecycle.State.RESUMED) {
            if (seen) onIntent(FieldIntent.Refresh)
            seen = true
        }
    }
}

private fun NavHostController.onFieldAction(action: FieldAction) {
    when (action) {
        is FieldAction.Open -> when (val target = action.target) {
            is TodayTarget.Ticket -> navigate(SupportTicket.Detail(target.id))
            is TodayTarget.Order -> navigate(Subscription.Register(installationOrderId = target.id))
        }
        is FieldAction.Register -> when (val target = action.target) {
            is TodayTarget.Ticket -> navigate(Subscription.Register())
            is TodayTarget.Order -> navigate(Subscription.Register(installationOrderId = target.id))
        }
        is FieldAction.Directions -> context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=" + android.net.Uri.encode(action.query)))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        is FieldAction.Filtered -> Unit
        FieldAction.Profile -> navigate(Profile)
        FieldAction.PendingUploads -> navigate(Subscription.PendingSubscriptions)
        FieldAction.NewOrder -> navigate(FeatureRoutes.Installation.Create)
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CashDayRoute(backStackEntry: NavBackStackEntry, navController: NavHostController) {
    val viewModel: CashDayViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    var pickingDate by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.onIntent(CashIntent.Load) }
    // Coming back from a charge refreshes the day.
    val resumed = backStackEntry.lifecycle.currentStateAsState()
    LaunchedEffect(resumed.value) {
        if (resumed.value == androidx.lifecycle.Lifecycle.State.RESUMED && state.day != null) viewModel.onIntent(CashIntent.Refresh)
    }
    CashDayContent(state) { action ->
        when (action) {
            CashAction.Search -> navController.navigate(Payment.SearchPayers)
            CashAction.NewCharge -> navController.navigateToRoot(Subscription.Find)
            CashAction.Retry -> viewModel.onIntent(CashIntent.Refresh)
            is CashAction.OpenPayment -> navController.navigate(Payment.Receipt(action.receiptId))
            is CashAction.Period -> when (action.period) {
                CashPeriod.TODAY -> viewModel.onIntent(CashIntent.SelectDate(java.time.LocalDate.now(LIMA)))
                CashPeriod.YESTERDAY -> viewModel.onIntent(CashIntent.SelectDate(java.time.LocalDate.now(LIMA).minusDays(1)))
                CashPeriod.PICK -> pickingDate = true
            }
        }
    }
    if (pickingDate) {
        val picker = androidx.compose.material3.rememberDatePickerState(
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= System.currentTimeMillis()
            },
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    picker.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                        viewModel.onIntent(CashIntent.SelectDate(date))
                    }
                    pickingDate = false
                }) { Text("Ver") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { pickingDate = false }) { Text("Cancelar") } },
        ) { androidx.compose.material3.DatePicker(state = picker) }
    }
}

/** Key the ficha action screens use to hand their result message back to the ficha. */
private const val FICHA_RESULT = "ficha_result"

/** Leaves an action screen and tells the ficha to reload and show [message]. */
private fun NavHostController.returnToFicha(message: String) {
    previousBackStackEntry?.savedStateHandle?.set(FICHA_RESULT, message)
    popBackStack()
}

private val LIMA: java.time.ZoneId = java.time.ZoneId.of("America/Lima")
