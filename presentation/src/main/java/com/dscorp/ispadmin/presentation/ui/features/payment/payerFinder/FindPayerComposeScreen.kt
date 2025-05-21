package com.dscorp.ispadmin.presentation.ui.features.payment.payerFinder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.features.payment.payerFinder.compose.PayerFinderScreen
import com.dscorp.ispadmin.presentation.ui.features.payment.register.RegisterPaymentViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Pantalla de búsqueda de pagadores en Compose, siguiendo el patrón de UI establecido.
 * 
 * @param navController Controlador de navegación
 */
@Composable
fun FindPayerComposeScreen(
    navController: NavHostController
) {
    // Obtenemos las dependencias
    val viewModel: RegisterPaymentViewModel = koinViewModel()
    
    // Observamos el estado
    val uiState by viewModel.state.collectAsState()
    
    // Renderizamos la UI con el tema de la aplicación
    MyTheme {
        PayerFinderScreen(
            paymentViewModel = viewModel
        )
    }
} 