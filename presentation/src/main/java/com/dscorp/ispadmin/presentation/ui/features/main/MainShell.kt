package com.dscorp.ispadmin.presentation.ui.features.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.navigation.MainDestination
import com.dscorp.ispadmin.navigation.ShellChrome
import com.dscorp.ispadmin.presentation.theme.IspTheme

object MainShellTestTags {
    const val BOTTOM_BAR = "main_bottom_bar"
    const val TITLE = "main_top_bar_title"
    fun item(destination: MainDestination) = "main_nav_${destination.name.lowercase()}"
}

/** App frame: title bar (back arrow on secondary screens) and the role's bottom bar on root screens. */
@Composable
fun MainShell(
    chrome: ShellChrome,
    onDestinationSelected: (MainDestination) -> Unit,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { if (chrome.showTopBar) ShellTopBar(chrome, onBack) },
        bottomBar = {
            if (chrome.bottomBar.isNotEmpty()) ShellBottomBar(chrome.bottomBar, chrome.selected, onDestinationSelected)
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) { content() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShellTopBar(chrome: ShellChrome, onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = chrome.title,
                fontSize = if (chrome.showBack) 20.sp else 22.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag(MainShellTestTags.TITLE),
            )
        },
        navigationIcon = {
            if (chrome.showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

@Composable
private fun ShellBottomBar(
    destinations: List<MainDestination>,
    selected: MainDestination?,
    onDestinationSelected: (MainDestination) -> Unit,
) {
    val colors = IspTheme.colors
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag(MainShellTestTags.BOTTOM_BAR),
    ) {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = destination == selected,
                onClick = { onDestinationSelected(destination) },
                icon = { Icon(destination.icon(), contentDescription = null) },
                label = { Text(destination.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = colors.selectedContainer,
                    selectedIconColor = colors.onSelectedContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.testTag(MainShellTestTags.item(destination)),
            )
        }
    }
}

private fun MainDestination.icon(): ImageVector = when (this) {
    MainDestination.HOME -> Icons.Rounded.Home
    MainDestination.SUBSCRIPTIONS -> Icons.Rounded.Groups
    MainDestination.TICKETS -> Icons.Rounded.ConfirmationNumber
    MainDestination.CASH -> Icons.Rounded.AccountBalanceWallet
    MainDestination.INSTALLATIONS -> Icons.Rounded.Handyman
    MainDestination.MORE -> Icons.Rounded.GridView
}
