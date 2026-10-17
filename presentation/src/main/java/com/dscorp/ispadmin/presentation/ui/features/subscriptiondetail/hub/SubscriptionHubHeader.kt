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

object SubscriptionHubTestTags {
    const val CHARGE = "hub_charge"
    const val SUBMIT_CHARGE = "hub_charge_submit"
    const val PAYER_NAME = "hub_charge_payer"
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

/** Body of the charge bottom sheet: receipts, method, payer, restore and the single primary action. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChargeSheetContent(
    draft: ChargeDraft,
    holderName: String,
    onIntent: (SubscriptionHubIntent) -> Unit,
    isSubmitting: Boolean = false,
    error: String? = null,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Cobrar a $holderName", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)

        Column {
            SectionLabel("Recibos")
            draft.receipts.forEach { receipt ->
                val id = receipt.id ?: return@forEach
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .clickable(role = Role.Checkbox) { onIntent(SubscriptionHubIntent.ReceiptToggled(id)) }
                        .testTag(SubscriptionHubTestTags.receipt(id)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = id in draft.selectedIds,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                    )
                    Text(billingMonthLabel(receipt.billingDate), fontSize = 15.sp, modifier = Modifier.weight(1f).padding(start = 12.dp))
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
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(SubscriptionHubTestTags.PAYER_NAME),
            )
        }

        if (draft.canRestore) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Restablecer el servicio", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Está cortado y queda con menos de 2 recibos pendientes", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = draft.restore, onCheckedChange = { onIntent(SubscriptionHubIntent.RestoreToggled) })
            }
        }

        val message = error ?: draft.blockingReason
        message?.let {
            Text(it, fontSize = 13.sp, color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(
            onClick = { onIntent(SubscriptionHubIntent.SubmitCharge) },
            enabled = draft.blockingReason == null && !isSubmitting,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(SubscriptionHubTestTags.SUBMIT_CHARGE),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text(if (isSubmitting) "Registrando…" else "Registrar cobro · ${formatSoles(draft.total)}", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
private fun MethodTile(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val outline = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = modifier.heightIn(min = 52.dp)
            .border(if (selected) 2.dp else 1.dp, outline, CARD)
            .background(if (selected) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surface, CARD)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(label, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}
