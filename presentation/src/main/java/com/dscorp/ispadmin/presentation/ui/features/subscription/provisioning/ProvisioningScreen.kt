package com.dscorp.ispadmin.presentation.ui.features.subscription.provisioning

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dscorp.ispadmin.presentation.theme.MyTheme
import org.koin.androidx.compose.koinViewModel

enum class ProvisioningDestination { BACK, NEW_REGISTRATION }

@Composable
fun ProvisioningScreen(subscriptionId: Int, onNavigate: (ProvisioningDestination) -> Unit) {
    val viewModel: ProvisioningViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(subscriptionId) { viewModel.onIntent(ProvisioningIntent.Load(subscriptionId)) }
    DisposableEffect(viewModel) { onDispose { viewModel.onIntent(ProvisioningIntent.Stop) } }
    MyTheme { ProvisioningContent(state = state, onIntent = viewModel::onIntent, onNavigate = onNavigate) }
}

@Composable
fun ProvisioningContent(
    state: ProvisioningUiState,
    onIntent: (ProvisioningIntent) -> Unit,
    onNavigate: (ProvisioningDestination) -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                TextButton(modifier = Modifier.testTag("provisioning_back"), onClick = { onNavigate(ProvisioningDestination.BACK) }) { Text("Volver") }
                Text("Registro de la ONU", style = MaterialTheme.typography.headlineSmall)
                if (state.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            item {
                val progress = state.progress
                if (progress != null) {
                    Text(provisioningLabel(progress.operation.state))
                    Text("Referencia: ${progress.operation.id}", style = MaterialTheme.typography.bodySmall)
                    ProvisioningActions(state = state, onIntent = onIntent, onNavigate = onNavigate)
                } else if (!state.isLoading && state.error == null) {
                    Text("Esta suscripción no tiene un registro v2.")
                }
                state.subscriptionId?.let { id ->
                    TextButton(modifier = Modifier.testTag("provisioning_refresh"), enabled = !state.isSubmitting,
                        onClick = { onIntent(ProvisioningIntent.Load(id)) }) { Text("Actualizar estado") }
                }
            }
            items(items = state.progress?.operation?.checkpoints.orEmpty(), key = { it.stage }) { step ->
                Text("${provisioningLabel(step.stage)}: ${provisioningLabel(step.state)} · Intento ${step.attempts}")
                step.failure?.let { Text("${it.message}\n${it.code}", color = MaterialTheme.colorScheme.error) }
            }
        }
        if (state.confirmCancel) CancelConfirmation(onIntent)
    }
}

@Composable
private fun ProvisioningActions(state: ProvisioningUiState, onIntent: (ProvisioningIntent) -> Unit,
    onNavigate: (ProvisioningDestination) -> Unit) {
    val progress = state.progress ?: return
    val enabled = !state.isSubmitting && state.error == null
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (progress.canRetry) Button(modifier = Modifier.testTag("provisioning_retry"), enabled = enabled,
            onClick = { onIntent(ProvisioningIntent.Retry) }) { Text("Reintentar alta") }
        if (progress.canCancel || progress.canRetryCancellation) OutlinedButton(
            modifier = Modifier.testTag("provisioning_cancel"), enabled = enabled,
            onClick = { onIntent(ProvisioningIntent.AskCancel) }) {
            Text(if (progress.canRetryCancellation) "Reintentar cancelación" else "Cancelar registro")
        }
        if (progress.canStartAgain) Button(modifier = Modifier.testTag("provisioning_new"), enabled = enabled,
            onClick = { onNavigate(ProvisioningDestination.NEW_REGISTRATION) }) { Text("Nuevo registro") }
    }
}

@Composable
private fun CancelConfirmation(onIntent: (ProvisioningIntent) -> Unit) {
    AlertDialog(onDismissRequest = { onIntent(ProvisioningIntent.DismissCancel) },
        title = { Text("Cancelar registro") },
        text = { Text("Se revertirán los cambios de esta alta. Podrás iniciar otro registro cuando se confirme la limpieza.") },
        confirmButton = { TextButton(modifier = Modifier.testTag("provisioning_confirm_cancel"),
            onClick = { onIntent(ProvisioningIntent.ConfirmCancel) }) { Text("Confirmar cancelación") } },
        dismissButton = { TextButton(modifier = Modifier.testTag("provisioning_dismiss_cancel"),
            onClick = { onIntent(ProvisioningIntent.DismissCancel) }) { Text("Volver") } })
}

private fun provisioningLabel(value: String): String = when (value) {
    "VALIDATE" -> "Validación"
    "MIKROTIK" -> "Acceso del cliente"
    "OLT" -> "Autorización de la ONU"
    "OMCI" -> "WAN de gestión"
    "ACS_CONTACT" -> "Conexión con ACS"
    "INTERNET" -> "Internet PPPoE"
    "WIFI" -> "WiFi"
    "WAN_CLEANUP" -> "Limpieza de WAN"
    "VERIFY" -> "Verificación final"
    "PENDING" -> "Pendiente"
    "RUNNING" -> "En curso"
    "WAITING" -> "Esperando respuesta"
    "SUCCEEDED" -> "Completado"
    "FAILED" -> "Requiere atención"
    "CANCEL_REQUESTED", "CANCELLING" -> "Revirtiendo el registro"
    "CANCEL_FAILED" -> "Limpieza pendiente"
    "CANCELLED" -> "Registro cancelado"
    "COMPENSATED" -> "Revertido"
    else -> value
}

@Preview
@Composable
private fun ProvisioningPreview() {
    MyTheme { ProvisioningContent(state = ProvisioningUiState(), onIntent = {}, onNavigate = {}) }
}
