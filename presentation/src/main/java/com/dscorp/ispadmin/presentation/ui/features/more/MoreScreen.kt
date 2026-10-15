package com.dscorp.ispadmin.presentation.ui.features.more

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

/** «Más»: the role's secondary entries, one grouped list, plus the app version. */
@Composable
fun MoreContent(items: List<MoreItem>, versionLabel: String, onItemSelected: (MoreItem) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column {
                items.forEachIndexed { index, item ->
                    MoreRow(item, onClick = { onItemSelected(item) })
                    if (index < items.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
                }
            }
        }
        Text(
            text = versionLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun MoreRow(item: MoreItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick)
            .testTag(MoreTestTags.item(item)).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(item.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(item.label, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
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
    MoreItem.REGISTER_OUTLAY -> Icons.AutoMirrored.Rounded.TrendingDown
    MoreItem.PLANS -> Icons.Rounded.Wifi
    MoreItem.PROFILE -> Icons.Rounded.Person
}
