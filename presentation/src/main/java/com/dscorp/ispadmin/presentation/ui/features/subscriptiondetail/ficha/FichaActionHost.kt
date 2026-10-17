package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes
import com.dscorp.ispadmin.presentation.ui.features.locationMapView.LocationSelectorComposeDialog
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.CancelSubscriptionDialog
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.CancelSubscriptionState
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.ChangeNapBoxDialog
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.CustomerDataForm
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.DeferCutDialog
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.LocationUpdateDialog
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.ReactivateServiceDialog
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.ReactivateServiceState
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionFinderViewModel
import com.dscorp.ispadmin.presentation.utils.PermissionUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng

/**
 * Runs one «Más acciones» choice with the dialogs and logic the subscription search already uses,
 * so the ficha does not duplicate cancel, defer cut, NAP, location or holder editing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FichaActionHost(
    action: MoreAction?,
    subscription: SubscriptionResponse,
    viewModel: SubscriptionFinderViewModel,
    navController: NavController,
    onUpdatePhoto: () -> Unit,
    onChanged: (message: String?) -> Unit,
    onClosed: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val resume = remember(subscription) { subscription.toDomain() }
    var showMap by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.customerSaveMessages.collect { message ->
            onChanged(message)
            onClosed()
        }
    }
    LaunchedEffect(state.deferCutMessage) {
        state.deferCutMessage?.let {
            viewModel.clearDeferCutMessage()
            onChanged(it)
        }
    }
    LaunchedEffect(state.cancelSubscriptionState) {
        when (state.cancelSubscriptionState) {
            CancelSubscriptionState.Success -> onChanged("Suscripción cancelada")
            CancelSubscriptionState.Error -> onChanged("No se pudo cancelar la suscripción")
            else -> Unit
        }
    }
    LaunchedEffect(state.reactivateServiceState) {
        when (val reactivate = state.reactivateServiceState) {
            ReactivateServiceState.Success -> {
                viewModel.clearReactivateServiceState()
                onClosed()
                onChanged("Servicio reactivado")
            }
            is ReactivateServiceState.Error -> {
                viewModel.clearReactivateServiceState()
                onChanged(reactivate.error ?: "No se pudo reactivar el servicio")
            }
            else -> Unit
        }
    }

    LaunchedEffect(action) {
        when (action) {
            MoreAction.CHANGE_PLAN -> navController.navigate(FeatureRoutes.Subscription.ChangePlan(subscription.id)).also { onClosed() }
            MoreAction.MIGRATE_TO_FIBER -> navController.navigate(FeatureRoutes.Subscription.Migrate(subscription.id)).also { onClosed() }
            MoreAction.NEW_SERVICE -> {
                subscription.customerId?.let { navController.navigate(FeatureRoutes.Subscription.Register(customerId = it)) }
                onClosed()
            }
            MoreAction.UPDATE_PHOTO -> {
                onUpdatePhoto()
                onClosed()
            }
            MoreAction.CHANGE_NAP -> viewModel.setSelectedSubscription(resume)
            MoreAction.UPDATE_LOCATION -> {
                viewModel.setSelectedSubscription(resume)
                viewModel.toggleLocationUpdateDialog(true)
            }
            MoreAction.EDIT_HOLDER -> {
                viewModel.setSelectedSubscription(resume)
                viewModel.getPlaces()
                viewModel.initCustomerFormData(resume)
            }
            MoreAction.CANCEL, MoreAction.REACTIVATE, MoreAction.DEFER_CUT -> viewModel.setSelectedSubscription(resume)
            null -> Unit
        }
    }

    when (action) {
        MoreAction.CHANGE_NAP -> ChangeNapBoxDialog(
            viewModel = viewModel,
            selectedSubscription = resume,
            napBoxesState = state.napBoxesState,
            context = context,
            onDismiss = {
                viewModel.resetNapBoxFlow()
                onClosed()
                onChanged(null)
            },
        )
        MoreAction.UPDATE_LOCATION -> if (state.showLocationUpdateDialog) {
            val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
            LocationUpdateDialog(
                selectedSubscription = state.selectedSubscription,
                saveState = state.saveSubscriptionState,
                latitude = state.editableLatitude,
                longitude = state.editableLongitude,
                onShowMap = { showMap = true },
                onGetCurrentLocationClick = {
                    viewModel.setFetchingCurrentLocation(true)
                    PermissionUtils.requestLocationPermissionFromContext(
                        context = context,
                        onPermissionGranted = {
                            locationClient.lastLocation
                                .addOnSuccessListener { location ->
                                    if (location != null) viewModel.updateCurrentLocation(LatLng(location.latitude, location.longitude))
                                    else viewModel.onLocationError()
                                    viewModel.setFetchingCurrentLocation(false)
                                }
                                .addOnFailureListener {
                                    viewModel.onLocationError()
                                    viewModel.setFetchingCurrentLocation(false)
                                }
                        },
                        onPermissionDenied = { viewModel.onLocationError() },
                    )
                },
                isFetchingCurrentLocation = state.isFetchingCurrentLocation,
                onUpdateLocation = { viewModel.updateSubscriptionLocation() },
                onDismiss = {
                    viewModel.toggleLocationUpdateDialog(false)
                    onClosed()
                    onChanged(null)
                },
            )
            if (showMap) {
                LocationSelectorComposeDialog(
                    initialLocation = subscription.location,
                    onLocationSelected = { latLng ->
                        viewModel.updateCoordinatesFromMap(latLng)
                        showMap = false
                    },
                    onDismiss = { showMap = false },
                )
            }
        }
        MoreAction.DEFER_CUT -> DeferCutDialog(
            onDismiss = onClosed,
            onConfirm = { untilDate, reason ->
                viewModel.deferCut(subscription.id, untilDate, reason)
                onClosed()
            },
        )
        MoreAction.CANCEL -> CancelSubscriptionDialog(
            onDismiss = onClosed,
            onConfirm = {
                viewModel.cancelSubscription(subscription.id)
                onClosed()
            },
        )
        MoreAction.REACTIVATE -> ReactivateServiceDialog(
            onDismiss = { if (state.reactivateServiceState != ReactivateServiceState.Loading) onClosed() },
            onConfirm = { viewModel.reactivateService(subscription.id) },
            isLoading = state.reactivateServiceState == ReactivateServiceState.Loading,
            notes = state.reactivationNotes,
            onNotesChange = viewModel::updateReactivationNotes,
        )
        MoreAction.EDIT_HOLDER -> state.customerFormData?.takeIf { it.subscriptionId == subscription.id }?.let { form ->
            ModalBottomSheet(onDismissRequest = onClosed, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    CustomerDataForm(
                        formData = form,
                        placesState = state.placesState,
                        saveState = state.saveSubscriptionState,
                        onFieldChange = viewModel::updateCustomerFormField,
                        onPlaceSelected = viewModel::onPlaceSelected,
                        onUpdatePlace = viewModel::updateCustomerPlaceId,
                        onSaveClick = viewModel::saveCustomerData,
                    )
                }
            }
        }
        else -> Unit
    }
}

