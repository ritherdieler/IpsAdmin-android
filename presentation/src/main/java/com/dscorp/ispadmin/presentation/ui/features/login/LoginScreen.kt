package com.dscorp.ispadmin.presentation.ui.features.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.dscorp.ispadmin.domain.model.User
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    onCreatedAccountClicked: () -> Unit = {},
    onLoginSuccess: (User) -> Unit = {},
    viewModel: LoginViewModel = koinViewModel()
) {
    val loginState by viewModel.loginRequestFlow.collectAsState()

    // Show login UI regardless of version check
    Login(
        loginState = loginState,
        onLoginClicked = { loginData ->
            viewModel.doLogin(loginData)
        },
        onCreateAccountClicked = {
            onCreatedAccountClicked()
        }
    )

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
                title = { Text("Cuenta no verificada") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Su cuenta aún no ha sido verificada por un administrador.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "IMPORTANTE: Debe esperar a que un administrador verifique su cuenta antes de poder ingresar al sistema.",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.resetLoginState()
                    }) {
                        Text("Entendido")
                    }
                }
            )
        }
        else -> {}
    }
}
