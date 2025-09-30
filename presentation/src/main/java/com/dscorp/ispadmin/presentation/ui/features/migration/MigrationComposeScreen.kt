package com.dscorp.ispadmin.presentation.ui.features.migration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.components.Loader
import org.koin.androidx.compose.koinViewModel

@Composable
fun MigrationComposeScreen(
    navController: NavController,
    subscriptionId: Int,
    viewModel: MigrationViewModel = koinViewModel()
) {
    // Obtener los datos del formulario cuando se navega a esta pantalla
    LaunchedEffect(subscriptionId) {
        viewModel.getMigrationFormData(subscriptionId)
    }
    
    // Observar el estado del ViewModel
    val state by viewModel.uiState.collectAsState()
    
    MyTheme {
        when (val currentState = state) {
            MigrationUiState.Empty -> {
                // Estado inicial, no mostrar nada
            }
            is MigrationUiState.Error -> {
                ErrorDialog(
                    error = currentState.error.message ?: "Error desconocido",
                    onDismissRequest = {
                        viewModel.getMigrationFormData(subscriptionId)
                    }
                )
            }
            is MigrationUiState.FormDataReady -> {
                MigrationForm(
                    onus = currentState.unconfirmedOnus,
                    plans = currentState.plans,
                    subscription = currentState.subscription,
                    onMigrationRequest = { request ->
                        request.apply { this.subscriptionId = subscriptionId }
                        viewModel.doMigration(request)
                    }
                )
            }
            MigrationUiState.Loading -> {
                Loader()
            }
            is MigrationUiState.Success -> {
                // Mostrar mensaje de éxito y navegar de vuelta
                LaunchedEffect(Unit) {
                    // Delay para mostrar una notificación de éxito
                    kotlinx.coroutines.delay(1500)
                    navController.popBackStack()
                }
                SuccessMessage()
            }
        }
    }
}

@Composable
private fun SuccessMessage() {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { },
        title = { androidx.compose.material3.Text("Éxito") },
        text = { androidx.compose.material3.Text("Migración realizada con éxito") },
        confirmButton = { }
    )
} 