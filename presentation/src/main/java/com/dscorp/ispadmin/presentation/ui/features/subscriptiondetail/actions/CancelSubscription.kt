package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.actions

import androidx.compose.foundation.background
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
 * The ONU is not removed from the OLT; it stays to be reused.
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

/** Body of the cancel bottom sheet over the ficha. */
@Composable
fun CancelSubscriptionSheetContent(model: CancelSubscription, busy: Boolean, error: String?, onConfirm: () -> Unit, onBack: () -> Unit) {
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
        error?.let { Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.error) }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onConfirm,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
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
