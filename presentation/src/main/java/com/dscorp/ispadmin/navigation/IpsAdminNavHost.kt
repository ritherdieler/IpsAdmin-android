package com.dscorp.ispadmin.navigation

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dscorp.ispadmin.data.datasource.remote.auth.SessionEventBus
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.navigation.NavRoutes.Features
import com.dscorp.ispadmin.navigation.NavRoutes.Splash
import com.dscorp.ispadmin.observability.ObservabilityClient
import com.dscorp.ispadmin.presentation.ui.features.splash.compose.SplashScreen
import org.koin.compose.koinInject

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
    observabilityClient: ObservabilityClient = koinInject(),
    sessionEventBus: SessionEventBus = koinInject(),
    onSessionStarted: (User) -> Unit = {},
    onSessionEnded: () -> Unit = {},
) {
    val context = LocalContext.current

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            val route = entry.destination.route ?: entry.destination.displayName
            observabilityClient.addBreadcrumb(category = "navigation", message = route)
        }
    }

    LaunchedEffect(navController) {
        sessionEventBus.sessionExpired.collect {
            onSessionEnded()
            navController.navigate(NavRoutes.Auth) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

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
                    (context as? Activity)?.finish()
                }
            )
        }

        composable<NavRoutes.Auth> {
            AuthNavGraph(onLoginSuccess = { user ->
                onSessionStarted(user)
                navController.navigate(Features) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            })
        }

        composable<Features> {
            FeatureNavGraph( onLoggedOut = {
                onSessionEnded()
                navController.navigate(NavRoutes.Auth) {
                    popUpTo(Features) { inclusive = true }
                }
            })
        }
    }

}

