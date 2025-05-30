package com.dscorp.ispadmin.presentation.ui.features.payment.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.navigation.NavRoutes.FeatureRoutes.Payment
import com.dscorp.ispadmin.presentation.theme.MyTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Versión Compose pura de la pantalla de historial de pagos, adaptada para el nuevo sistema de navegación.
 *
 * @param navController Controlador de navegación de Compose
 * @param subscriptionId ID de la suscripción para mostrar su historial de pagos
 * @param serviceStatus Estado del servicio de la suscripción
 */
@Composable
fun PaymentHistoryComposeScreen(
    navController: NavHostController,
    subscriptionId: Int,
    serviceStatus: String
) {
    // Obtiene el ViewModel con los parámetros necesarios
    val viewModel: PaymentHistoryViewModel = koinViewModel { parametersOf(subscriptionId) }
    val currentUser = viewModel.repository.getUserSession()

    // Convertir el string a ServiceStatus
    val serviceStatusEnum = try {
        ServiceStatus.valueOf(serviceStatus)
    } catch (e: Exception) {
        ServiceStatus.ACTIVE
    }

    // Establecer el ID de suscripción y el estado del servicio
    LaunchedEffect(subscriptionId, serviceStatusEnum) {
        viewModel.subscriptionId = subscriptionId
        viewModel.serviceStatus = serviceStatusEnum
        viewModel.getLastPayments(PaymentHistoryViewModel.LAST_PAYMENTS_ROW_LIMIT)
    }

    // Obtener el estado de la UI
    val state by viewModel.state.collectAsState()

    val canManagePayments = currentUser?.type in listOf(
        User.UserType.ADMIN,
        User.UserType.ACCOUNTANT,
        User.UserType.SECRETARY
    )

    // Renderizar la pantalla
    MyTheme {
        PaymentScreenContent(
            state = state,
            subscriptionId = viewModel.subscriptionId,
            onPaymentItemClicked = { payment ->
                if (!canManagePayments) return@PaymentScreenContent
                
                if (!payment.paid) {
                    // Navegar a la pantalla de registro de pago
                    // Necesitamos proporcionar el ID del pago para la ruta
                    payment.id?.let { paymentId ->
                        navController.navigate(
                            Payment.Register(
                                paymentId
                            ))
                    }
                } else {
                    // Navegar a la pantalla de detalle de pago
                    payment.id?.toString()?.let { paymentId ->
                        navController.navigate(Payment.Detail(paymentId))
                    }
                }
            },
            onUpdateReactivationNotes = { viewModel.updateReactivationNotes(it) },
            onReactivateService = { viewModel.reactivateService() },
            onTogglePendingPaymentsFilter = { isChecked ->
                if (isChecked) {
                    viewModel.showOnlyPendingPayments()
                } else {
                    viewModel.showAllPayments()
                }
            },
            showReactivationSection = canManagePayments
        )
    }
} 