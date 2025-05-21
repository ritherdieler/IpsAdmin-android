package com.dscorp.ispadmin.presentation.ui.features.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import com.dscorp.ispadmin.presentation.theme.MyTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Pantalla del Dashboard en Compose, siguiendo el patrón de UI establecido.
 * 
 * @param navController Controlador de navegación
 */
@Composable
fun DashBoardComposeScreen(
    navController: NavHostController
) {
    // Obtenemos las dependencias
    val viewModel: DashBoardViewModel = koinViewModel()
    
    // Observamos el estado
    val uiState by viewModel.state.collectAsState()
    
    // Renderizamos la UI con el tema de la aplicación
    MyTheme {
        DashboardScreen(
            uiState = uiState,
            onRefresh = {
                viewModel.getDashBoardData()
            }
        )
    }
} 