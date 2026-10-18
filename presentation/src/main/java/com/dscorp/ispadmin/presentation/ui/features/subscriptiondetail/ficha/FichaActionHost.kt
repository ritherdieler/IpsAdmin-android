package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes
import com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose.CancelSubscriptionState
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions.CancelSubscription
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions.CancelSubscriptionSheetContent
import java.time.LocalDate
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
            CancelSubscriptionState.Success -> {
                onClosed()
                onChanged("Suscripción cancelada")
            }
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
            MoreAction.EDIT_HOLDER -> navController.navigate(FeatureRoutes.Subscription.EditHolder(subscription.id)).also { onClosed() }
            MoreAction.DEFER_CUT -> navController.navigate(FeatureRoutes.Subscription.DeferCut(subscription.id)).also { onClosed() }
            MoreAction.CANCEL, MoreAction.REACTIVATE -> viewModel.setSelectedSubscription(resume)
            null -> Unit
        }
    }

    when (action) {
        MoreAction.CANCEL -> {
            val busy = state.cancelSubscriptionState == CancelSubscriptionState.Loading
            ModalBottomSheet(onDismissRequest = { if (!busy) onClosed() }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
                CancelSubscriptionSheetContent(
                    model = remember(subscription) { CancelSubscription.from(subscription, LocalDate.now(FICHA_ZONE)) },
                    busy = busy,
                    error = if (state.cancelSubscriptionState == CancelSubscriptionState.Error) "No se pudo cancelar la suscripción. Vuelve a intentar." else null,
                    // Stays open until the Core answers; success closes it from the effect above.
                    onConfirm = { viewModel.cancelSubscription(subscription.id) },
                    onBack = onClosed,
                )
            }
        }
        MoreAction.REACTIVATE -> ReactivateServiceDialog(
            onDismiss = { if (state.reactivateServiceState != ReactivateServiceState.Loading) onClosed() },
            onConfirm = { viewModel.reactivateService(subscription.id) },
            isLoading = state.reactivateServiceState == ReactivateServiceState.Loading,
            notes = state.reactivationNotes,
            onNotesChange = viewModel::updateReactivationNotes,
        )
        else -> Unit
    }
}

