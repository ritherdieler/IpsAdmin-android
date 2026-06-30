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
import com.dscorp.ispadmin.presentation.ui.features.login.FaceEnrollmentScreen
import com.dscorp.ispadmin.presentation.ui.features.login.FaceEnrollmentCredentialsScreen
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

            // Ofrece registrar un rostro solo cuando el backend no reconoce el intento facial.
//            if (loginState is LoginState.FaceEnrollmentOffer) {
//                AlertDialog(
//                    onDismissRequest = { },
//                    title = {
//                        Text("Rostro no reconocido")
//                    },
//                    text = {
//                        Text("No encontramos un rostro registrado. Deseas registrar tu rostro ahora?")
//                    },
//                    confirmButton = {
//                        Button(
//                            onClick = {
//                                viewModel.resetLoginState()
//                                navController.navigate(
//                                    NavRoutes.AuthRoutes.FaceEnrollmentCredentials
//                                ) {
//                                    popUpTo(NavRoutes.AuthRoutes.FaceLogin) {
//                                        inclusive = true
//                                    }
//                                }
//                            }
//                        ) {
//                            Text("Registrar rostro")
//                        }
//                    },
//                    dismissButton = {
//                        Button(
//                            onClick = {
//                                viewModel.resetLoginState()
//                                navController.navigateUp()
//                            }
//                        ) {
//                            Text("Cancelar")
//                        }
//                    }
//                )
//            }

            // Flujo de registro facial deshabilitado temporalmente.
// Antes, cuando el rostro no era reconocido, se ofrecía registrar un nuevo rostro.
// Se conserva el código comentado abajo para poder reactivarlo si negocio lo pide.
            if (loginState is LoginState.FaceEnrollmentOffer) {
                AlertDialog(
                    onDismissRequest = { },
                    title = {
                        Text("Rostro no reconocido")
                    },
                    text = {
                        Text("No pudimos reconocer tu rostro. Puedes intentar nuevamente o cancelar.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.resetLoginState()
                                faceRetryTrigger++
                            }
                        ) {
                            Text("Intentar nuevamente")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                viewModel.resetLoginState()
                                navController.navigateUp()
                            }
                        ) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // Si falla el intento facial, reinicia la camara sin mostrar cuadros intermedios.
            // Muestra un mensaje claro si el backend no reconoce el rostro.
            // Al aceptar, reinicia la camara para permitir un nuevo intento.
            if (loginState is LoginState.Error) {
                AlertDialog(
                    onDismissRequest = { },
                    title = {
                        Text("Reconocimiento facial")
                    },
                    text = {
                        Text((loginState as LoginState.Error).message)
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.resetLoginState()
                                faceRetryTrigger++
                            }
                        ) {
                            Text("Intentar nuevamente")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                viewModel.resetLoginState()
                                navController.navigateUp()
                            }
                        ) {
                            Text("Cancelar")
                        }
                    }
                )
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

        // Solicita credenciales antes de abrir la camara de registro facial.
        composable<NavRoutes.AuthRoutes.FaceEnrollmentCredentials> {
            val viewModel: LoginViewModel = koinViewModel()
            val loginState by viewModel.loginRequestFlow.collectAsState()

            FaceEnrollmentCredentialsScreen(
                isLoading = loginState is LoginState.Loading,
                onContinue = { loginForm ->
                    // Reutiliza el login normal para validar credenciales y guardar la sesion activa.
                    viewModel.doLogin(loginForm)
                },
                onCancel = {
                    navController.navigate(Login) {
                        popUpTo(Login) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )

            // Despues de validar las credenciales, abre la camara para registrar el rostro.
            if (loginState is LoginState.LoginSuccess) {
                LaunchedEffect(loginState) {
                    viewModel.resetLoginState()
                    navController.navigate(NavRoutes.AuthRoutes.FaceEnrollment) {
                        popUpTo(NavRoutes.AuthRoutes.FaceEnrollmentCredentials) {
                            inclusive = true
                        }
                    }
                }
            }

            if (loginState is LoginState.Error) {
                AlertDialog(
                    onDismissRequest = { },
                    title = {
                        Text("No se pudo validar la cuenta")
                    },
                    text = {
                        Text((loginState as LoginState.Error).message)
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.resetLoginState()
                            }
                        ) {
                            Text("Intentar nuevamente")
                        }
                    }
                )
            }

            if (loginState is LoginState.UnverifiedAccount) {
                AlertDialog(
                    onDismissRequest = { },
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

        // Registra el rostro de un usuario que ya inicio sesion con contrasena.
        // Reutiliza la camara nativa y envia la foto al backend para guardar face_data.
        composable<NavRoutes.AuthRoutes.FaceEnrollment> {
            val viewModel: LoginViewModel = koinViewModel()
            val loginState by viewModel.loginRequestFlow.collectAsState()
            var faceRetryTrigger by remember { mutableStateOf(0) }

            FaceEnrollmentScreen(
                onFacePhotoCaptured = { photo ->
                    // La sesion activa permite obtener el userId sin volver a pedir credenciales.
                    viewModel.registerFaceForLoggedUser(photo)
                },
                onCancel = {
                    // Regresa al login para permitir continuar sin registrar el rostro.
                    navController.navigateUp()
                },
                retryTrigger = faceRetryTrigger
            )

            // Muestra una confirmacion cuando el backend registra correctamente el rostro.
            // El usuario entra al sistema al presionar Continuar.
            if (loginState is LoginState.LoginSuccess) {
                val registeredUser = (loginState as LoginState.LoginSuccess).data

                AlertDialog(
                    onDismissRequest = { },
                    title = {
                        Text("Registro facial completado")
                    },
                    text = {
                        Text("Tu rostro fue registrado correctamente.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.resetLoginState()
                                onLoginSuccess(registeredUser)
                            }
                        ) {
                            Text("Continuar")
                        }
                    }
                )
            }

            // Si el backend rechaza la foto o existe un problema de conexion,
            // muestra el motivo y permite intentar nuevamente.
            if (loginState is LoginState.Error) {
                AlertDialog(
                    onDismissRequest = { },
                    title = {
                        Text("No se pudo registrar el rostro")
                    },
                    text = {
                        Text((loginState as LoginState.Error).message)
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.resetLoginState()
                                faceRetryTrigger++
                            }
                        ) {
                            Text("Intentar nuevamente")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                viewModel.resetLoginState()
                                navController.navigateUp()
                            }
                        ) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }
}
