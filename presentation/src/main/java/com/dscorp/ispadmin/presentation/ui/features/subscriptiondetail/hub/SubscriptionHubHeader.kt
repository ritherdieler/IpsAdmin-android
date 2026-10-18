package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.presentation.designsystem.components.pullUp
import com.dscorp.ispadmin.presentation.theme.IspTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.KeyboardType
import com.dscorp.ispadmin.presentation.ui.features.outlay.ChoiceChip
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.ficha.FICHA_ZONE
import java.time.LocalDate

object SubscriptionHubTestTags {
    const val CHARGE = "hub_charge"
    const val SUBMIT_CHARGE = "hub_charge_submit"
    const val PAYER_NAME = "hub_charge_payer"
    const val RECEIVED = "hub_charge_received"
    const val DISCOUNT = "hub_charge_discount"
    fun receipt(id: Int) = "hub_charge_receipt_$id"
}

private val HERO_BOTTOM = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
private val CARD = RoundedCornerShape(16.dp)
private val DEBT_CARD_OVERLAP = 40.dp

/** Navy band with the holder, status and debt; the debt card overlaps its edge and carries «Cobrar». */
@Composable
fun SubscriptionHubHeader(header: HubHeader, onCharge: () -> Unit) {
    val colors = IspTheme.colors
    Column {
        Column(
            modifier = Modifier.fillMaxWidth().background(colors.hero, HERO_BOTTOM)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = if (header.canCharge) 56.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(header.name, color = colors.onHero, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text(header.subtitle, color = colors.onHeroMuted, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                HubBadge(header.status)
                header.debtBadge?.let { HubBadge(it) }
            }
        }
        if (header.canCharge) {
            Surface(
                shape = CARD,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp).pullUp(DEBT_CARD_OVERLAP).fillMaxWidth(),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Deuda pendiente", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(header.debtAmount.orEmpty(), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(onClick = onCharge, modifier = Modifier.heightIn(min = 48.dp).testTag(SubscriptionHubTestTags.CHARGE)) {
                        Text("Cobrar", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun HubBadge(badge: StatusBadge) {
    val (container, content) = badge.tone.colors()
    Text(
        text = badge.text,
        color = content,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.background(container, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun BadgeTone.colors(): Pair<Color, Color> {
    val colors = IspTheme.colors
    return when (this) {
        BadgeTone.SUCCESS -> colors.successContainer to colors.success
        BadgeTone.WARNING -> colors.warningContainer to colors.warning
        BadgeTone.ERROR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
        BadgeTone.INFO -> colors.infoContainer to colors.info
        BadgeTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainer to MaterialTheme.colorScheme.onSurfaceVariant
    }
}

/** a-caj-02/03: body of the charge bottom sheet: receipts, method, payer or cash, discount, restore and one action. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChargeSheetContent(
    draft: ChargeDraft,
    holderName: String,
    onIntent: (SubscriptionHubIntent) -> Unit,
    isSubmitting: Boolean = false,
    error: String? = null,
    subtitle: String? = null,
    today: LocalDate = LocalDate.now(FICHA_ZONE),
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Cobrar a $holderName", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            subtitle?.let { Text(it, fontSize = 13.sp, color = muted) }
        }

        Column {
            SectionLabel("Recibos")
            draft.receipts.forEach { receipt ->
                val id = receipt.id ?: return@forEach
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        .clickable(role = Role.Checkbox) { onIntent(SubscriptionHubIntent.ReceiptToggled(id)) }
                        .testTag(SubscriptionHubTestTags.receipt(id)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = id in draft.selectedIds,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                    )
                    Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(billingMonthLabel(receipt.billingDate), fontSize = 15.sp)
                        receiptStatus(receipt, today)?.let { HubBadge(it) }
                    }
                    Text(formatSoles(receipt.amountToPay), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Método")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 2) {
                draft.methods.forEach { option ->
                    MethodTile(
                        label = option.name,
                        selected = option.name == draft.method,
                        lastUsed = option.name == draft.lastUsed,
                        onClick = { onIntent(SubscriptionHubIntent.MethodSelected(option.name)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (draft.requiresPayerName) {
            OutlinedTextField(
                value = draft.payerName,
                onValueChange = { onIntent(SubscriptionHubIntent.PayerNameChanged(it)) },
                label = { Text("Nombre en ${draft.method} *") },
                supportingText = { Text("Como aparece en la captura") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(SubscriptionHubTestTags.PAYER_NAME),
            )
        }

        if (draft.isCash) {
            OutlinedTextField(
                value = draft.received,
                onValueChange = { onIntent(SubscriptionHubIntent.ReceivedChanged(it)) },
                label = { Text("Recibido") },
                prefix = { Text("S/ ") },
                supportingText = draft.change?.let { change -> { Text("Vuelto ${formatSoles(change)}") } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().testTag(SubscriptionHubTestTags.RECEIVED),
            )
        }

        DiscountSection(draft, onIntent)

        if (draft.canRestore) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Restablecer el servicio", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(restoreNote(draft), fontSize = 12.sp, color = muted)
                }
                Switch(checked = draft.restore, onCheckedChange = { onIntent(SubscriptionHubIntent.RestoreToggled) })
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val count = draft.selectedIds.size
            Text("Total · $count ${if (count == 1) "recibo" else "recibos"}", fontSize = 14.sp, color = muted, modifier = Modifier.weight(1f))
            Text(formatSoles(draft.toPay), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        val message = error ?: draft.blockingReason
        message?.let {
            Text(it, fontSize = 13.sp, color = if (error != null) MaterialTheme.colorScheme.error else muted)
        }
        Button(
            onClick = { onIntent(SubscriptionHubIntent.SubmitCharge) },
            enabled = draft.blockingReason == null && !isSubmitting,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(SubscriptionHubTestTags.SUBMIT_CHARGE),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text(if (isSubmitting) "Registrando…" else "Registrar cobro · ${formatSoles(draft.toPay)}", fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun restoreNote(draft: ChargeDraft): String {
    val left = draft.receipts.filter { it.id !in draft.selectedIds }
    return when (left.size) {
        0 -> "Queda al día: se puede restablecer"
        1 -> "Queda con 1 recibo pendiente (${billingMonthLabel(left.single().billingDate).substringBefore(' ').lowercase()}): se puede restablecer"
        else -> "Queda con ${left.size} recibos pendientes"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscountSection(draft: ChargeDraft, onIntent: (SubscriptionHubIntent) -> Unit) {
    if (!draft.discountOpen) {
        TextButton(onClick = { onIntent(SubscriptionHubIntent.DiscountToggled) }, contentPadding = PaddingValues(0.dp)) {
            Text("+ Aplicar descuento", fontWeight = FontWeight.SemiBold)
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Descuento", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = { onIntent(SubscriptionHubIntent.DiscountToggled) }) { Text("Quitar") }
        }
        OutlinedTextField(
            value = draft.discount,
            onValueChange = { onIntent(SubscriptionHubIntent.DiscountChanged(it)) },
            label = { Text("Monto") },
            prefix = { Text("S/ ") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().testTag(SubscriptionHubTestTags.DISCOUNT),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DISCOUNT_REASONS.forEach { reason ->
                ChoiceChip(reason, selected = reason == draft.discountReason) {
                    onIntent(SubscriptionHubIntent.DiscountReasonSelected(reason))
                }
            }
        }
    }
}

/** a-caj-04: confirmation after the charge, with the data the client may ask for. */
@Composable
fun ChargeReceiptContent(receipt: ChargeReceipt, onDone: () -> Unit, onShare: () -> Unit) {
    val colors = IspTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.success, modifier = Modifier.size(48.dp))
        Text("Cobro registrado", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(receipt.amount, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Column(
            Modifier.fillMaxWidth().border(1.dp, colors.cardBorder, CARD).padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            listOf(
                "N.º de pago" to receipt.number,
                "Titular" to receipt.holder,
                "Recibos" to receipt.months,
                "Método" to receipt.method,
                "Registrado por" to receipt.collector,
                "Fecha" to receipt.date,
            ).filter { it.second.isNotBlank() }.forEachIndexed { index, (label, value) ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        receipt.restored?.let { restored ->
            val (container, content) = if (restored) colors.successContainer to colors.success
            else MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
            Text(
                if (restored) "Servicio restablecido." else "No se pudo restablecer el servicio. Hazlo desde la ficha.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = content,
                modifier = Modifier.fillMaxWidth().background(container, CARD).padding(12.dp),
            )
        }
        OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text("Compartir comprobante", fontWeight = FontWeight.SemiBold)
        }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text("Listo", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
private fun MethodTile(label: String, selected: Boolean, lastUsed: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val outline = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = modifier.heightIn(min = 52.dp)
            .border(if (selected) 2.dp else 1.dp, outline, CARD)
            .background(if (selected) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surface, CARD)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(label, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            if (lastUsed) Text("Último usado", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
