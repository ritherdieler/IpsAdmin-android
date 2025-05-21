package com.dscorp.ispadmin.navigation


import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.navigation.NavRoutes.AuthRoutes.Login
import com.dscorp.ispadmin.navigation.NavRoutes.AuthRoutes.Register
import com.dscorp.ispadmin.presentation.ui.features.login.compose.LoginScreen

@Composable
fun AuthNavGraph(
    navController: NavHostController = rememberNavController(),
    onLoginSuccess  : (user: User) -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = Login,
    ) {
        composable<Login> {
            LoginScreen(
                onCreatedAccountClicked = {
                    navController.navigate(Register)
                },
                onLoginSuccess = { user ->
                    onLoginSuccess(user)
                }
            )
        }
    }
}
