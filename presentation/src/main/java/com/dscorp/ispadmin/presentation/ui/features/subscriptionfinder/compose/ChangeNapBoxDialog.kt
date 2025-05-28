package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.domain.model.NapBoxResponse
import com.dscorp.ispadmin.domain.model.SubscriptionResume
import com.dscorp.ispadmin.presentation.ui.components.MyButton
import com.dscorp.ispadmin.presentation.ui.components.MyOutLinedDropDown
import com.dscorp.ispadmin.presentation.ui.features.dialog.MyCustomDialog
import com.dscorp.ispadmin.data.apirequestmodel.MoveOnuRequest

/**
 * Dialog to change NAP box - Material 3 implementation
 */
@Composable
fun ChangeNapBoxDialog(
    viewModel: SubscriptionFinderViewModel,
    selectedSubscription: SubscriptionResume?,
    napBoxesState: NapBoxesState,
    context: Context,
    onDismiss: () -> Unit
) {
    // Fetch NAP boxes when dialog is shown
    LaunchedEffect(Unit) {
        viewModel.getNapBoxes()
    }

    if (selectedSubscription == null) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "No hay suscripción seleccionada", Toast.LENGTH_LONG).show()
            onDismiss()
        }
        return
    }

    MyCustomDialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            // Header
            Surface(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cambiar NAP Box",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                when (napBoxesState) {
                    is NapBoxesState.Error -> ErrorContent(onDismiss)
                    is NapBoxesState.Loading -> LoadingContent()
                    is NapBoxesState.NapBoxListLoaded -> NapBoxSelectionContent(
                        selectedSubscription = selectedSubscription,
                        availableNapBoxes = napBoxesState.items.filter { 
                            it.code != (selectedSubscription.napBox?.code ?: "") 
                        },
                        onConfirm = { selectedNapBox ->
                            val request = MoveOnuRequest(
                                subscriptionId = selectedSubscription.id,
                                newNapBoxId = selectedNapBox.id!!.toInt()
                            )
                            viewModel.changeNapBox(request)
                        }
                    )
                    is NapBoxesState.NapBoxChanged -> SuccessContent(
                        onAccept = {
                            onDismiss()
                            viewModel.resetNapBoxFlow()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        
        Text(
            text = "Error al cargar los NAP Box",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        
        MyButton(
            text = "Cerrar",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(0.7f)
        )
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Cargando NAP Boxes...",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun NapBoxSelectionContent(
    selectedSubscription: SubscriptionResume,
    availableNapBoxes: List<NapBoxResponse>,
    onConfirm: (NapBoxResponse) -> Unit
) {
    var selectedNapBox by remember { mutableStateOf<NapBoxResponse?>(null) }
    val currentNapBoxCode = selectedSubscription.napBox?.code ?: "Sin asignar"
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current NAP Box info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "NAP Box actual",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Text(
                    text = currentNapBoxCode,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                
                selectedSubscription.napBox?.address?.let { address ->
                    if (address.isNotEmpty()) {
                        Text(
                            text = address,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        
        Divider()
        
        // New NAP Box selection
        Text(
            text = "Seleccione nuevo NAP Box:",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        
        if (availableNapBoxes.isEmpty()) {
            Text(
                text = "No hay NAP Boxes disponibles para seleccionar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            MyOutLinedDropDown(
                items = availableNapBoxes,
                selected = selectedNapBox,
                label = "Nuevo NAP Box",
                onItemSelected = { selectedNapBox = it },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Confirm button
        MyButton(
            text = "Confirmar cambio",
            isLoading = false,
            enabled = selectedNapBox != null,
            onClick = { selectedNapBox?.let { onConfirm(it) } },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SuccessContent(onAccept: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
        }
        
        Text(
            text = "NAP Box cambiado exitosamente",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        MyButton(
            text = "Aceptar",
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth(0.7f)
        )
    }
}