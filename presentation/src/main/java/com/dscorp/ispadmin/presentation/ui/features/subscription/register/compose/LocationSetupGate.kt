package com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.presentation.theme.MyTheme

@Composable
fun LocationSetupGate(
    status: LocationSetupStatus,
    onContinue: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = gateContent(status)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = content.title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = content.body,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onContinue) {
            Text(content.primaryLabel)
        }
        content.secondaryLabel?.let { label ->
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = {
                when (status) {
                    LocationSetupStatus.NeedsLocationEnabled -> onOpenLocationSettings()
                    LocationSetupStatus.PermissionPermanentlyDenied -> onOpenAppSettings()
                    else -> onOpenAppSettings()
                }
            }) {
                Text(label)
            }
        }
    }
}

private data class GateContent(
    val title: String,
    val body: String,
    val primaryLabel: String,
    val secondaryLabel: String?,
)

private fun gateContent(status: LocationSetupStatus): GateContent {
    return when (status) {
        LocationSetupStatus.NeedsPermission -> GateContent(
            title = "Ubicación necesaria para el registro",
            body = "Usamos tu ubicación para asignar automáticamente el lugar y las cajas NAP cercanas al cliente.",
            primaryLabel = "Continuar",
            secondaryLabel = null,
        )

        LocationSetupStatus.NeedsPermissionRationale -> GateContent(
            title = "Permiso de ubicación requerido",
            body = "Sin acceso a la ubicación no podemos determinar el lugar ni las cajas NAP del cliente. Concede el permiso para continuar.",
            primaryLabel = "Conceder permiso",
            secondaryLabel = null,
        )

        LocationSetupStatus.PermissionPermanentlyDenied -> GateContent(
            title = "Permiso de ubicación bloqueado",
            body = "Activa el permiso de ubicación manualmente en la configuración de la aplicación para continuar con el registro.",
            primaryLabel = "Abrir ajustes de la app",
            secondaryLabel = null,
        )

        LocationSetupStatus.NeedsLocationEnabled -> GateContent(
            title = "Activa la ubicación del dispositivo",
            body = "Para registrar la suscripción necesitamos que la ubicación del dispositivo esté activa.",
            primaryLabel = "Activar ubicación",
            secondaryLabel = "Abrir ajustes de ubicación",
        )

        LocationSetupStatus.Ready -> GateContent(
            title = "",
            body = "",
            primaryLabel = "",
            secondaryLabel = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationSetupGateNeedsPermissionPreview() {
    MyTheme {
        LocationSetupGate(
            status = LocationSetupStatus.NeedsPermission,
            onContinue = {},
            onOpenAppSettings = {},
            onOpenLocationSettings = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LocationSetupGateRationalePreview() {
    MyTheme {
        LocationSetupGate(
            status = LocationSetupStatus.NeedsPermissionRationale,
            onContinue = {},
            onOpenAppSettings = {},
            onOpenLocationSettings = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationSetupGateLocationDisabledPreview() {
    MyTheme {
        LocationSetupGate(
            status = LocationSetupStatus.NeedsLocationEnabled,
            onContinue = {},
            onOpenAppSettings = {},
            onOpenLocationSettings = {},
        )
    }
}
