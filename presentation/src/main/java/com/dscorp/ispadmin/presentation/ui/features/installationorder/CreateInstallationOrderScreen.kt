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
import com.dscorp.ispadmin.domain.model.InstallationOrder
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.Loader
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyButton
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyCustomDialog
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.MyOutlinedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInstallationOrderScreen(
    uiState: InstallationOrderUiState,
    onCreateOrder: (InstallationOrder) -> Unit,
    onErrorDismissed: () -> Unit,
    onSuccessDismissed: () -> Unit,
    onOrderCreationHandled: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    // Validación de campos
    val isFormValid = firstName.isNotBlank() && lastName.isNotBlank() && 
                     address.isNotBlank() && phone.isNotBlank()

    // Manejo de errores
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onErrorDismissed()
        }
    }

    // Manejo de éxito
    LaunchedEffect(uiState.successMessage, uiState.orderCreated) {
        if (uiState.successMessage != null && uiState.orderCreated != null) {
            snackbarHostState.showSnackbar(uiState.successMessage)
            onSuccessDismissed()
            onOrderCreationHandled()
            onNavigateBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Nueva Orden de Instalación") },
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
                    Text(
                        text = "Información del Cliente",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    MyOutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = "Nombre",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    MyOutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = "Apellido",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    MyOutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = "Dirección",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    MyOutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = "Teléfono",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    MyButton(
                        text = "Crear Orden de Instalación",
                        enabled = isFormValid,
                        onClick = {
                            onCreateOrder(
                                InstallationOrder(
                                    customerFirstName = firstName,
                                    customerLastName = lastName,
                                    customerAddress = address,
                                    customerPhone = phone
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Diálogo de confirmación de éxito
            if (uiState.orderCreated != null) {
                MyCustomDialog(
                    onDismissRequest = {
                        onSuccessDismissed()
                        onOrderCreationHandled()
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
                            text = "Orden de instalación creada correctamente.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        MyButton(
                            text = "Aceptar",
                            onClick = {
                                onSuccessDismissed()
                                onOrderCreationHandled()
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