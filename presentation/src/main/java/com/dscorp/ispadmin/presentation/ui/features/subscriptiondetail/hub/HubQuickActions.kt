package com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.SubscriptionDetailTestTags

/** Shortcuts under the hub header. Charging lives on the debt card, so it is not repeated here. */
enum class HubAction(val label: String) {
    CALL("Llamar"), WHATSAPP("WhatsApp"), PAYMENTS("Pagos"), TICKET("Ticket"), PROVISIONING("Registro ONU");

    companion object {
        fun available(hasPhone: Boolean, isFiber: Boolean): List<HubAction> = buildList {
            if (hasPhone) addAll(listOf(CALL, WHATSAPP))
            add(PAYMENTS)
            add(TICKET)
            if (isFiber) add(PROVISIONING)
        }
    }
}

@Composable
fun HubQuickActions(actions: List<HubAction>, onAction: (HubAction) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        actions.forEach { action ->
            Column(
                modifier = Modifier.weight(1f).clickable(role = Role.Button) { onAction(action) }
                    .padding(vertical = 4.dp).then(action.testTag()?.let { Modifier.testTag(it) } ?: Modifier),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(action.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Text(action.label, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

private fun HubAction.testTag(): String? = when (this) {
    HubAction.CALL -> SubscriptionDetailTestTags.CALL
    HubAction.WHATSAPP -> SubscriptionDetailTestTags.WHATSAPP
    else -> null
}

private fun HubAction.icon(): ImageVector = when (this) {
    HubAction.CALL -> Icons.Rounded.Phone
    HubAction.WHATSAPP -> Icons.AutoMirrored.Rounded.Chat
    HubAction.PAYMENTS -> Icons.AutoMirrored.Rounded.ReceiptLong
    HubAction.TICKET -> Icons.Rounded.ConfirmationNumber
    HubAction.PROVISIONING -> Icons.Rounded.Router
}
