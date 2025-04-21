package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.domain.model.InstallationType
import com.dscorp.ispadmin.domain.model.SubscriptionResume

/**
 * Enum representing the menu options available for a subscription
 */
enum class SubscriptionMenu(val menuId: Int) {
    SHOW_PAYMENT_HISTORY(R.string.show_payment_history),
    EDIT_PLAN_SUBSCRIPTION(R.string.edit_plan),
    SEE_DETAILS(R.string.see_details),
    MIGRATE_TO_FIBER(R.string.migrate_to_fiber),
    CANCEL_SUBSCRIPTION(R.string.cancel_subscription),
    CHANGE_NAP_BOX(R.string.change_nap_box),
    UPDATE_LOCATION(R.string.update_location);

    fun getTitle(context: Context): String {
        return context.getString(menuId)
    }
}

/**
 * Header component for the subscription card displaying customer name and menu options
 * 
 * @param subscription The subscription data to display
 * @param onMenuItemSelected Callback for when a menu item is selected
 * @param modifier Optional modifier for customizing the component
 */
@Composable
fun CardHeader(
    subscription: SubscriptionResume,
    onMenuItemSelected: (SubscriptionMenu) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Customer name (takes most of the space)
        Text(
            text = subscription.customerName.capitalize(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start
        )
        
        // More options menu
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Ver opciones",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            SubscriptionDropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                subscription = subscription,
                onMenuItemSelected = { 
                    menuExpanded = false
                    onMenuItemSelected(it)
                }
            )
        }
    }
}

/**
 * Dropdown menu for subscription actions
 */
@Composable
private fun SubscriptionDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    subscription: SubscriptionResume,
    onMenuItemSelected: (SubscriptionMenu) -> Unit
) {
    val context = LocalContext.current
    
    // Filter menu items based on installation type
    val menuItems = SubscriptionMenu.values().toMutableList().filter { menuItem ->
        when (subscription.installationType) {
            InstallationType.FIBER -> menuItem != SubscriptionMenu.MIGRATE_TO_FIBER
            InstallationType.WIRELESS -> menuItem != SubscriptionMenu.CHANGE_NAP_BOX
            else -> true
        }
    }
    
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.width(220.dp)
    ) {
        // Title section
        Text(
            text = "Opciones de suscripción",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.primary
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        
        // Menu items
        menuItems.forEach { menuItem ->
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Icon based on menu item type
                        Icon(
                            painter = painterResource(
                                id = R.drawable.ic_more_dot
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = menuItem.getTitle(context),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                onClick = { onMenuItemSelected(menuItem) }
            )
        }
    }
}