package com.dscorp.ispadmin.presentation.ui.features.payment.register

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dscorp.ispadmin.domain.model.Payment
import com.google.gson.Gson

private const val REGISTER_PAYMENT_ROUTE = "register_payment"
private const val PAYMENT_ARG = "payment"

fun NavController.navigateToRegisterPayment(payment: Payment) {
    val paymentJson = Gson().toJson(payment)
    this.navigate("$REGISTER_PAYMENT_ROUTE/$paymentJson")
}

fun NavGraphBuilder.registerPaymentScreen(
    onNavigateBack: () -> Unit
) {
    composable(
        route = "$REGISTER_PAYMENT_ROUTE/{$PAYMENT_ARG}",
        arguments = listOf(
            navArgument(PAYMENT_ARG) {
                type = NavType.StringType
            }
        )
    ) { backStackEntry ->
        val paymentJson = backStackEntry.arguments?.getString(PAYMENT_ARG) ?: ""
        val payment = Gson().fromJson(paymentJson, Payment::class.java)
        
        RegisterPaymentScreen(
            payment = payment,
            onNavigateBack = onNavigateBack
        )
    }
} 