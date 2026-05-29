package com.dscorp.ispadmin.presentation.ui.features.login

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.presentation.theme.MyTheme
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
    onFaceLoginClicked: () -> Unit = {}, // agrego edwin
    // Evento del boton de huella; la pantalla contenedora decide si abre AndroidX Biometric.
    onBiometricLoginClicked: () -> Unit = {},
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

            // Boton principal: conserva el login normal con usuario, contrasena y checkbox.
            PrimaryLoginButton(
                text = "Iniciar sesion",
                enabled = loginState !is LoginState.Loading,
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

            Spacer(modifier = Modifier.height(16.dp))

            // Boton secundario: abre el flujo facial que ya existe sin cambiar su logica.
            LoginOptionButton(
                text = "Iniciar sesion con reconocimiento facial",
                icon = Icons.Default.Face,
                enabled = loginState !is LoginState.Loading,
                onClick = onFaceLoginClicked
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Boton secundario: abre el flujo de h  uella con AndroidX Biometric.
            LoginOptionButton(
                text = "Iniciar sesion con huella digital",
                icon = Icons.Default.Fingerprint,
                enabled = loginState !is LoginState.Loading,
                onClick = onBiometricLoginClicked
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Accion de registro: queda como enlace para no competir con los botones de ingreso.
            TextButton(
                enabled = loginState !is LoginState.Loading,
                onClick = onCreateAccountClicked
            ) {
                Text(
                    text = "Crear cuenta",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// Dibuja el boton principal de inicio de sesion solo para esta pantalla.
// Se mantiene local para no modificar el componente global MyButton ni afectar otras vistas.
@Composable
private fun PrimaryLoginButton(
    text: String,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f)
        ),
        onClick = onClick
    ) {
        Text(
            text = if (isLoading) "Iniciando..." else text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// Dibuja los botones secundarios del login facial y huella digital.
// Usa borde fino para separarlos visualmente del boton principal.
@Composable
private fun LoginOptionButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) {
                MaterialTheme.colorScheme.outline
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            }
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        ),
        onClick = onClick
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 10.dp)
                .size(20.dp)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginPreview() {
    MyTheme {
        Login(loginState = LoginState.Loading)
    }
}
