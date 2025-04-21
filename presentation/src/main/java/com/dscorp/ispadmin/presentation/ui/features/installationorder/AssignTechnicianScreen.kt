package com.dscorp.ispadmin.presentation.ui.features.installationorder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.Loader
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyButton
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyCustomDialog
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyDatePickerField
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyOutLinedDropDown
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignTechnicianScreen(
    uiState: InstallationOrderUiState,
    orderId: Int,
    onAssignTechnician: (technicianId: Int, assignedById: Int, scheduledDate: LocalDate) -> Unit,
    onErrorDismissed: () -> Unit,
    onSuccessDismissed: () -> Unit,
    onOrderUpdateHandled: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    
    // Estado local
    var selectedTechnician by remember { mutableStateOf<User?>(null) }
    var selectedDate by remember { mutableStateOf("") }
    
    // Validación
    val isFormValid = selectedTechnician != null && selectedDate.isNotBlank()
    
    // Convertir la fecha seleccionada a LocalDate
    val parsedDate = try {
        if (selectedDate.isNotBlank()) {
            LocalDate.parse(selectedDate.split(" ")[0], dateFormatter)
        } else null
    } catch (e: Exception) {
        null
    }
    
    // Manejo de errores
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onErrorDismissed()
        }
    }
    
    // Manejo de éxito
    LaunchedEffect(uiState.successMessage, uiState.orderUpdated) {
        if (uiState.successMessage != null && uiState.orderUpdated != null) {
            snackbarHostState.showSnackbar(uiState.successMessage)
            onSuccessDismissed()
            onOrderUpdateHandled()
            onNavigateBack()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Asignar Técnico") },
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
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Orden de Instalación #$orderId",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Text(
                        text = "Asignación de Técnico",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                    
                    MyOutLinedDropDown(
                        items = uiState.technicians,
                        selected = selectedTechnician,
                        label = "Seleccionar Técnico",
                        onItemSelected = { selectedTechnician = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    MyDatePickerField(
                        label = "Fecha Programada",
                        date = selectedDate,
                        onDateSelected = { selectedDate = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    MyButton(
                        text = "Asignar Técnico",
                        enabled = isFormValid,
                        onClick = {
                            selectedTechnician?.id?.let { techId ->
                                parsedDate?.let { date ->
                                    // Asumimos que obtenemos el ID del usuario actual de alguna forma
                                    // Por simplicidad, usamos un valor fijo 1 como el ID del asignador
                                    onAssignTechnician(techId, 1, date)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Diálogo de confirmación de éxito
            if (uiState.orderUpdated != null) {
                MyCustomDialog(
                    onDismissRequest = {
                        onSuccessDismissed()
                        onOrderUpdateHandled()
                        onNavigateBack()
                    }
                ) { 
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "¡Éxito!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Técnico asignado correctamente a la orden de instalación.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        MyButton(
                            text = "Aceptar",
                            onClick = {
                                onSuccessDismissed()
                                onOrderUpdateHandled()
                                onNavigateBack()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
} 