package com.dscorp.ispadmin.presentation.ui.features.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import com.dscorp.ispadmin.presentation.theme.MyTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Versión adaptada de la pantalla de perfil para el nuevo sistema de navegación en Compose.
 * Esta misma pantalla se puede usar dentro de un Fragment o directamente en la navegación Compose.
 *
 * @param navController Controlador de navegación (opcional, solo para navegación Compose)
 */
@Composable
fun ProfileScreen(
    navController: NavHostController? = null,
    onLoggedOut: () -> Unit = {},
) {
    // Obtenemos las dependencias
    val viewModel: ProfileViewModel = koinViewModel()

    // Observamos el estado
    val uiState by viewModel.uiState.collectAsState()

    // Manejamos la navegación cuando el usuario cierra sesión (solo en navegación Compose)
    if (navController != null) {
        LaunchedEffect(uiState.isLoggedOut) {
            if (uiState.isLoggedOut) {
                onLoggedOut()
            }
        }
    }

    // Renderizamos la UI con el tema de la aplicación
    MyTheme {
        ProfileContent(
            uiState = uiState,
            onLogout = {
                viewModel.logOut()
            },
            onClearError = {
                viewModel.clearError()
            }
        )
    }
}