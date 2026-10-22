package com.dscorp.ispadmin.presentation.ui.features.profile

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.dscorp.ispadmin.presentation.theme.IspShapes

object LogoutDialogTags {
    const val CONFIRM = "logout_confirm"
}

/** a-est-06: the one logout confirmation, from Más and from Perfil. */
@Composable
fun LogoutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Cerrar sesión?") },
        text = { Text("Dejarás de recibir avisos de tickets en este teléfono hasta que vuelvas a ingresar.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag(LogoutDialogTags.CONFIRM),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
            ) { Text("Cerrar sesión", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Volver", fontWeight = FontWeight.SemiBold) } },
        shape = IspShapes.card,
    )
}
