package com.dscorp.ispadmin.navigation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.navigation.NavRoutes.AuthRoutes.Login
import com.dscorp.ispadmin.navigation.NavRoutes.AuthRoutes.Register
import com.dscorp.ispadmin.presentation.ui.features.login.LoginScreen
import com.dscorp.ispadmin.presentation.ui.features.login.LoginState
import com.dscorp.ispadmin.presentation.ui.features.login.LoginViewModel
import com.dscorp.ispadmin.presentation.ui.features.registration.RegisterScreen
import org.koin.androidx.compose.koinViewModel
import com.dscorp.ispadmin.presentation.ui.features.login.FaceLoginScreen

@Composable
fun AuthNavGraph(
    navController: NavHostController = rememberNavController(),
    onLoginSuccess: (user: User) -> Unit = {},
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
                },
                //agrego edwin
                onFaceLoginClicked = {
                    navController.navigate(NavRoutes.AuthRoutes.FaceLogin)
                }
            )
        }

        composable<Register> {
            RegisterScreen(onNavigateBack = {
                navController.navigateUp()
            }, onRegisterSuccess = {
                navController.navigate(Login) {
                    popUpTo(Login) { inclusive = true }
                }
            })
        }
        //agrego edwin
        composable<NavRoutes.AuthRoutes.FaceLogin> {
            val viewModel: LoginViewModel = koinViewModel()
            val loginState by viewModel.loginRequestFlow.collectAsState()
            var faceRetryTrigger by remember { mutableStateOf(0) }

            FaceLoginScreen(
                onFacePhotoCaptured = { photo ->
                    viewModel.doFaceLogin(photo)
                },
                onCancel ={
                    navController.navigateUp()
                },
                retryTrigger = faceRetryTrigger
            )

            // Si el backend reconoce el rostro, usa la misma salida del login normal.
            if (loginState is LoginState.LoginSuccess) {
                onLoginSuccess((loginState as LoginState.LoginSuccess).data)
            }

            // Si falla el intento facial, reinicia la camara sin mostrar cuadros intermedios.
            if (loginState is LoginState.Error) {
                LaunchedEffect(loginState) {
                    viewModel.resetLoginState()
                    faceRetryTrigger++
                }
            }

            // Si el usuario existe pero no esta verificado, conserva la regla del login normal.
            if (loginState is LoginState.UnverifiedAccount) {
                AlertDialog(
                    onDismissRequest = {
                        viewModel.resetLoginState()
                    },
                    title = {
                        Text("Cuenta no verificada")
                    },
                    text = {
                        Text("Su cuenta aun no ha sido verificada por un administrador.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.resetLoginState()
                            }
                        ) {
                            Text("Entendido")
                        }
                    }
                )
            }

        }
    }
}
