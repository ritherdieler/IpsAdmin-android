package com.dscorp.ispadmin.presentation.ui.features.supportTicket.list.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactActionSheet(
    phoneNumber: String,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Contactar al cliente",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(
                    horizontal = 24.dp,
                    vertical = 8.dp
                )
            )

            Text(
                text = phoneNumber,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = 24.dp,
                    vertical = 4.dp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Divider()

            ListItem(
                headlineContent = {
                    Text(text = "Llamar")
                },
                supportingContent = {
                    Text(text = "Abrir el marcador telefónico")
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clickable(onClick = onCall)
            )

            ListItem(
                headlineContent = {
                    Text(text = "Enviar mensaje por WhatsApp")
                },
                supportingContent = {
                    Text(text = "Abrir una conversación con el cliente")
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Filled.Chat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clickable(onClick = onWhatsApp)
            )

            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(text = "Cancelar")
            }
        }
    }
}