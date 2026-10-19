package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import com.dscorp.ispadmin.presentation.ui.features.subscription.register.compose.RegisterFieldLabel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.ServiceStatus
import com.dscorp.ispadmin.domain.model.SubscriptionResponse
import com.dscorp.ispadmin.presentation.ui.features.home.monthShort
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.formatSoles
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.displayName
import java.time.LocalDate

/**
 * a-sus-13: what cancelling does, as the Core does it today (PUT subscription/cancel-subscription):
 * stops billing, cuts the access, frees the NAP port (cancelled ones do not hold it) and keeps the debt.
 * The ONU is not removed from the OLT; it stays to be reused. The reason is stored with the cancellation.
 */
data class CancelSubscription(val title: String, val effects: List<String>) {
    companion object {
        fun from(subscription: SubscriptionResponse, today: LocalDate): CancelSubscription {
            val name = displayName(subscription.firstName, subscription.lastName)
            val next = today.withDayOfMonth(1).plusMonths(1)
            val nap = subscription.napBox?.code?.takeIf { it.isNotBlank() }
            val port = subscription.borneNumber?.trim()?.takeIf { it.isNotEmpty() }
            val fiber = subscription.installationType != InstallationType.WIRELESS
            return CancelSubscription(
                title = "¿Cancelar la suscripción de $name?",
                effects = listOfNotNull(
                    "Deja de generar recibos desde ${monthShort(next)} ${next.year}.",
                    when {
                        subscription.serviceStatus == ServiceStatus.CUT_OFF -> null
                        subscription.installationType == InstallationType.ONLY_TV_FIBER -> "Se le corta el servicio."
                        else -> "Se le corta el internet."
                    },
                    when {
                        nap != null && port != null -> "Se libera el puerto $port de la caja $nap."
                        nap != null -> "Deja libre su lugar en la caja $nap."
                        else -> null
                    },
                    subscription.fiberOnuSn?.takeIf { fiber && it.isNotBlank() }?.let { "La ONU $it queda registrada en la OLT para reutilizarla." },
                    subscription.totalDebt.takeIf { it > 0 }?.let { "La deuda de ${formatSoles(it)} se mantiene." },
                ),
            )
        }
    }
}

val CANCEL_REASONS = listOf("Mudanza", "Problemas con el servicio", "Precio", "Se cambia de proveedor", "Falta de pago", "Otro")

object CancelSubscriptionTestTags {
    const val REASON = "cancel_subscription_reason"
    const val CONFIRM = "cancel_subscription_confirm"
}

/** Body of the cancel bottom sheet over the ficha; the reason is required and stored by the Core. */
@Composable
fun CancelSubscriptionSheetContent(
    model: CancelSubscription,
    reason: String?,
    onReason: (String) -> Unit,
    busy: Boolean,
    error: String?,
    onConfirm: (String) -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(model.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            model.effects.forEach { effect ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.padding(top = 7.dp).size(6.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape))
                    Spacer(Modifier.width(10.dp))
                    Text(effect, fontSize = 15.sp)
                }
            }
        }
        ReasonField(reason, enabled = !busy, onReason = onReason)
        error?.let { Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.error) }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { reason?.let(onConfirm) },
                enabled = !busy && reason != null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(CancelSubscriptionTestTags.CONFIRM),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onError, strokeWidth = 2.dp)
                else Text("Cancelar suscripción", fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(onClick = onBack, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Volver", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ReasonField(reason: String?, enabled: Boolean, onReason: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RegisterFieldLabel("Motivo *")
        Box {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .clickable(enabled = enabled, role = Role.DropdownList) { open = true }
                    .padding(horizontal = 16.dp)
                    .testTag(CancelSubscriptionTestTags.REASON),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    reason ?: "Elige el motivo",
                    fontSize = 16.sp,
                    color = if (reason == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Rounded.ExpandMore, contentDescription = null)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                CANCEL_REASONS.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { open = false; onReason(option) })
                }
            }
        }
    }
}
