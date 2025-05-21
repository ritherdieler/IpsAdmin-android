package com.dscorp.ispadmin.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.dscorp.ispadmin.domain.model.GeoLocation
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.AsyncImageViewer
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Dashboard
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Payment
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Profile
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Subscription
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.SupportTicket
import com.dscorp.ispadmin.presentation.ui.features.dashboard.DashBoardComposeScreen
import com.dscorp.ispadmin.presentation.ui.features.locationMapView.LocationSelectorComposeDialog
import com.dscorp.ispadmin.presentation.ui.features.main.MenuDrawerContent
import com.dscorp.ispadmin.presentation.ui.features.migration.MigrationComposeScreen
import com.dscorp.ispadmin.presentation.ui.features.payment.detail.PaymentDetailScreen
import com.dscorp.ispadmin.presentation.ui.features.payment.detail.PaymentDetailViewModel
import com.dscorp.ispadmin.presentation.ui.features.payment.history.PaymentHistoryComposeScreen
import com.dscorp.ispadmin.presentation.ui.features.payment.payerFinder.FindPayerComposeScreen
import com.dscorp.ispadmin.presentation.ui.features.payment.register.RegisterPaymentScreen
import com.dscorp.ispadmin.presentation.ui.features.profile.ProfileScreen
import com.dscorp.ispadmin.presentation.ui.features.subscription.edit.EditSubscriptionViewModel
import com.dscorp.ispadmin.presentation.ui.features.subscription.edit.compose.EditPlanSubscriptionScreen
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.RegisterSubscriptionFormScreen
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.compose.SubscriptionDetailForm
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionFinderScreen
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionFinderViewModel
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.create.CreateSupportTicketScreen
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.create.CreateSupportTicketViewModel
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose.SupportTicketListScreen
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose.SupportTicketListViewModel
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose.TicketImageDialog
import com.dscorp.ispadmin.presentation.utils.PermissionUtils
import com.example.data2.data.response.AssistanceTicketStatus
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureNavGraph(navController: NavHostController = rememberNavController(), onLoggedOut : () -> Unit = {}) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("IPS Admin") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                MenuDrawerContent(
                    navController = navController,
                    onItemClicked = {
                        scope.launch {
                            drawerState.close()
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                // Contenido del grafo de navegación
                NavGraphContent(navController, onLoggedOut = onLoggedOut)
            }
        }
    }
}

@Composable
private fun NavGraphContent(navController: NavHostController,onLoggedOut : () -> Unit = {}) {
    NavHost(
        navController = navController,
        startDestination = Profile,
    ) {
        // Dashboard
        composable<Dashboard> {
            DashBoardComposeScreen(navController = navController)
        }
        // Profile
        composable<Profile> {
            ProfileScreen(navController = navController, onLoggedOut = onLoggedOut)
        }
        // SUBSCRIPTION MODULE
        composable<Subscription.Register> {
            RegisterSubscriptionFormScreen(
                viewModel = koinViewModel(),
                installationOrderId = null,
                onSubscriptionRegisterSuccess = {
                    navController.popBackStack()
                }
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
            val subscriptionId = backStackEntry.toRoute<Subscription.Details>().subscriptionId
            SubscriptionDetailForm(
                subscriptionId = subscriptionId
            )
        }
        composable<Subscription.ChangePlan> { backStackEntry ->
            val subscriptionId = backStackEntry.toRoute<Subscription.ChangePlan>().subscriptionId
            val viewModel = koinViewModel<EditSubscriptionViewModel>()
            LaunchedEffect(subscriptionId) {
                viewModel.getFormData(subscriptionId)
            }
            val state by viewModel.uiState.collectAsState()
            EditPlanSubscriptionScreen(
                state = state,
                onPlanSelected = { plan -> viewModel.updateSelectedPlan(plan) },
                onEditClick = {
                    viewModel.editSubscription(subscriptionId)
                },
                onSuccessDialogDismiss = {
                    viewModel.clearSuccess()
                    navController.popBackStack()
                },
                onErrorDismiss = {
                    viewModel.clearError()
                }
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
        composable<Payment.FindPayer> {
            FindPayerComposeScreen(navController = navController)
        }
        // SUPPORT TICKET MODULE
        composable<SupportTicket.List> {
            val viewModel: SupportTicketListViewModel = koinViewModel()
            val uiState by viewModel.uiState.collectAsState()
            SupportTicketListScreen(
                uiState = uiState,
                onTabChange = viewModel::onTabChange,
                onTakeTicket = viewModel::takeTicket,
                onCloseUnattendedTicket = viewModel::closeUnattendedTicket,
                onCloseTicket = viewModel::closeTicket,
                onRefresh = viewModel::refreshData,
                onDismissError = viewModel::dismissError,
                onTicketCardClick = { ticket ->
                    if (ticket.status == AssistanceTicketStatus.CLOSED && ticket.sheetImageUrl.isNotEmpty()) {
                        navController.navigate("ticket/image/${ticket.sheetImageUrl}")
                    }
                }
            )
        }
        // Aquí puedes agregar más rutas de soporte
         composable<SupportTicket.Create> {
             val viewModel: CreateSupportTicketViewModel = koinViewModel()
             val uiState by viewModel.uiState.collectAsState()

              if(uiState.isTicketCreated){
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
              }

             CreateSupportTicketScreen(
                 uiState = uiState,
                 onPhoneChange = viewModel::updatePhone,
                 onCategoryChange =  viewModel ::updateCategory,
                 onDescriptionChange =  viewModel::updateDescription,
                 onIsClientChange =  viewModel::updateIsClient,
                 onPlaceSelected =  viewModel::updateSelectedPlace,
                 onSubscriptionSelected =  viewModel::updateSelectedSubscription,
                 onSearchTextChange = viewModel::findSubscriptionByNames,
                 onCustomerNameChange = viewModel::updateCustomerName,
                 onCreateTicket = viewModel::createTicket,
                 onDismissError = viewModel::resetError,
                 categories = viewModel.categories
             )
         }

        // RUTA PARA VER UNA IMAGEN DE TICKET
        composable<AsyncImageViewer> { backStackEntry ->
            val imageUrl = backStackEntry.toRoute<AsyncImageViewer>().imageUrl
            TicketImageDialog(
                imageUrl = imageUrl,
                onDismiss = { navController.popBackStack() }
            )
        }
        // ...puedes seguir agregando features aquí
    }
}
