package com.dscorp.ispadmin.presentation.ui.features.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.presentation.util.BiometricAuthManager
import androidx.fragment.app.FragmentActivity
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    onCreatedAccountClicked: () -> Unit = {},
    onLoginSuccess: (User) -> Unit = {},
    onFaceLoginClicked: () -> Unit = {}, // agrego edwin
    viewModel: LoginViewModel = koinViewModel()
) {
    val loginState by viewModel.loginRequestFlow.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    // Controla el dialogo que aparece cuando el telefono soporta huella,
    // pero el usuario aun no registro ninguna en los ajustes de Android.
    var showBiometricEnrollmentDialog by remember { mutableStateOf(false) }

    // Show login UI regardless of version check
    Login(
        loginState = loginState,
        onLoginClicked = { loginData ->
            viewModel.doLogin(loginData)
        },
        onCreateAccountClicked = {
            onCreatedAccountClicked()
        },
        onFaceLoginClicked = {
            onFaceLoginClicked()
        },
        onBiometricLoginClicked = {
            // Obtiene la Activity necesaria para mostrar el prompt nativo de AndroidX Biometric.
            val currentActivity = activity
            if (currentActivity == null) {
                viewModel.showBiometricError("No se pudo abrir la autenticacion biometrica.")
                return@Login
            }

            // Antes de abrir la huella, valida hardware, disponibilidad y registro biometrico.
            val availabilityError = BiometricAuthManager.getAvailabilityMessage(context)
            if (availabilityError != null) {
                if (BiometricAuthManager.needsBiometricEnrollment(context)) {
                    showBiometricEnrollmentDialog = true
                } else {
                    viewModel.showBiometricError(availabilityError)
                }
                return@Login
            }

            // Android valida la huella; si es correcta, la app reutiliza la sesion local guardada.
            BiometricAuthManager.authenticate(
                activity = currentActivity,
                onSuccess = {
                    viewModel.loginWithSavedSession()
                },
                onError = { message ->
                    viewModel.showBiometricError(message)
                }
            )
        }
    )

    if (showBiometricEnrollmentDialog) {
        // Si no hay huella registrada, permite abrir los ajustes del telefono para registrarla.
        AlertDialog(
            onDismissRequest = {
                showBiometricEnrollmentDialog = false
            },
            title = {
                Text("Registrar huella digital")
            },
            text = {
                Text("Para iniciar sesion con huella, primero registra una huella en los ajustes del telefono.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBiometricEnrollmentDialog = false
                        activity?.let { BiometricAuthManager.openBiometricEnrollment(it) }
                    }
                ) {
                    Text("Ir a ajustes")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showBiometricEnrollmentDialog = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Handle successful login
    when (loginState) {
        is LoginState.LoginSuccess -> {
            onLoginSuccess((loginState as LoginState.LoginSuccess).data)
        }
        is LoginState.UnverifiedAccount -> {
            AlertDialog(
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                onDismissRequest = { },
                title = { 
                    Text(
                        "Cuenta no verificada",
                        color = MaterialTheme.colorScheme.onSurface
                    ) 
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Su cuenta aún no ha sido verificada por un administrador.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "IMPORTANTE: Debe esperar a que un administrador verifique su cuenta antes de poder ingresar al sistema.",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetLoginState()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Entendido")
                    }
                }
            )
        }
        is LoginState.Error -> {
            AlertDialog(
                onDismissRequest = {
                    viewModel.resetLoginState()
                },
                title = { 
                    Text(
                        "Error de inicio de sesión",
                        color = MaterialTheme.colorScheme.onSurface
                    ) 
                },
                text = {
                    Text(
                        (loginState as LoginState.Error).message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetLoginState()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Aceptar")
                    }
                }
            )
        }
        else -> {}
    }
}
