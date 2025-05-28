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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.presentation.theme.MyTheme
import com.dscorp.ispadmin.presentation.theme.myTypography
import com.dscorp.ispadmin.presentation.ui.components.MyButton

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
    val userNameError by remember { derivedStateOf { username.isEmpty() } }
    val passwordError by remember { derivedStateOf { password.isEmpty() } }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), color = Color.White
    ) {
        Column(
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

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth(),
                label = { Text(text = "Usuario") },
                placeholder = { Text(text = "Usuario") },
                value = username,
                onValueChange = { username = it },
                isError = userNameError,
            )
            if (userNameError)
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, top = 8.dp),
                    text = "*Debe ingresar un usuario valido",
                    color = Color.Red,
                    style = myTypography.labelSmall
                ) else Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth(),
                label = { Text(text = "Contraseña") },
                placeholder = { Text(text = "Contraseña") },
                value = password,
                onValueChange = { password = it },
                isError = passwordError,
            )
            if (passwordError) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, top = 8.dp),
                    text = "*La contraseña no puede estar vacia",
                    color = Color.Red,
                    style = myTypography.labelSmall
                )
            } else Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Checkbox(
                    checked = checkedState,
                    onCheckedChange = { checkedState = it }
                )
                Text(
                    text = "Mantener sesion iniciada",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            MyButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                text = "Iniciar sesion",
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
                })

            MyButton (
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