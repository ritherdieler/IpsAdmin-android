package com.dscorp.ispadmin.presentation.ui.features.supportTicket.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.dscorp.ispadmin.data.media.prepareFacadePhotoFile
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes
import com.dscorp.ispadmin.presentation.ui.components.rememberPhotoTaker
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.IspSnackbarHost
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose.ContactIntentHelper
import org.koin.androidx.compose.koinViewModel

@Composable
fun TicketDetailScreen(ticketId: Int, navController: NavController, viewModel: TicketDetailViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(ticketId) { viewModel.onIntent(TicketDetailIntent.Load(ticketId)) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event -> if (event is TicketDetailEvent.Message) snackbar.showSnackbar(event.text) }
    }
    val openSubscription = { state.subscriptionId?.let { navController.navigate(FeatureRoutes.Subscription.Details(it)) } }
    Box(Modifier.fillMaxSize()) {
        TicketDetailContent(
            state = state,
            onIntent = viewModel::onIntent,
            onBack = { navController.popBackStack() },
            onCharge = { openSubscription() },
            onOpenSubscription = { openSubscription() },
            onCall = { state.detail?.phone?.let { ContactIntentHelper.openDialer(context, it) } },
            onWhatsApp = { state.detail?.phone?.let { ContactIntentHelper.openWhatsApp(context, it) } },
            onClose = { navController.navigate(FeatureRoutes.SupportTicket.Close(ticketId)) },
            onOpenTicket = { navController.navigate(FeatureRoutes.SupportTicket.Detail(it)) },
        )
        IspSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun CloseTicketScreen(ticketId: Int, navController: NavController, viewModel: CloseTicketViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val (takePhoto, _) = rememberPhotoTaker(
        context = context,
        onPhotoTaken = { uri -> viewModel.onIntent(CloseTicketIntent.PhotoTaken(prepareFacadePhotoFile(context, uri))) },
    )
    LaunchedEffect(ticketId) { viewModel.onIntent(CloseTicketIntent.Load(ticketId)) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is CloseTicketEvent.Closed) {
                navController.previousBackStackEntry?.savedStateHandle?.set(CLOSED_MESSAGE_KEY, event.message)
                navController.popBackStack()
            }
        }
    }
    CloseTicketContent(state, viewModel::onIntent, onTakePhoto = { takePhoto() }, onBack = { navController.popBackStack() })
}

const val CLOSED_MESSAGE_KEY = "ticket_closed_message"
