package com.dscorp.ispadmin.presentation.ui.features.subscription.pending

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.components.Loader
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.SuccessSnackbar
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.HubBadge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** a-reg-10: registrations queued earlier without signal; they are sent or discarded by hand. */
@Composable
fun PendingSubscriptionsScreen(
    uiState: PendingSubscriptionsUiState,
    onIntent: (PendingSubscriptionsIntent) -> Unit,
    modifier: Modifier = Modifier,
    uiEvents: Flow<PendingSubscriptionsUiEvent> = emptyFlow()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var successMessage by remember { mutableStateOf("") }
    var discarding by remember { mutableStateOf<PendingSubscriptionListItem?>(null) }

    LaunchedEffect(uiEvents) {
        uiEvents.collect { event ->
            when (event) {
                is PendingSubscriptionsUiEvent.ShowSuccessSnackbar -> successMessage = event.message
            }
        }
    }

    MyTheme {
        val colors = IspTheme.colors
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState, modifier = Modifier.testTag("pending_subscriptions_snackbar")) },
            bottomBar = {
                Column(Modifier.fillMaxWidth().background(colors.card).navigationBarsPadding()) {
                    HorizontalDivider(color = colors.cardBorder)
                    Button(
                        onClick = { onIntent(PendingSubscriptionsIntent.Sync) },
                        enabled = !uiState.isSyncing && uiState.items.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 52.dp).testTag("btn_sync_pending_subscriptions"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = colors.onHero, strokeWidth = 2.dp)
                        } else {
                            Text("Enviar ahora", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                }
            },
        ) { padding ->
            SuccessSnackbar(snackbarHostState = snackbarHostState, successMessage = successMessage, onDismiss = { successMessage = "" })
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize().padding(padding)) { Loader() }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (uiState.items.isNotEmpty()) item("banner") {
                        Row(
                            Modifier.fillMaxWidth().background(colors.warningContainer, RoundedCornerShape(16.dp)).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = colors.warning, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Envíalos con “Enviar ahora” cuando tengas señal", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    uiState.errorMessage?.let { error ->
                        item("error") { Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                    }
                    item("title") {
                        Text("Por enviar · ${uiState.items.size}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                    if (uiState.items.isEmpty()) {
                        item("empty") { Text("No hay registros por enviar.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    items(uiState.items, key = { it.localId }) { item ->
                        PendingSubscriptionCard(item, onDiscard = { discarding = item })
                    }
                    if (uiState.items.isNotEmpty()) item("note") {
                        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = 4.dp)) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Ya no se guardan registros sin señal: aquí solo quedan los que se guardaron antes.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        discarding?.let { item ->
            AlertDialog(
                onDismissRequest = { discarding = null },
                title = { Text("¿Descartar el registro?") },
                text = { Text("Se borra el registro de ${item.clientName} guardado en este teléfono. No se envía al servidor.") },
                confirmButton = {
                    TextButton(onClick = { onIntent(PendingSubscriptionsIntent.Discard(item.localId)); discarding = null }) {
                        Text("Descartar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = { TextButton(onClick = { discarding = null }) { Text("Volver") } },
            )
        }
    }
}

@Composable
private fun PendingSubscriptionCard(item: PendingSubscriptionListItem, onDiscard: () -> Unit) {
    val colors = IspTheme.colors
    val savedAt = SimpleDateFormat("HH:mm", Locale.US).format(Date(item.createdAt))
    Column(
        Modifier.fillMaxWidth().background(colors.card, RoundedCornerShape(16.dp)).padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(item.clientName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(item.detail.ifBlank { "DNI ${item.dni}" }, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HubBadge(StatusBadge(item.statusLabel, if (item.failed) BadgeTone.ERROR else BadgeTone.WARNING))
            Text("guardado $savedAt", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item.error?.let { Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.error) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDiscard) { Text("Descartar", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error) }
        }
    }
}
