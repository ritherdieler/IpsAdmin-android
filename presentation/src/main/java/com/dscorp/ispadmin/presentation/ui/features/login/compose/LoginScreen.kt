package com.dscorp.ispadmin.presentation.ui.features.login.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.dscorp.ispadmin.domain.model.User
import com.dscorp.ispadmin.presentation.ui.features.login.LoginState
import com.dscorp.ispadmin.presentation.ui.features.login.LoginViewModel
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
    if (loginState is LoginState.LoginSuccess) {
        onLoginSuccess((loginState as LoginState.LoginSuccess).data)
    }
}
