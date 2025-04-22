package com.dscorp.ispadmin.presentation.ui.features.installationorder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.domain.model.InstallationOrder
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.Loader
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyButton
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyCustomDialog
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyDateTimePickerField
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyOutLinedDropDown
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingInstallationOrdersScreen(
    uiState: PendingOrdersUiState,
    onOrderSelected: (InstallationOrder) -> Unit,
    onTechnicianSelected: (User) -> Unit,
    onScheduledDateSelected: (LocalDateTime) -> Unit,
    onAssignTechnician: () -> Unit,
    onCloseDialog: () -> Unit,
    onErrorDismissed: () -> Unit,
    onSuccessDismissed: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onErrorDismissed()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            onSuccessDismissed()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Órdenes de Instalación Pendientes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            if (uiState.isLoading) {
                Loader()
            } else if (uiState.pendingOrders.isEmpty()) {
                EmptyOrdersMessage(onRefresh)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    item { 
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Seleccione una orden para asignar a un técnico",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    
                    items(uiState.pendingOrders) { order ->
                        OrderItem(
                            order = order,
                            onOrderClick = { onOrderSelected(order) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
            
            if (uiState.showAssignDialog) {
                AssignTechnicianDialog(
                    order = uiState.selectedOrder,
                    technicians = uiState.technicians,
                    selectedTechnician = uiState.selectedTechnician,
                    onTechnicianSelected = onTechnicianSelected,
                    onScheduledDateSelected = onScheduledDateSelected,
                    onAssign = onAssignTechnician,
                    onDismiss = onCloseDialog
                )
            }
        }
    }
}

@Composable
fun OrderItem(
    order: InstallationOrder,
    onOrderClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOrderClick),
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
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = order.customerAddress,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tel: ${order.customerPhone}",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Text(
                    text = order.place?.name ?: "Sin lugar asignado",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Divider(modifier = Modifier.padding(vertical = 2.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vendedor:",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Text(
                    text = order.seller?.let { "${it.name} ${it.lastName}" } ?: "No asignado",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun EmptyOrdersMessage(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No hay órdenes pendientes",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        MyButton(
            text = "Refrescar",
            onClick = onRefresh
        )
    }
}

@Composable
fun AssignTechnicianDialog(
    order: InstallationOrder?,
    technicians: List<User>,
    selectedTechnician: User?,
    onTechnicianSelected: (User) -> Unit,
    onScheduledDateSelected: (LocalDateTime) -> Unit,
    onAssign: () -> Unit,
    onDismiss: () -> Unit
) {
    if (order == null) return
    
    var dateTimeText by rememberSaveable { mutableStateOf("") }
    val isFormValid = selectedTechnician != null && dateTimeText.isNotEmpty()
    
    MyCustomDialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Asignar Técnico",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "Cliente: ${order.customerFirstName} ${order.customerLastName}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            
            Text(
                text = "Dirección: ${order.customerAddress}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            MyOutLinedDropDown(
                items = technicians,
                selected = selectedTechnician,
                label = "Técnico",
                onItemSelected = onTechnicianSelected,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            MyDateTimePickerField(
                label = "Fecha y Hora Programada",
                dateTime = dateTimeText,
                onDateTimeSelected = { 
                    dateTimeText = it
                    // Convertir a LocalDate para el ViewModel
                    try {
                        // Extraer solo la fecha (ignorar la hora para la conversión a LocalDate)
                        val date = LocalDateTime.parse(it, DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))
                        onScheduledDateSelected(date)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Manejar error de análisis de fecha
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                MyButton(
                    text = "Cancelar",
                    onClick = onDismiss,
                    modifier = Modifier.padding(end = 8.dp)
                )
                
                MyButton(
                    text = "Asignar",
                    enabled = isFormValid,
                    onClick = onAssign
                )
            }
        }
    }
} 