package com.dscorp.ispadmin.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dscorp.ispadmin.navigation.NavRoutes.Features
import com.dscorp.ispadmin.navigation.NavRoutes.Splash
import com.dscorp.ispadmin.presentation.ui.features.splash.compose.SplashScreen

/**
 * Componente principal de navegación para la aplicación IpsAdmin.
 * Define todas las rutas y sus pantallas correspondientes.
 *
 * @param navController Controlador de navegación
 * @param modifier Modificador opcional para personalizar el NavHost
 * @param startDestination Destino inicial (por defecto: Splash)
 */
@SuppressLint("MissingPermission")
@Composable
fun IpsAdminNavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Splash,
        modifier = modifier
    ) {
        // Pantallas de autenticación
        composable<Splash> {
            SplashScreen(
                onNavigateToMain = { user ->
                    navController.navigate(Features) {
                        popUpTo(Splash) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(NavRoutes.Auth) {
                        popUpTo(Splash) { inclusive = true }
                    }
                },
                onFinishApp = {
                    // Manejar cierre de app
                }
            )
        }

        composable<NavRoutes.Auth> {
            AuthNavGraph(onLoginSuccess = {
                navController.navigate(Features) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            })
        }

        composable<Features> {
            FeatureNavGraph( onLoggedOut = {
                navController.navigate(NavRoutes.Auth) {
                    popUpTo(Features) { inclusive = true }
                }
            })
        }
    }

}

