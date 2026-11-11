package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.presentation.theme.IspTheme

object ChargeProofTestTags {
    const val CARD = "charge_proof_card"
    const val USE = "charge_proof_use"
    const val DISCARD = "charge_proof_discard"
    const val CONFIRM_DISCARD = "charge_proof_confirm_discard"
    fun reason(reason: DiscardReason) = "charge_proof_reason_${reason.name}"
}

private val PROOF_CARD = RoundedCornerShape(16.dp)

/** a-cmp-02/09: the pending proof the charge must use; the rest of the sheet waits until staff use or discard it. */
@Composable
fun PendingProofCard(proof: PendingProof, position: Int, count: Int, onIntent: (SubscriptionHubIntent) -> Unit) {
    val colors = IspTheme.colors
    Surface(
        shape = PROOF_CARD,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().border(2.dp, colors.warning, PROOF_CARD).testTag(ChargeProofTestTags.CARD),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Este cobro se registra con este comprobante", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                HubBadge(
                    if (proof.readConfirmed) StatusBadge("◷ $position de $count", BadgeTone.WARNING)
                    else StatusBadge("! Lectura sin confirmar", BadgeTone.WARNING),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProofThumbnail(proof)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ProofLine("Llegó", listOf(proof.source, proof.received).filter { it.isNotBlank() }.joinToString(" · "))
                ProofLine("Monto", proof.amount?.let(::formatSoles) ?: "Sin monto leído")
                ProofLine("Medio", proof.method)
                proof.payerName?.let { ProofLine("Pagador", it) }
                proof.operation?.let { ProofLine("Operación", it) }
            }
            }
            if (proof.sentCount > 1) HubBadge(StatusBadge("Enviado ${proof.sentCount} veces", BadgeTone.NEUTRAL))
            if (proof.otherServiceFees.isNotEmpty()) {
                Text(
                    "También paga: " + proof.otherServiceFees.joinToString(" · ") { listOfNotNull(it.place, it.period, formatSoles(it.amount)).joinToString(" ") },
                    fontSize = 13.sp,
                    color = colors.info,
                )
            }
            Text(
                if (proof.readConfirmed) "Compara los datos con la captura. Solo descártalo si no es un comprobante válido."
                else "El cliente lo ve «En revisión», no como pagado. Compara en la captura que el pago fue a GigaFiber y el monto antes de usarlo.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onIntent(SubscriptionHubIntent.DiscardProof(proof.key)) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag(ChargeProofTestTags.DISCARD),
                ) { Text("Descartar") }
                Button(
                    onClick = { onIntent(SubscriptionHubIntent.UseProof(proof.key)) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag(ChargeProofTestTags.USE),
                ) { Text("Usar en el cobro", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

/** a-cmp-03: the proof already in this charge, with «Cambiar» to pick another pending one. */
@Composable
fun UsedProofRow(proof: PendingProof, canChange: Boolean, onIntent: (SubscriptionHubIntent) -> Unit) {
    Surface(shape = PROOF_CARD, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                HubBadge(StatusBadge("✓ En este cobro", BadgeTone.SUCCESS))
                Text(
                    listOfNotNull(
                        "${proof.source} ${proof.received}".trim(),
                        listOfNotNull(proof.method, proof.amount?.let(::formatSoles)).joinToString(" "),
                        proof.operation?.let { "op. $it" },
                    ).joinToString(" · "),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (canChange) TextButton(onClick = { onIntent(SubscriptionHubIntent.ChangeProof) }) { Text("Cambiar") }
        }
    }
}

/** a-cmp-03: the method comes from the proof and cannot change. */
@Composable
fun LockedMethod(method: String) {
    Surface(
        shape = PROOF_CARD,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.primary, PROOF_CARD),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(method, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("Del comprobante · no se cambia", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** a-cmp-05: why the proof is discarded; the CRM's reasons, the same ones the back office uses. */
@Composable
fun DiscardProofSection(charge: ChargeSheetState, onIntent: (SubscriptionHubIntent) -> Unit) {
    val proof = charge.discarding ?: return
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("¿Por qué lo descartas?", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                listOfNotNull("Comprobante de ${proof.source} ${proof.received}".trim(), listOfNotNull(proof.method, proof.amount?.let(::formatSoles)).joinToString(" "))
                    .joinToString(" · "),
                fontSize = 13.sp,
                color = muted,
            )
        }
        DiscardReason.values().forEach { reason ->
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    .clickable(role = Role.RadioButton) { onIntent(SubscriptionHubIntent.DiscardReasonPicked(reason)) }
                    .testTag(ChargeProofTestTags.reason(reason)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = charge.discardReason == reason, onClick = null)
                Text(reason.label, fontSize = 15.sp, modifier = Modifier.padding(start = 12.dp))
            }
        }
        Text(
            "Con «La captura se leyó mal» o «No se lee el monto o el número de operación» el cliente puede reenviar la misma captura. Con los demás motivos, no.",
            fontSize = 12.sp,
            color = muted,
        )
        Text(
            "Descarta solo si no es un comprobante válido: si lo es, el cobro debe registrarse con él. Queda como descartado en el chat del CRM, con tu nombre y el motivo.",
            fontSize = 12.sp,
            color = muted,
        )
        charge.error?.let { Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.error) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onIntent(SubscriptionHubIntent.CancelDiscard) }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                Text("Volver")
            }
            Button(
                onClick = { onIntent(SubscriptionHubIntent.ConfirmDiscard) },
                enabled = charge.discardReason != null && !charge.isDiscarding,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp).testTag(ChargeProofTestTags.CONFIRM_DISCARD),
            ) { Text(if (charge.isDiscarding) "Descartando…" else "Descartar y seguir", fontWeight = FontWeight.SemiBold) }
        }
    }
}

/** a-cmp-02 «Ampliar»: the capture, opened with the session (Core media needs ADMIN or SECRETARY). */
@Composable
private fun ProofThumbnail(proof: PendingProof) {
    val url = proof.imageUrl ?: return
    var expanded by remember { mutableStateOf(false) }
    val context = LocalPlatformContext.current
    val request = remember(url, proof.imageAuthorization) {
        ImageRequest.Builder(context).data(url).apply {
            proof.imageAuthorization?.let { httpHeaders(NetworkHeaders.Builder().set("Authorization", it).build()) }
        }.build()
    }
    Box(
        Modifier.size(width = 72.dp, height = 104.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
            .clickable(onClickLabel = "Ver captura en grande") { expanded = true },
        contentAlignment = Alignment.BottomCenter,
    ) {
        AsyncImage(model = request, contentDescription = "Captura del comprobante", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Text("Ampliar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
    }
    if (expanded) {
        Dialog(onDismissRequest = { expanded = false }) {
            Surface(shape = PROOF_CARD) {
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(model = request, contentDescription = "Captura del comprobante", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp))
                    TextButton(onClick = { expanded = false }) { Text("Cerrar") }
                }
            }
        }
    }
}

@Composable
private fun ProofLine(label: String, value: String) {
    Row {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(84.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
