package com.dscorp.ispadmin.presentation.ui.features.installationorder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.domain.model.InstallationOrder
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerClosedOrdersScreen(
    uiState: ClosedOrdersUiState,
    onOrderSelected: (InstallationOrder) -> Unit = {},
    onErrorDismissed: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Mostrar errores en Snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
            onErrorDismissed()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.see_closed_orders)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refrescar",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.closedOrders.isEmpty()) {
                Text(
                    text = "No hay órdenes cerradas",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.Center),
                    textAlign = TextAlign.Center
                )
            } else {
                OrdersList(
                    orders = uiState.closedOrders,
                    onOrderSelected = onOrderSelected,
                    contentPadding = PaddingValues(16.dp)
                )
            }
        }
    }

    // Efecto para refrescar los datos al componer

}

@Composable
private fun OrdersList(
    orders: List<InstallationOrder>,
    onOrderSelected: (InstallationOrder) -> Unit,
    contentPadding: PaddingValues
) {
    LazyColumn(
        contentPadding = contentPadding
    ) {
        items(orders) { order ->
            ClosedOrderCard(order = order, onClick = { onOrderSelected(order) })
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClosedOrderCard(order: InstallationOrder, onClick: () -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "${order.customerFirstName} ${order.customerLastName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "DNI: ${order.customerDni}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Dirección: ${order.customerAddress}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Teléfono: ${order.customerPhone}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Ubicación: ${order.place?.name ?: "No especificada"}",
                style = MaterialTheme.typography.bodyMedium
            )

            order.scheduledDate?.let { date ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Fecha programada: ${formatter.format(date)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            order.technician?.let { tech ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Técnico asignado: ${tech.name} ${tech.lastName}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            order.subscription?.let { subs ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Suscripción: ${subs.clientName} (ID: ${subs.id})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            order.cancellationReason?.let { reason ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Motivo de cancelación: $reason",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
} 