package com.dscorp.ispadmin.presentation.ui.features.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.dscorp.ispadmin.presentation.ui.features.profile.LogoutDialog
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.BadgeTone
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.HubBadge
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.hub.StatusBadge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.navigation.MoreItem

object MoreTestTags {
    fun item(item: MoreItem) = "more_item_${item.name.lowercase()}"
}

/** «Más» (a-mas-01): user card to the profile, the role's entries by section, logout and the app version. */
@Composable
fun MoreContent(
    menu: MoreMenu,
    versionLabel: String,
    onItemSelected: (MoreItem) -> Unit,
    onProfile: () -> Unit,
    onLogout: () -> Unit,
) {
    val colors = IspTheme.colors
    var confirmLogout by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().background(colors.card, CARD).clickable(enabled = menu.hasProfile, onClick = onProfile).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(48.dp).background(colors.selectedContainer, CircleShape), contentAlignment = Alignment.Center) {
                Text(menu.header.initials, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.onSelectedContainer)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(menu.header.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                if (menu.header.role.isNotBlank()) HubBadge(StatusBadge(menu.header.role, BadgeTone.INFO))
            }
            if (menu.hasProfile) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        menu.sections.forEach { section ->
            SectionTitle(section.title)
            Column(Modifier.fillMaxWidth().background(colors.card, CARD)) {
                section.entries.forEachIndexed { index, entry ->
                    MoreRow(entry, onClick = { onItemSelected(entry.item) })
                    if (index < section.entries.lastIndex) HorizontalDivider(Modifier.padding(start = 56.dp), color = colors.track)
                }
            }
        }
        SectionTitle("Cuenta")
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).background(colors.card, CARD).clickable { confirmLogout = true }.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text("Cerrar sesión", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
        }
        Text(
            text = versionLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
        )
    }
    if (confirmLogout) {
        LogoutDialog(onConfirm = { confirmLogout = false; onLogout() }, onDismiss = { confirmLogout = false })
    }
}

private val CARD = RoundedCornerShape(16.dp)

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp))
}

@Composable
private fun MoreRow(entry: MoreEntry, onClick: () -> Unit) {
    val item = entry.item
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick)
            .testTag(MoreTestTags.item(item)).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(item.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(item.label, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        entry.badge?.let { Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun MoreItem.icon(): ImageVector = when (this) {
    MoreItem.REGISTER_SUBSCRIPTION -> Icons.AutoMirrored.Rounded.NoteAdd
    MoreItem.CREATE_TICKET -> Icons.Rounded.AddCircleOutline
    MoreItem.SUPPORT_TICKETS -> Icons.Rounded.ConfirmationNumber
    MoreItem.INSTALLATION_ORDERS -> Icons.Rounded.Handyman
    MoreItem.PENDING_SUBSCRIPTIONS -> Icons.Rounded.CloudSync
    MoreItem.OUTLAYS -> Icons.Rounded.ReceiptLong
    MoreItem.REGISTER_OUTLAY -> Icons.AutoMirrored.Rounded.TrendingDown
    MoreItem.PLANS -> Icons.Rounded.Wifi
    MoreItem.REPORTS -> Icons.Rounded.TableChart
    MoreItem.NETWORK_MAP -> Icons.Rounded.Map
    MoreItem.OLT_TOOLS -> Icons.Rounded.Router
    MoreItem.PROFILE -> Icons.Rounded.Person
}
