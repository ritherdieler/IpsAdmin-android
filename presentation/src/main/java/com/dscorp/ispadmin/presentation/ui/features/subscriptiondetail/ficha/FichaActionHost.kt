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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.CancelSubscriptionDialog
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.CancelSubscriptionState
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.CustomerDataForm
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.DeferCutDialog
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.ReactivateServiceDialog
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.ReactivateServiceState
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.SubscriptionFinderViewModel

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
    val state by viewModel.uiState.collectAsState()
    val resume = remember(subscription) { subscription.toDomain() }

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
            MoreAction.CHANGE_NAP -> navController.navigate(FeatureRoutes.Subscription.ChangeNap(subscription.id)).also { onClosed() }
            MoreAction.UPDATE_LOCATION -> navController.navigate(FeatureRoutes.Subscription.UpdateLocation(subscription.id)).also { onClosed() }
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

