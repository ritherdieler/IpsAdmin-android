package com.dscorp.ispadmin.presentation.ui.features.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.dscorp.components.components.formfields.MyOutlinedTextField

// Solicita credenciales antes de registrar un rostro nuevo.
// La camara se abre solamente despues de validar la identidad del usuario.
@Composable
fun FaceEnrollmentCredentialsScreen(
    isLoading: Boolean,
    onContinue: (LoginForm) -> Unit,
    onCancel: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Registrar reconocimiento facial",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Confirma tu usuario y contrasena para asociar el rostro a tu cuenta.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            MyOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = username,
                label = "Usuario",
                onValueChange = { username = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            MyOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = password,
                label = "Contrasena",
                onValueChange = { password = it },
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (passwordVisible) {
                                "Ocultar contrasena"
                            } else {
                                "Mostrar contrasena"
                            }
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isLoading && username.isNotBlank() && password.isNotBlank(),
                shape = RoundedCornerShape(26.dp),
                onClick = {
                    onContinue(
                        LoginForm(
                            username = username,
                            password = password,
                            checkedState = false
                        )
                    )
                }
            ) {
                Text(if (isLoading) "Validando..." else "Continuar")
            }

            TextButton(
                enabled = !isLoading,
                onClick = onCancel
            ) {
                Text("Cancelar")
            }
        }
    }
}
