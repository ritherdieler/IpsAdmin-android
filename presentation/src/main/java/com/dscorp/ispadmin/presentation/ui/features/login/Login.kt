package com.dscorp.ispadmin.presentation.ui.features.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.ui.components.MyButton
import com.dscorp.components.components.formfields.MyOutlinedTextField

data class LoginForm(
    var username: String = "",
    var password: String = "",
    var checkedState: Boolean = false
) {
    fun isValid(): Boolean {
        return username.isNotEmpty() && password.isNotEmpty()
    }
}

@Composable
fun Login(
    onLoginClicked: (LoginForm) -> Unit = { },
    loginState: LoginState,
    onCreateAccountClicked: () -> Unit = { },
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var checkedState by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    val userNameError by remember { derivedStateOf { username.isEmpty() } }
    val passwordError by remember { derivedStateOf { password.isEmpty() } }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            ,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier =  Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(200.dp)
                    .clip(RoundedCornerShape(15.dp)),
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "",
            )

            MyOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = username,
                label = "Usuario",
                onValueChange = { username = it },
                errorMessage = if (userNameError) "Debe ingresar un usuario válido" else null
            )

            Spacer(modifier = Modifier.height(16.dp))

            MyOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = password,
                label = "Contraseña",
                onValueChange = { password = it },
                errorMessage = if (passwordError) "La contraseña no puede estar vacía" else null,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Checkbox(
                    checked = checkedState,
                    onCheckedChange = { checkedState = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.outline,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                Text(
                    text = "Mantener sesión iniciada",
                    modifier = Modifier.padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            MyButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                text = "Iniciar sesión",
                enabled = true,
                isLoading = loginState is LoginState.Loading,
                onClick = {
                    onLoginClicked(
                        LoginForm(
                            username = username,
                            password = password,
                            checkedState = checkedState
                        )
                    )
                }
            )

            MyButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Crear cuenta",
                onClick = {
                    onCreateAccountClicked()
                },
                enabled = loginState !is LoginState.Loading
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginPreview() {
    MyTheme {
        Login(loginState = LoginState.Loading)
    }
}