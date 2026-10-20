package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.domain.model.CustomerServiceSummary
import com.dscorp.ispadmin.domain.model.SubscriptionResponse

/** Proposal A ficha (a-sus-04…07): navy header, quick actions on «Resumen», tabs below. */
@Composable
fun FichaScreen(
    header: FichaHeader,
    subscription: SubscriptionResponse,
    otherServices: List<CustomerServiceSummary>,
    tab: FichaTab,
    ticketsCount: Int?,
    onTab: (FichaTab) -> Unit,
    onBack: () -> Unit,
    onMore: (() -> Unit)?,
    onCharge: () -> Unit,
    onAction: (FichaAction) -> Unit,
    onUpdatePhoto: () -> Unit,
    onOpenService: (Int) -> Unit,
    tabContent: @Composable (FichaTab) -> Unit,
) {
    var confirmRestore by remember { mutableStateOf(false) }
    val pending = header.pending
    val cut = subscription.offersRestore()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        FichaHeaderBand(header, compact = tab != FichaTab.RESUMEN, onBack = onBack, onMore = onMore, onCharge = onCharge)
        if (tab == FichaTab.RESUMEN) {
            FichaQuickActions(quickActions(subscription)) { action ->
                if (action == FichaAction.RESTORE && canRestore(cut, pending)) confirmRestore = true
                else onAction(action)
            }
        }
        FichaTabs(tab, onTab, ticketsCount)
        when (tab) {
            FichaTab.RESUMEN -> FichaSummary(
                restoreHint = restoreHint(cut, pending),
                service = serviceRows(subscription),
                holder = holderRows(subscription),
                network = networkRows(subscription),
                facadePhotoUrl = subscription.facadePhotoUrl,
                otherServices = otherServices,
                onSeeNetwork = { onTab(FichaTab.RED) },
                onUpdatePhoto = onUpdatePhoto,
                onOpenService = onOpenService,
            )
            else -> Column(Modifier.padding(vertical = 4.dp)) { tabContent(tab) }
        }
    }
    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("¿Restablecer el servicio?") },
            text = { Text("El abonado recupera internet en 1–2 minutos.", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    onAction(FichaAction.RESTORE)
                }) { Text("Restablecer") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Volver") } },
        )
    }
}
